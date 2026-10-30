package com.kartikkhandelwal.wigglebun.data.repo

import com.kartikkhandelwal.wigglebun.activity.MovementTracker
import com.kartikkhandelwal.wigglebun.activity.StepCounter
import com.kartikkhandelwal.wigglebun.data.ApplicationScope
import com.kartikkhandelwal.wigglebun.data.prefs.MovementStore
import com.kartikkhandelwal.wigglebun.data.prefs.ReceivedTransition
import com.kartikkhandelwal.wigglebun.data.prefs.SettingsRepository
import com.kartikkhandelwal.wigglebun.data.prefs.SitStateStore
import com.kartikkhandelwal.wigglebun.data.time.AppClock
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.MovementTransition
import com.kartikkhandelwal.wigglebun.domain.sitting.SitState
import com.kartikkhandelwal.wigglebun.domain.sitting.SitStatus
import com.kartikkhandelwal.wigglebun.domain.sitting.SittingEngine
import com.kartikkhandelwal.wigglebun.domain.sitting.SittingSettings
import com.kartikkhandelwal.wigglebun.domain.sitting.SnoozeResult
import com.kartikkhandelwal.wigglebun.notify.ReminderNotifier
import com.kartikkhandelwal.wigglebun.notify.ReminderScheduler
import com.kartikkhandelwal.wigglebun.widget.WidgetRefresher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

/**
 * Owns the sit timer: runs the [SittingEngine] over the persisted state and settings,
 * applies Done / Snooze / Skip and movement, writes the BreakLog, posts reminders and keeps
 * the alarm, movement tracking and home-screen widget in step with work hours.
 */
