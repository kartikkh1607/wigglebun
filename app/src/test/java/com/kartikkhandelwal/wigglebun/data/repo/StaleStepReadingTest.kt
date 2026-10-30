package com.kartikkhandelwal.wigglebun.data.repo

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kartikkhandelwal.wigglebun.activity.MovementPermissions
import com.kartikkhandelwal.wigglebun.activity.MovementTracker
import com.kartikkhandelwal.wigglebun.activity.StepCounter
import com.kartikkhandelwal.wigglebun.activity.TransitionRegistrar
import com.kartikkhandelwal.wigglebun.data.db.AppDatabase
import com.kartikkhandelwal.wigglebun.data.prefs.MovementStore
import com.kartikkhandelwal.wigglebun.data.prefs.SettingsRepository
import com.kartikkhandelwal.wigglebun.data.prefs.SitStateStore
import com.kartikkhandelwal.wigglebun.data.prefs.StepBaseline
import com.kartikkhandelwal.wigglebun.data.prefs.inMemoryPreferencesDataStore
import com.kartikkhandelwal.wigglebun.data.time.AppClock
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.MovementActivity
import com.kartikkhandelwal.wigglebun.domain.model.MovementTransition
import com.kartikkhandelwal.wigglebun.domain.model.ResetReason
import com.kartikkhandelwal.wigglebun.domain.sitting.SittingEngine
import com.kartikkhandelwal.wigglebun.domain.sitting.WorkSchedule
import com.kartikkhandelwal.wigglebun.notify.ReminderNotifier
import com.kartikkhandelwal.wigglebun.notify.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Restarting the timer flushes the step sensor while SitRepository holds its lock, and a flush
 * delivers its batch one reading at a time. A reading from the start of that batch is then waiting
 * for the lock, older than the baseline just saved. It must not look like the counter went back
 * (which counts every step on the counter as new, so the timer "resets by steps" right away).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class)
class StaleStepReadingTest {

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dataStore = inMemoryPreferencesDataStore(scope)
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    private val clock = AppClock()
    private val settings = SettingsRepository(dataStore, clock)
    private val store = SitStateStore(dataStore)
    private val movementStore = MovementStore(dataStore, dataStore)
    private val permissions = MovementPermissions(context)
    private val stepCounter = FlushingStepCounter(context, permissions)
    private val widget = CountingWidgetRefresher()

    /** A step sensor whose next flush delivers [nextFlush], with time in between for the collector to see the first reading. */
    private class FlushingStepCounter(context: Context, permissions: MovementPermissions) : StepCounter(context, permissions) {
        var total = START_STEPS
        var nextFlush = emptyList<Long>()

        fun publish(reading: Long) = record(reading)

        override suspend fun readTotal(): Long {
            val flush = nextFlush
            nextFlush = emptyList()
            flush.forEach { reading ->
                total = reading
                record(reading)
                delay(READING_GAP_MILLIS)
            }
            return total
        }
    }

    private lateinit var repo: SitRepository

    @Before
    fun setUp() = runBlocking {
        Settings.Global.putInt(context.contentResolver, Settings.Global.BOOT_COUNT, BOOT)
        settings.setSchedule(WorkSchedule.AlwaysOn)
        movementStore.setBaseline(StepBaseline(START_STEPS, BOOT))
        store.update { it.copy(sitStartedAt = Instant.now().minusSeconds(30 * 60)) }
        repo = SitRepository(
            engine = SittingEngine(clock),
            clock = clock,
            settingsRepository = settings,
            store = store,
            breaks = BreakRepository(db.breakLogDao()),
            scheduler = ReminderScheduler(context, context.getSystemService<AlarmManager>()!!, clock),
            notifier = ReminderNotifier(context, NotificationManagerCompat.from(context)).apply { createChannel() },
            movementStore = movementStore,
            movementTracker = MovementTracker(permissions, TransitionRegistrar(context, permissions), stepCounter, movementStore),
            stepCounter = stepCounter,
            widget = widget,
            petRepository = PetRepository(WalletRepository(db.sparkLedgerDao()), settings, clock, widget),
            scope = scope,
        )
        // The startup check reads the counter too; let it finish before arming the flush.
        withTimeout(10_000) { while (widget.count == 0) delay(10) }
    }

    @After
    fun tearDown() {
        db.close()
        scope.cancel()
    }

    /** Time for a reading waiting on the lock to be handled once the lock is free. */
    private suspend fun awaitWaitingReading() = delay(SETTLE_MILLIS)

    /** A step reset saves the reason first and the new baseline after it, so wait for both. */
    private suspend fun awaitStepsReset(baseline: StepBaseline) = withTimeout(5_000) {
        while (store.current().lastResetReason != ResetReason.STEPS || movementStore.current().baseline != baseline) delay(10)
    }

    @Test
    fun aWalkEndKeepsItsResetWhenTheFlushDeliversABatch() = runBlocking {
        val end = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        stepCounter.nextFlush = listOf(START_STEPS + 120, START_STEPS + 180)

        repo.simulateTransitions(
            listOf(
                MovementTransition(MovementActivity.WALKING, entered = true, at = end.minusSeconds(180)),
                MovementTransition(MovementActivity.STILL, entered = true, at = end),
            ),
        )
        awaitWaitingReading()

        assertEquals(ResetReason.WALKING, store.current().lastResetReason)
        assertEquals(end, store.current().sitStartedAt)
        assertEquals(StepBaseline(START_STEPS + 180, BOOT), movementStore.current().baseline)
    }

    @Test
    fun aDoneKeepsItsResetWhenTheFlushDeliversABatch() = runBlocking {
        stepCounter.nextFlush = listOf(START_STEPS + 120, START_STEPS + 180)

        repo.done(BreakType.WALK)
        val doneAt = store.current().sitStartedAt
        awaitWaitingReading()

        assertEquals(ResetReason.DONE, store.current().lastResetReason)
        assertEquals(doneAt, store.current().sitStartedAt)
        assertEquals(StepBaseline(START_STEPS + 180, BOOT), movementStore.current().baseline)
    }

    /** The real rule still works: 100 new steps after the baseline reset the timer. */
    @Test
    fun aHundredNewStepsStillReset() = runBlocking {
        stepCounter.total = START_STEPS + 100
        stepCounter.publish(START_STEPS + 100)
        awaitStepsReset(StepBaseline(START_STEPS + 100, BOOT))
    }

    /** The counter really restarting (deactivated, then back at 0) still counts as new steps. */
    @Test
    fun aCounterThatReallyWentBackStillCounts() = runBlocking {
        stepCounter.total = 150
        stepCounter.publish(150)
        awaitStepsReset(StepBaseline(150, BOOT))
    }

    private companion object {
        const val BOOT = 7
        const val START_STEPS = 5_000L
        const val READING_GAP_MILLIS = 150L
        const val SETTLE_MILLIS = 400L
    }
}
