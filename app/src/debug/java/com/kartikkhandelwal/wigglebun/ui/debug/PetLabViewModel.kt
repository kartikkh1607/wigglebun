package com.kartikkhandelwal.wigglebun.ui.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartikkhandelwal.wigglebun.data.prefs.SettingsRepository
import com.kartikkhandelwal.wigglebun.data.repo.SitRepository
import com.kartikkhandelwal.wigglebun.data.time.AppClock
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.domain.sitting.SitStatus
import com.kartikkhandelwal.wigglebun.domain.sitting.SittingSettings
import com.kartikkhandelwal.wigglebun.domain.sitting.WorkSchedule
import com.kartikkhandelwal.wigglebun.notify.ReminderNotifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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

data class PetLabState(
    val manual: ManualPet = ManualPet(),
    val followEngine: Boolean = true,
    val animated: Boolean = true,
    val engine: EngineInfo? = null,
) {
    val petEnergy: Float get() = if (followEngine && engine != null) engine.energy else manual.energy
    val petMood: Mood get() = if (followEngine && engine != null) engine.mood else manual.mood
}

private data class LabControls(val manual: ManualPet, val followEngine: Boolean, val animated: Boolean)

@HiltViewModel
class PetLabViewModel @Inject constructor(
    private val sitRepository: SitRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: AppClock,
    private val notifier: ReminderNotifier,
) : ViewModel() {

    private val controls = MutableStateFlow(LabControls(ManualPet(), followEngine = true, animated = true))

    val state: StateFlow<PetLabState> = combine(
        controls,
        sitRepository.status,
        settingsRepository.settings,
        clock.speed,
    ) { c, status, settings, speed ->
        PetLabState(
            manual = c.manual,
            followEngine = c.followEngine,
            animated = c.animated,
            engine = status.toInfo(settings, speed),
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

    fun done() = viewModelScope.launch { sitRepository.done(suggestedBreak()) }

    fun snooze() = viewModelScope.launch { sitRepository.snooze() }

    fun skip() = viewModelScope.launch { sitRepository.skip(suggestedBreak()) }

    fun resetTimer() = viewModelScope.launch { sitRepository.resetTimer() }

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