@Singleton
class SitRepository @Inject constructor(
    private val engine: SittingEngine,
    private val clock: AppClock,
    private val settingsRepository: SettingsRepository,
    private val store: SitStateStore,
    private val breaks: BreakRepository,
    private val scheduler: ReminderScheduler,
    private val notifier: ReminderNotifier,
    private val movementStore: MovementStore,
    private val movementTracker: MovementTracker,
    private val stepCounter: StepCounter,
    private val widget: WidgetRefresher,
    private val petRepository: PetRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    // UI buttons, notification actions and movement events can all arrive at once.
    private val mutex = Mutex()

    private val ticker: Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(TICK_MILLIS.milliseconds)
        }
    }

    /**
     * Live status, recomputed every second while someone is watching (Home, Pet lab).
     * Null until the persisted state and settings have loaded, so a cold start never shows a
     * made-up default pet for a frame before the real one (the "cold-start flicker").
     */
    val status: StateFlow<SitStatus?> =
        combine(store.state, settingsRepository.settings, clock.speed, ticker) { state, settings, _, _ ->
            engine.status(state, settings)
        }
            // While the app is open, post the reminder right on time instead of waiting for the
            // inexact alarm (which may run late). Deduped by lastNotifiedDue, so never twice.
            .onEach { if (it.shouldNotify) checkReminder() }
            .stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    init {
        // Settings changes (work hours, interval) move the next reminder and the work-hours
        // boundaries, so re-arm the alarm and movement tracking whoever changed them. No drop(1):
        // a change made before this collector subscribes would otherwise be lost; re-checking the
        // initial value too is harmless (mutex + each due is notified once).
        scope.launch { settingsRepository.settings.collect { checkReminder() } }
        // Each new step-counter batch is a chance to apply the 100-step rule.
        scope.launch { stepCounter.lastReading.filterNotNull().collect { onStepReading() } }
    }

    /** First run starts the timer now (not at 09:00); every start re-arms alarm and tracking. */
    suspend fun onAppStart() {
        mutex.withLock {
            if (store.current().sitStartedAt == null) {
                store.update { engine.resetTimer(it) }
                captureStepBaseline()
            }
        }
        checkReminder()
    }

    /**
     * The central check, run on every alarm, boot/update, app start and action:
     * apply the step fallback, sync movement tracking and the widget's periodic refresh with
     * work hours, post a due reminder, then arm the next wake-up (next reminder or work-hours
     * boundary, so this runs again when a session starts and when it ends).
     */
    suspend fun checkReminder() = mutex.withLock {
        val settings = settingsRepository.current()
        var state = applySteps(store.current(), settings)

        val isWorkTime = engine.status(state, settings).isWorkTime
        widget.syncPeriodicRefresh(isWorkTime)
        val tracking = movementTracker.sync(isWorkTime)
        if (!tracking && (state.movingSince != null || state.activeMoving.isNotEmpty())) {
            // Not tracking any more, so an open walk will never get its exit event.
            state = store.update { engine.clearMovement(it) }
        } else if (engine.closeOverlongWalk(state) != state) {
            // Tracking, but the walk has been open longer than the cap: its exit was lost.
            state = store.update { engine.closeOverlongWalk(it) }
            onMovementReset()
        }

        val status = engine.status(state, settings)
        val due = status.reminderDue
        if (status.shouldNotify && due != null) {
            notifier.showReminder(
                pet = petRepository.activePet.first(),
                suggested = BreakType.suggestedFor(due.epochSecond / 60),
                snoozesLeft = status.snoozesLeft,
            )
            // Marked even if notifications are blocked, so we don't retry every second.
            state = store.update { engine.markNotified(it, due) }
        }
        armAlarm(state, settings)
    }

    /** Activity Recognition transitions from Play services, in chronological order. */
    suspend fun onReceivedTransitions(events: List<ReceivedTransition>, bootCount: Int) = mutex.withLock {
        val accepted = movementStore.acceptAndLog(events, bootCount)
        if (accepted.isEmpty()) return@withLock // replays/redeliveries at or below the watermark
        // Event times are real wall-clock time; the engine works in app time (debug fast clock).
        applyTransitions(accepted.map { it.copy(at = clock.fromRealEpochMillis(it.at.toEpochMilli())) })
    }

    /** Debug only: feed made-up transitions (already in app time), bypassing the watermark. */
    suspend fun simulateTransitions(events: List<MovementTransition>) = mutex.withLock {
        movementStore.appendLog(events)
        applyTransitions(events)
    }

    private suspend fun applyTransitions(appEvents: List<MovementTransition>) {
        val before = store.current()
        var state = store.update { current -> appEvents.fold(current) { s, e -> engine.onMovement(s, e) } }
        val settings = settingsRepository.current()
        if (state.sitStartedAt != before.sitStartedAt) {
            onMovementReset()
        } else {
            state = applySteps(state, settings)
        }
        armAlarm(state, settings)
    }

    suspend fun done(type: BreakType) = mutex.withLock {
        val state = store.update { engine.done(it) }
        breaks.logDone(type, clock.instant())
        notifier.cancel()
        captureStepBaseline()
        armAlarm(state, settingsRepository.current())
    }

    suspend fun skip(type: BreakType) = mutex.withLock {
        val state = store.update { engine.skip(it) }
        breaks.logSkipped(type, clock.instant())
        notifier.cancel()
        captureStepBaseline()
        armAlarm(state, settingsRepository.current())
    }

    suspend fun snooze(): SnoozeResult = mutex.withLock {
        val settings = settingsRepository.current()
        val result = engine.snooze(store.current(), settings)
        if (result is SnoozeResult.Snoozed) {
            store.update { result.state }
            notifier.cancel()
            armAlarm(result.state, settings)
        }
        result
    }

    suspend fun resetTimer() = mutex.withLock {
        val state = store.update { engine.resetTimer(it) }
        notifier.cancel()
        captureStepBaseline()
        armAlarm(state, settingsRepository.current())
    }

    /** User switch for movement detection; turning it off unregisters right away. */
    suspend fun setMovementEnabled(enabled: Boolean) {
        movementStore.setEnabled(enabled)
        checkReminder()
    }

    /** Debug fast clock: switching speed restarts the timer so old timestamps don't confuse it. */
    suspend fun setClockSpeed(speed: Int) {
        clock.setSpeed(speed)
        resetTimer()
    }

    private suspend fun onStepReading() = mutex.withLock {
        // Read the value inside the lock. A reading picked up before it can be older than the
        // baseline by now (a flush delivers its batch while a reset holds the lock), which would
        // look like the counter went back and count every step on it as new.
        val total = stepCounter.lastReading.value?.total ?: return@withLock
        val settings = settingsRepository.current()
        val state = store.current()
        val after = applySteps(state, settings, total)
        if (after != state) armAlarm(after, settings)
    }

    /** Step-counter fallback (work hours only, best effort): ≥ 100 steps since the timer started resets it. */
    private suspend fun applySteps(state: SitState, settings: SittingSettings, knownTotal: Long? = null): SitState {
        if (!engine.status(state, settings).isWorkTime) return state
        val total = knownTotal ?: stepCounter.readTotal() ?: return state
        val baseline = movementStore.current().baseline
        if (baseline == null) {
            movementStore.setBaseline(stepCounter.baselineNow(total))
            return state
        }
        val steps = StepCounter.stepsSince(baseline, total, stepCounter.bootCount())
        val after = engine.onSteps(state, steps)
        if (after == state) return state
        val saved = store.update { engine.onSteps(it, steps) }
        movementStore.setBaseline(stepCounter.baselineNow(total))
        notifier.cancel()
        return saved
    }

    private suspend fun onMovementReset() {
        notifier.cancel() // no point nagging someone who just walked
        captureStepBaseline()
    }

    /** Remember the step counter value when the timer restarts (cleared if it can't be read). */
    private suspend fun captureStepBaseline() {
        movementStore.setBaseline(stepCounter.readTotal()?.let(stepCounter::baselineNow))
    }

    /** Runs after every change to the sit state, so it also redraws the widget. */
    private fun armAlarm(state: SitState, settings: SittingSettings) {
        scheduler.schedule(engine.status(state, settings).nextWakeAt)
        widget.requestUpdate()
    }

    private companion object {
        const val TICK_MILLIS = 1_000L
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
