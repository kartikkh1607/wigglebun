package com.kartikkhandelwal.wigglebun.ui.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartikkhandelwal.wigglebun.activity.MovementPermissions
import com.kartikkhandelwal.wigglebun.activity.StepCounter
import com.kartikkhandelwal.wigglebun.activity.StepReading
import com.kartikkhandelwal.wigglebun.data.prefs.LoggedTransition
import com.kartikkhandelwal.wigglebun.data.prefs.MovementStore
import com.kartikkhandelwal.wigglebun.data.prefs.SettingsRepository
import com.kartikkhandelwal.wigglebun.data.repo.PetRepository
import com.kartikkhandelwal.wigglebun.data.repo.RecordBreakDone
import com.kartikkhandelwal.wigglebun.data.repo.SitRepository
import com.kartikkhandelwal.wigglebun.data.repo.WalletRepository
import com.kartikkhandelwal.wigglebun.data.time.AppClock
import com.kartikkhandelwal.wigglebun.domain.model.ActivePet
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.domain.model.MovementActivity
import com.kartikkhandelwal.wigglebun.domain.model.MovementTransition
import com.kartikkhandelwal.wigglebun.domain.model.ResetReason
import com.kartikkhandelwal.wigglebun.domain.model.SparkReason
import com.kartikkhandelwal.wigglebun.domain.sitting.SitStatus
import com.kartikkhandelwal.wigglebun.domain.sitting.SittingSettings
import com.kartikkhandelwal.wigglebun.domain.sitting.WorkSchedule
import com.kartikkhandelwal.wigglebun.notify.ReminderNotifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

private const val CELEBRATE_MS = 3_000L
private const val INTERVAL_STEP = 5

/** Manual pet controls (used when the pet does not follow the engine). */
data class ManualPet(
    val energy: Float = 1f,
    val mood: Mood = Mood.HAPPY,
    /** Mood follows the energy slider (HAPPY / TIRED / STIFF). */
    val moodFollowsEnergy: Boolean = true,
)

/** What the engine panel shows. */
data class EngineInfo(
    val now: Instant,
    val zone: ZoneId,
    val isWorkTime: Boolean,
    val sitSeconds: Long,
    val intervalMinutes: Int,
    val nextReminder: Instant?,
    val snoozeCount: Int,
    val mood: Mood,
    val energy: Float,
    val notificationsAllowed: Boolean,
    val fastClock: Boolean,
    val alwaysWorkHours: Boolean,
)

/** What the Movement card shows. */
data class MovementCard(
    val enabled: Boolean,
    val registered: Boolean,
    val permissionGranted: Boolean,
    val needsRuntimePermission: Boolean,
    val playServicesAvailable: Boolean,
    val hasStepSensor: Boolean,
    val currentActivity: MovementActivity?,
    /** Newest event first; real wall-clock times. */
    val log: List<LoggedTransition>,
    val stepsSinceTimerStart: Long?,
    /** Last value the app itself got from the step counter (no extra listener in the lab). */
    val lastStepReading: StepReading?,
    /** Whether the best-effort work-hours step listener is registered right now. */
    val stepListenerOn: Boolean,
    val lastResetReason: ResetReason?,
    val isOnWalk: Boolean,
)

data class PetLabState(
    /** The active pet (M8): the lab draws whichever pet is with you. */
    val pet: ActivePet = ActivePet.Default,
    val manual: ManualPet = ManualPet(),
    val followEngine: Boolean = true,
    val animated: Boolean = true,
    val engine: EngineInfo? = null,
    val movement: MovementCard? = null,
) {
    val petEnergy: Float get() = if (followEngine && engine != null) engine.energy else manual.energy
    val petMood: Mood get() = if (followEngine && engine != null) engine.mood else manual.mood
}

private data class LabControls(val manual: ManualPet, val followEngine: Boolean, val animated: Boolean)

