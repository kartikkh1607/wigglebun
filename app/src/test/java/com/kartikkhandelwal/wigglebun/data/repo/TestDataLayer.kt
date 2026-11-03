package com.kartikkhandelwal.wigglebun.data.repo

import android.app.AlarmManager
import android.app.Application
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.datastore.preferences.core.edit
import androidx.room.Room
import com.kartikkhandelwal.wigglebun.activity.MovementPermissions
import com.kartikkhandelwal.wigglebun.activity.MovementTracker
import com.kartikkhandelwal.wigglebun.activity.StepCounter
import com.kartikkhandelwal.wigglebun.activity.TransitionRegistrar
import com.kartikkhandelwal.wigglebun.data.db.AppDatabase
import com.kartikkhandelwal.wigglebun.data.db.BreakLogEntity
import com.kartikkhandelwal.wigglebun.data.prefs.MovementStore
import com.kartikkhandelwal.wigglebun.data.prefs.SettingsRepository
import com.kartikkhandelwal.wigglebun.data.prefs.SitStateStore
import com.kartikkhandelwal.wigglebun.data.prefs.inMemoryPreferencesDataStore
import com.kartikkhandelwal.wigglebun.data.prefs.writeDailyGoalHistory
import com.kartikkhandelwal.wigglebun.data.prefs.writeSchedule
import com.kartikkhandelwal.wigglebun.data.time.AppClock
import com.kartikkhandelwal.wigglebun.domain.model.BreakOutcome
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.rewards.DailyGoal
import com.kartikkhandelwal.wigglebun.domain.rewards.DailyGoalHistory
import com.kartikkhandelwal.wigglebun.domain.rewards.RewardRules
import com.kartikkhandelwal.wigglebun.domain.sitting.SittingEngine
import com.kartikkhandelwal.wigglebun.domain.sitting.WorkSchedule
import com.kartikkhandelwal.wigglebun.notify.ReminderNotifier
import com.kartikkhandelwal.wigglebun.notify.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * The real data layer (SitRepository, BreakRepository, WalletRepository, RecordBreakDone…) on an
 * in-memory DataStore and Room database, for Robolectric tests. Close it in @After.
 */
internal class TestDataLayer(context: Application, val clock: AppClock = AppClock()) : AutoCloseable {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val dataStore = inMemoryPreferencesDataStore(scope)
    val db: AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    val settings = SettingsRepository(dataStore, clock)
    val store = SitStateStore(dataStore)
    val breaks = BreakRepository(db.breakLogDao())
    val wallet = WalletRepository(db.sparkLedgerDao())
    private val permissions = MovementPermissions(context)
    private val stepCounter = StepCounter(context, permissions)
    val movementStore = MovementStore(dataStore, dataStore)
    val widgetUpdates = CountingWidgetRefresher()
    val pets = PetRepository(wallet, settings, clock, widgetUpdates)
    val sitRepository = SitRepository(
        engine = SittingEngine(clock),
        clock = clock,
        settingsRepository = settings,
        store = store,
        breaks = breaks,
        scheduler = ReminderScheduler(context, context.getSystemService<AlarmManager>()!!, clock),
        notifier = ReminderNotifier(context, NotificationManagerCompat.from(context)).apply { createChannel() },
        movementStore = movementStore,
        movementTracker = MovementTracker(permissions, TransitionRegistrar(context, permissions), stepCounter, movementStore),
        stepCounter = stepCounter,
        widget = widgetUpdates,
        petRepository = pets,
        scope = scope,
    )
    val recordBreakDone = RecordBreakDone(sitRepository, breaks, wallet, settings, clock)

    /**
     * Every day is a work day, always (written directly, so no work-day history is recorded and
     * the result doesn't depend on which weekday the test runs).
     */
    fun alwaysWorkHours() = runBlocking {
        dataStore.edit { it.writeSchedule(WorkSchedule.AlwaysOn) }
    }

    /** In work hours with the sit timer started [minutes] ago. */
    fun sittingFor(minutes: Long): Instant = runBlocking {
        alwaysWorkHours()
        val started = clock.instant().truncatedTo(ChronoUnit.MILLIS).minusSeconds(minutes * 60)
        store.update { it.copy(sitStartedAt = started) }
        started
    }

    fun logs(): List<BreakLogEntity> = runBlocking { breaks.observeSince(Instant.EPOCH).first() }

    fun balance(): Int = runBlocking { wallet.balance.first() }

    fun ledger() = runBlocking { db.sparkLedgerDao().all() }

    /**
     * The streak's goal has been [goal] since [from], as if the install first started then. Without
     * it, days before the first adoption keep the original rule (see [DailyGoalHistory]).
     */
    fun dailyGoalSince(from: LocalDate, goal: DailyGoal = RewardRules.DAILY_GOAL) = runBlocking {
        dataStore.edit { it.writeDailyGoalHistory(DailyGoalHistory().withChange(goal, from)) }
    }

    fun today(): LocalDate = clock.instant().atZone(clock.zone).toLocalDate()

    /** A completed break written straight to the log (no reward), e.g. on an earlier day. */
    fun logDone(type: BreakType, at: Instant) = runBlocking {
        db.breakLogDao().insert(BreakLogEntity(type = type, completedAt = at.toEpochMilli(), reward = type.defaultReward, outcome = BreakOutcome.DONE))
    }

    /**
     * Stops the background work first (SitRepository's settings collector runs checkReminder after
     * every settings write), then closes the database. The other way round, a check still running
     * hits the closed database, and the error fails whichever test runs next.
     */
    override fun close() {
        runBlocking { withTimeoutOrNull(CLOSE_TIMEOUT_MILLIS) { scope.coroutineContext.job.cancelAndJoin() } }
        scope.cancel()
        db.close()
    }

    private companion object {
        /** Bounded, so a coroutine waiting for the main thread can't hang the teardown. */
        const val CLOSE_TIMEOUT_MILLIS = 5_000L
    }
}
