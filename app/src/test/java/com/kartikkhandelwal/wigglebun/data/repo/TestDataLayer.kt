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
import com.kartikkhandelwal.wigglebun.data.prefs.writeSchedule
import com.kartikkhandelwal.wigglebun.data.time.AppClock
import com.kartikkhandelwal.wigglebun.domain.model.BreakOutcome
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.sitting.SittingEngine
import com.kartikkhandelwal.wigglebun.domain.sitting.WorkSchedule
import com.kartikkhandelwal.wigglebun.notify.ReminderNotifier
import com.kartikkhandelwal.wigglebun.notify.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * The real data layer (SitRepository, BreakRepository, WalletRepository, RecordBreakDone…) on an
 * in-memory DataStore and Room database, for Robolectric tests. Close it in @After.
 */
internal class TestDataLayer(context: Application) : AutoCloseable {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val dataStore = inMemoryPreferencesDataStore(scope)
    val db: AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    val clock = AppClock()
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
        val started = Instant.now().truncatedTo(ChronoUnit.MILLIS).minusSeconds(minutes * 60)
        store.update { it.copy(sitStartedAt = started) }
        started
    }

    fun logs(): List<BreakLogEntity> = runBlocking { breaks.observeSince(Instant.EPOCH).first() }

    fun balance(): Int = runBlocking { wallet.balance.first() }

    fun ledger() = runBlocking { db.sparkLedgerDao().all() }

    /** A completed break written straight to the log (no reward), e.g. on an earlier day. */
    fun logDone(type: BreakType, at: Instant) = runBlocking {
        db.breakLogDao().insert(BreakLogEntity(type = type, completedAt = at.toEpochMilli(), reward = type.defaultReward, outcome = BreakOutcome.DONE))
    }

    override fun close() {
        db.close()
        scope.cancel()
    }
}