@HiltViewModel
class PetLabViewModel @Inject constructor(
    private val sitRepository: SitRepository,
    private val recordBreakDone: RecordBreakDone,
    private val settingsRepository: SettingsRepository,
    private val clock: AppClock,
    private val notifier: ReminderNotifier,
    private val movementStore: MovementStore,
    private val permissions: MovementPermissions,
    private val stepCounter: StepCounter,
    petRepository: PetRepository,
    private val wallet: WalletRepository,
) : ViewModel() {

    private val controls = MutableStateFlow(LabControls(ManualPet(), followEngine = true, animated = true))

    // The lab deliberately does NOT register its own step listener: that would keep the sensor
    // activated and hide how the real best-effort counting behaves.
    private val movement: Flow<MovementCard> = combine(
        movementStore.info,
        stepCounter.lastReading,
        stepCounter.isCounting,
        sitRepository.status.filterNotNull(),
    ) { info, reading, counting, status ->
        MovementCard(
            enabled = info.enabled,
            registered = info.registered,
            permissionGranted = permissions.granted(),
            needsRuntimePermission = permissions.needsRuntimePermission,
            playServicesAvailable = permissions.playServicesAvailable(),
            hasStepSensor = stepCounter.hasSensor,
            currentActivity = info.currentActivity,
            log = info.logByEventTime,
            stepsSinceTimerStart = if (reading != null && info.baseline != null) {
                StepCounter.stepsSince(info.baseline, reading.total, stepCounter.bootCount())
            } else {
                null
            },
            lastStepReading = reading,
            stepListenerOn = counting,
            lastResetReason = status.lastResetReason,
            isOnWalk = status.isOnWalk,
        )
    }

    val state: StateFlow<PetLabState> = combine(
        combine(controls, petRepository.activePet, ::Pair),
        sitRepository.status.filterNotNull(),
        settingsRepository.settings,
        clock.speed,
        movement,
    ) { (c, pet), status, settings, speed, movementCard ->
        PetLabState(
            pet = pet,
            manual = c.manual,
            followEngine = c.followEngine,
            animated = c.animated,
            engine = status.toInfo(settings, speed),
            movement = movementCard,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PetLabState())

    private var celebrateJob: Job? = null

    // --- Manual pet ---

    fun setEnergy(energy: Float) = updateManual {
        it.copy(energy = energy, mood = if (it.moodFollowsEnergy) moodForEnergy(energy) else it.mood)
    }

    fun setMood(mood: Mood) {
        celebrateJob?.cancel()
        controls.update { it.copy(followEngine = false, manual = it.manual.copy(mood = mood, moodFollowsEnergy = false)) }
    }

    fun setMoodFollowsEnergy(follow: Boolean) = updateManual {
        it.copy(moodFollowsEnergy = follow, mood = if (follow) moodForEnergy(it.energy) else it.mood)
    }

    fun setAnimated(animated: Boolean) = controls.update { it.copy(animated = animated) }

    fun setFollowEngine(follow: Boolean) = controls.update { it.copy(followEngine = follow) }

    /** CLAUDE.md §5.2: just finished a break → full energy, CELEBRATE for 3 s, then HAPPY. */
    fun celebrate() {
        celebrateJob?.cancel()
        controls.update { it.copy(followEngine = false) }
        celebrateJob = viewModelScope.launch {
            updateManual { it.copy(energy = 1f, mood = Mood.CELEBRATE) }
            delay(CELEBRATE_MS)
            updateManual { it.copy(mood = Mood.HAPPY) }
        }
    }

    // --- Engine ---

    // Same path as the notification's Done, so sparks and the streak behave like the real app.
    fun done() = viewModelScope.launch { recordBreakDone(suggestedBreak()) }

    fun snooze() = viewModelScope.launch { sitRepository.snooze() }

    fun skip() = viewModelScope.launch { sitRepository.skip(suggestedBreak()) }

    fun resetTimer() = viewModelScope.launch { sitRepository.resetTimer() }

    /** Debug only: enough sparks to try adopting without doing 30 breaks first. */
    fun addSparks() = viewModelScope.launch {
        wallet.award("debug:${clock.millis()}", DEBUG_SPARKS, SparkReason.DEBUG, clock.instant())
    }

    fun setFastClock(on: Boolean) = viewModelScope.launch {
        sitRepository.setClockSpeed(if (on) AppClock.FAST_SPEED else 1)
    }

    fun setAlwaysWorkHours(on: Boolean) = viewModelScope.launch {
        settingsRepository.setSchedule(if (on) WorkSchedule.AlwaysOn else WorkSchedule.Default)
        sitRepository.checkReminder()
    }

    fun changeInterval(steps: Int) = viewModelScope.launch {
        val current = settingsRepository.current().interval
        settingsRepository.setInterval(current + steps * INTERVAL_STEP)
        sitRepository.checkReminder()
    }

    // --- Movement ---

    fun onPermissionResult() = viewModelScope.launch {
        sitRepository.checkReminder() // registers transitions and the step listener now that we may
    }

    fun setMovementEnabled(enabled: Boolean) = viewModelScope.launch { sitRepository.setMovementEnabled(enabled) }

    /** Feeds a fake 3-minute walk that ended now, to test the reset at a desk. */
    fun simulateWalk() = viewModelScope.launch {
        val now = clock.instant()
        sitRepository.simulateTransitions(
            listOf(
                MovementTransition(MovementActivity.WALKING, entered = true, at = now.minusSeconds(180)),
                MovementTransition(MovementActivity.STILL, entered = true, at = now),
            ),
        )
    }

    private fun suggestedBreak(): BreakType = BreakType.suggestedFor(clock.instant().epochSecond / 60)

    private fun updateManual(transform: (ManualPet) -> ManualPet) =
        controls.update { it.copy(manual = transform(it.manual)) }

    private fun SitStatus.toInfo(settings: SittingSettings, speed: Int) = EngineInfo(
        now = now,
        zone = clock.zone,
        isWorkTime = isWorkTime,
        sitSeconds = sitDuration.seconds,
        intervalMinutes = intervalMinutes,
        nextReminder = nextAlarmAt,
        snoozeCount = snoozeCount,
        mood = mood,
        energy = energy,
        notificationsAllowed = notifier.canPost(),
        fastClock = speed > 1,
        alwaysWorkHours = settings.schedule == WorkSchedule.AlwaysOn,
    )

    /**
     * Inverts energy = 1 − sitFraction / 1.2 (CLAUDE.md §5.2) to pick the mood:
     * HAPPY under 50 % of the interval, TIRED under 100 %, then STIFF.
     */
    private fun moodForEnergy(energy: Float): Mood {
        val sitFraction = (1f - energy) * 1.2f
        return when {
            sitFraction < 0.5f -> Mood.HAPPY
            sitFraction < 1f -> Mood.TIRED
            else -> Mood.STIFF
        }
    }
}

private const val DEBUG_SPARKS = 100
