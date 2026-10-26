package com.kartikkhandelwal.wigglebun.data.repo

import com.kartikkhandelwal.wigglebun.data.ApplicationScope
import com.kartikkhandelwal.wigglebun.data.prefs.SettingsRepository
import com.kartikkhandelwal.wigglebun.data.prefs.SitStateStore
import com.kartikkhandelwal.wigglebun.data.time.AppClock
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.sitting.SitState
import com.kartikkhandelwal.wigglebun.domain.sitting.SitStatus
import com.kartikkhandelwal.wigglebun.domain.sitting.SittingEngine
import com.kartikkhandelwal.wigglebun.domain.sitting.SittingSettings
import com.kartikkhandelwal.wigglebun.domain.sitting.SnoozeResult
import com.kartikkhandelwal.wigglebun.notify.ReminderNotifier
import com.kartikkhandelwal.wigglebun.notify.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the sit timer: runs the [SittingEngine] over the persisted state and settings,
 * applies Done / Snooze / Skip, writes the BreakLog, posts reminders and keeps the alarm armed.
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
    @ApplicationScope scope: CoroutineScope,
) {
    // UI buttons and notification receivers can act at the same time.
    private val mutex = Mutex()

    private val ticker: Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(TICK_MILLIS)
        }
    }

    /** Live status, recomputed every second while someone is watching (Home, Pet lab). */
    val status: StateFlow<SitStatus> =
        combine(store.state, settingsRepository.settings, clock.speed, ticker) { state, settings, _, _ ->
            engine.status(state, settings)
        }
            // While the app is open, post the reminder right on time instead of waiting for the
            // inexact alarm (which may run late). Deduped by lastNotifiedDue, so never twice.
            .onEach { if (it.shouldNotify) checkReminder() }
            .stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), engine.status(SitState(), SittingSettings()))

    /** First run starts the timer now (not at 09:00); every start re-arms the alarm. */
    suspend fun onAppStart() {
        mutex.withLock {
            if (store.current().sitStartedAt == null) store.update { engine.resetTimer(it) }
        }
        checkReminder()
    }

    /** Post the reminder if one is due, then arm the alarm for the next one. */
    suspend fun checkReminder() = mutex.withLock {
        val settings = settingsRepository.current()
        var state = store.current()
        val status = engine.status(state, settings)
        val due = status.reminderDue
        if (status.shouldNotify && due != null) {
            notifier.showReminder(
                petName = settingsRepository.petName.first(),
                suggested = BreakType.suggestedFor(due.epochSecond / 60),
                snoozesLeft = status.snoozesLeft,
            )
            // Marked even if notifications are blocked, so we don't retry every second.
            state = store.update { engine.markNotified(it, due) }
        }
        armAlarm(state, settings)
    }

    suspend fun done(type: BreakType) = mutex.withLock {
        val state = store.update { engine.done(it) }
        breaks.logDone(type, clock.instant())
        notifier.cancel()
        armAlarm(state, settingsRepository.current())
    }

    suspend fun skip(type: BreakType) = mutex.withLock {
        val state = store.update { engine.skip(it) }
        breaks.logSkipped(type, clock.instant())
        notifier.cancel()
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
        armAlarm(state, settingsRepository.current())
    }

    /** Debug fast clock: switching speed restarts the timer so old timestamps don't confuse it. */
    suspend fun setClockSpeed(speed: Int) {
        clock.setSpeed(speed)
        resetTimer()
    }

    private fun armAlarm(state: SitState, settings: SittingSettings) {
        scheduler.schedule(engine.status(state, settings).nextAlarmAt)
    }

    private companion object {
        const val TICK_MILLIS = 1_000L
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
