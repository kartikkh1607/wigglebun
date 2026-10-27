package com.kartikkhandelwal.wigglebun.data.repo

import android.app.AlarmManager
import android.app.Application
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
import com.kartikkhandelwal.wigglebun.data.prefs.inMemoryPreferencesDataStore
import com.kartikkhandelwal.wigglebun.data.time.AppClock
import com.kartikkhandelwal.wigglebun.domain.model.BreakOutcome
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.ResetReason
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.temporal.ChronoUnit

/** Home's Done / Skip rules (M5). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class)
class BreakActionsTest {

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dataStore = inMemoryPreferencesDataStore(scope)
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    private val clock = AppClock()
    private val settings = SettingsRepository(dataStore)
    private val store = SitStateStore(dataStore)
    private val breaks = BreakRepository(db.breakLogDao())
    private val permissions = MovementPermissions(context)
    private val stepCounter = StepCounter(context, permissions)
    private val movementStore = MovementStore(dataStore)
    private val sitRepository = SitRepository(
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
        scope = scope,
    )
    private val actions = BreakActions(sitRepository, breaks, clock)

    @After
    fun tearDown() {
        db.close()
        scope.cancel()
    }

    private fun logs() = runBlocking { breaks.observeSince(Instant.EPOCH).first() }

    private fun sittingFor(minutes: Long): Instant = runBlocking {
        settings.setSchedule(WorkSchedule.AlwaysOn)
        val started = Instant.now().truncatedTo(ChronoUnit.MILLIS).minusSeconds(minutes * 60)
        store.update { it.copy(sitStartedAt = started) }
        started
    }

    @Test
    fun skipBeforeDueOnlyLogsAndKeepsTheTimer() = runBlocking {
        val started = sittingFor(5) // due in 40 min
        actions.skip(BreakType.NECK, reminderDue = false)
        assertEquals(started, store.current().sitStartedAt) // not a cheat: the pet stays as tired as before
        val log = logs().single()
        assertEquals(BreakType.NECK, log.type)
        assertEquals(BreakOutcome.SKIPPED, log.outcome)
        assertEquals(0, log.reward)
    }

    @Test
    fun skipWhenDueIsTheNotificationSkip() = runBlocking {
        val started = sittingFor(50)
        actions.skip(BreakType.WALK, reminderDue = true)
        val state = store.current()
        assert(state.sitStartedAt!!.isAfter(started))
        assertEquals(ResetReason.SKIP, state.lastResetReason)
        assertEquals(BreakOutcome.SKIPPED, logs().single().outcome)
    }

    @Test
    fun repeatDoneOfTheSameTypeStillResetsTheTimer() = runBlocking {
        sittingFor(30)
        actions.done(BreakType.WALK)
        val firstReset = store.current().sitStartedAt!!
        // Later the same day: walked again from the focused card.
        store.update { it.copy(sitStartedAt = firstReset.minusSeconds(50 * 60)) }
        actions.done(BreakType.WALK)
        val state = store.current()
        assert(!state.sitStartedAt!!.isBefore(firstReset)) // back to a fresh sit, not 50 min in
        assertEquals(ResetReason.DONE, state.lastResetReason)
        assertEquals(listOf(BreakOutcome.DONE, BreakOutcome.DONE), logs().map { it.outcome })
        assertEquals(setOf(BreakType.WALK), logs().map { it.type }.toSet())
    }

    @Test
    fun doneResetsCelebratesAndLogsTheType() = runBlocking {
        sittingFor(30)
        actions.done(BreakType.WATER)
        assertEquals(ResetReason.DONE, store.current().lastResetReason)
        val log = logs().single()
        assertEquals(BreakType.WATER, log.type)
        assertEquals(BreakOutcome.DONE, log.outcome)
    }
}
