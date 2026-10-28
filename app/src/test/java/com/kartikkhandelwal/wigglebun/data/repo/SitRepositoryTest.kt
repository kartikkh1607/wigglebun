package com.kartikkhandelwal.wigglebun.data.repo

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
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
import com.kartikkhandelwal.wigglebun.data.prefs.ReceivedTransition
import com.kartikkhandelwal.wigglebun.data.prefs.SettingsRepository
import com.kartikkhandelwal.wigglebun.data.prefs.SitStateStore
import com.kartikkhandelwal.wigglebun.data.prefs.inMemoryPreferencesDataStore
import com.kartikkhandelwal.wigglebun.data.time.AppClock
import com.kartikkhandelwal.wigglebun.domain.model.BreakOutcome
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.domain.model.MovementActivity
import com.kartikkhandelwal.wigglebun.domain.model.MovementTransition
import com.kartikkhandelwal.wigglebun.domain.model.ResetReason
import com.kartikkhandelwal.wigglebun.domain.sitting.SittingEngine
import com.kartikkhandelwal.wigglebun.domain.sitting.SnoozeResult
import com.kartikkhandelwal.wigglebun.domain.sitting.WorkSchedule
import com.kartikkhandelwal.wigglebun.notify.ReminderNotifier
import com.kartikkhandelwal.wigglebun.notify.ReminderScheduler
import com.kartikkhandelwal.wigglebun.widget.WidgetRefresher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Repository wiring: engine + DataStore + Room + notifier + alarm, as the receivers use it. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class)
class SitRepositoryTest {

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dataStore = inMemoryPreferencesDataStore(scope)
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    private val clock = AppClock()
    private val settings = SettingsRepository(dataStore, clock)
    private val store = SitStateStore(dataStore)
    private val alarms = context.getSystemService<AlarmManager>()!!
    private val permissions = MovementPermissions(context)
    private val movementStore = MovementStore(dataStore, dataStore)
    private val stepCounter = StepCounter(context, permissions)
    private val widgetUpdates = CountingWidgetRefresher()
    private val repo = SitRepository(
        engine = SittingEngine(clock),
        clock = clock,
        settingsRepository = settings,
        store = store,
        breaks = BreakRepository(db.breakLogDao()),
        scheduler = ReminderScheduler(context, alarms, clock),
        notifier = ReminderNotifier(context, NotificationManagerCompat.from(context)).apply { createChannel() },
        movementStore = movementStore,
        movementTracker = MovementTracker(permissions, TransitionRegistrar(context, permissions), stepCounter, movementStore),
        stepCounter = stepCounter,
        widget = widgetUpdates,
        petRepository = PetRepository(WalletRepository(db.sparkLedgerDao()), settings, clock, widgetUpdates),
        scope = scope,
    )

    @After
    fun tearDown() {
        db.close()
        scope.cancel()
    }

    private fun loggedBreaks() = runBlocking { db.breakLogDao().observeSince(0).first() }

    @Test
    fun appStartStartsTimerAndArmsAlarm() = runBlocking {
        settings.setSchedule(WorkSchedule.AlwaysOn)
        repo.onAppStart()
        assertNotNull(store.current().sitStartedAt)
        assertNotNull(shadowOf(alarms).peekNextScheduledAlarm())
    }

    @Test
    fun doneLogsRewardAndCelebrates() = runBlocking {
        settings.setSchedule(WorkSchedule.AlwaysOn)
        repo.onAppStart()
        repo.done(BreakType.WALK)
        val log = loggedBreaks().single()
        assertEquals(BreakOutcome.DONE, log.outcome)
        assertEquals(10, log.reward)
        assertEquals(Mood.CELEBRATE, SittingEngine(clock).status(store.current(), settings.current()).mood)
    }

    @Test
    fun skipLogsWithoutReward() = runBlocking {
        settings.setSchedule(WorkSchedule.AlwaysOn)
        repo.skip(BreakType.NECK)
        val log = loggedBreaks().single()
        assertEquals(BreakOutcome.SKIPPED, log.outcome)
        assertEquals(0, log.reward)
    }

    @Test
    fun snoozeStopsAfterThree() = runBlocking {
        settings.setSchedule(WorkSchedule.AlwaysOn)
        settings.setInterval(20)
        // Sat for 2 h: the reminder is due, and each 10-min snooze lands in the past too.
        store.update { it.copy(sitStartedAt = Instant.now().minusSeconds(2 * 60 * 60)) }
        assertTrue(repo.snooze() is SnoozeResult.Snoozed)
        repeat(2) {
            store.update { s -> s.copy(snoozedUntil = s.snoozedUntil!!.minusSeconds(11 * 60)) } // snooze expired
            assertTrue(repo.snooze() is SnoozeResult.Snoozed)
        }
        store.update { s -> s.copy(snoozedUntil = s.snoozedUntil!!.minusSeconds(11 * 60)) }
        assertEquals(SnoozeResult.LimitReached, repo.snooze())
    }

    /** Review fix: snoozing a reminder that isn't due yet changes nothing. */
    @Test
    fun snoozeBeforeDueChangesNothing() = runBlocking {
        settings.setSchedule(WorkSchedule.AlwaysOn)
        repo.onAppStart() // just started: due in 45 min
        val before = store.current()
        assertEquals(SnoozeResult.NotDue, repo.snooze())
        assertEquals(before, store.current())
    }

    /** Review fix (cold-start flicker): no made-up status before the saved state has loaded. */
    @Test
    fun statusIsNullUntilTheSavedStateLoads() = runBlocking {
        settings.setSchedule(WorkSchedule.AlwaysOn)
        store.update { it.copy(sitStartedAt = Instant.now().minusSeconds(30 * 60)) }
        assertNull(repo.status.value)
        val first = repo.status.filterNotNull().first()
        assertEquals(30, first.sitMinutes) // the real persisted timer, not a 0-minute default
    }

    /** Review fix: changing settings re-arms the alarm without anyone calling checkReminder(). */
    @Test
    fun settingsChangeReArmsTheAlarm() = runBlocking {
        settings.setSchedule(WorkSchedule.AlwaysOn)
        repo.onAppStart()
        val start = store.current().sitStartedAt!!
        val shadow = shadowOf(alarms)
        assertEquals(start.plusSeconds(45 * 60).toEpochMilli(), shadow.peekNextScheduledAlarm()!!.triggerAtMs)

        settings.setInterval(20)
        val expected = start.plusSeconds(20 * 60).toEpochMilli()
        val deadline = System.currentTimeMillis() + 5_000
        while (shadow.peekNextScheduledAlarm()?.triggerAtMs != expected && System.currentTimeMillis() < deadline) {
            Thread.sleep(50)
        }
        assertEquals(expected, shadow.peekNextScheduledAlarm()!!.triggerAtMs)
    }

    @Test
    fun walkResetsTimerWithoutBreakLogOrReward() = runBlocking {
        settings.setSchedule(WorkSchedule.AlwaysOn)
        store.update { it.copy(sitStartedAt = Instant.now().minusSeconds(30 * 60)) }
        val now = Instant.now().truncatedTo(ChronoUnit.MILLIS) // DataStore keeps millis
        repo.simulateTransitions(
            listOf(
                MovementTransition(MovementActivity.WALKING, entered = true, at = now.minusSeconds(180)),
                MovementTransition(MovementActivity.STILL, entered = true, at = now),
            ),
        )
        assertEquals(ResetReason.WALKING, store.current().lastResetReason)
        assertEquals(now, store.current().sitStartedAt)
        assertTrue(loggedBreaks().isEmpty())
        val log = movementStore.current().log
        assertEquals(listOf(MovementActivity.STILL, MovementActivity.WALKING), log.map { it.activity })
        assertEquals(MovementActivity.STILL, movementStore.current().currentActivity)
    }

    /** Play services replays the current state on registration: same elapsedRealtime, ignored. */
    @Test
    fun replayedTransitionsAreIgnoredByTheWatermark() = runBlocking {
        settings.setSchedule(WorkSchedule.AlwaysOn)
        val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        val walk = listOf(
            ReceivedTransition(MovementTransition(MovementActivity.WALKING, true, now.minusSeconds(180)), elapsedRealtimeNanos = 1_000),
            ReceivedTransition(MovementTransition(MovementActivity.STILL, true, now), elapsedRealtimeNanos = 181_000),
        )
        repo.onReceivedTransitions(walk, bootCount = 3)
        assertEquals(now, store.current().sitStartedAt)
        assertEquals(2, movementStore.current().log.size)

        repo.resetTimer()
        val afterReset = store.current().sitStartedAt
        repo.onReceivedTransitions(listOf(walk.last()), bootCount = 3) // replay of the STILL enter
        assertEquals(afterReset, store.current().sitStartedAt)
        assertEquals(2, movementStore.current().log.size)

        // After a reboot elapsedRealtime restarts, so a small value is new again.
        val afterReboot = ReceivedTransition(MovementTransition(MovementActivity.WALKING, true, now.plusSeconds(5)), 500)
        repo.onReceivedTransitions(listOf(afterReboot), bootCount = 4)
        assertEquals(3, movementStore.current().log.size)
    }

    /** Review item, through the simulate path (no watermark): the stale STILL is ignored by the engine. */
    @Test
    fun staleStillOnSimulatePathKeepsRunningActive() = runBlocking {
        settings.setSchedule(WorkSchedule.AlwaysOn)
        val t0 = Instant.now().truncatedTo(ChronoUnit.MILLIS).minusSeconds(10 * 60)
        store.update { it.copy(sitStartedAt = t0) }
        fun at(minutes: Long) = t0.plusSeconds(minutes * 60)
        repo.simulateTransitions(
            listOf(
                MovementTransition(MovementActivity.WALKING, true, at(1)),
                MovementTransition(MovementActivity.RUNNING, true, at(3)),
                MovementTransition(MovementActivity.STILL, true, t0.minusSeconds(30 * 60)), // replay
                MovementTransition(MovementActivity.WALKING, false, at(5)),
            ),
        )
        assertEquals(setOf(MovementActivity.RUNNING), store.current().activeMoving)
        assertEquals(t0, store.current().sitStartedAt)

        repo.simulateTransitions(listOf(MovementTransition(MovementActivity.RUNNING, false, at(7))))
        assertEquals(at(7), store.current().sitStartedAt)
        assertEquals(ResetReason.WALKING, store.current().lastResetReason)
    }

    @Test
    fun withoutPermissionNothingIsRegisteredAndManualStillWorks() = runBlocking {
        settings.setSchedule(WorkSchedule.AlwaysOn)
        repo.onAppStart()
        assertEquals(false, movementStore.current().registered)
        repo.done(BreakType.STAND_UP)
        assertEquals(ResetReason.DONE, store.current().lastResetReason)
    }

    @Test
    fun dueReminderIsPostedOnceAndMarked() = runBlocking {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        settings.setSchedule(WorkSchedule.AlwaysOn)
        settings.setInterval(20)
        // Started 25 minutes ago: past the 20-minute interval.
        store.update { it.copy(sitStartedAt = Instant.now().minusSeconds(25 * 60)) }

        repo.checkReminder()
        repo.checkReminder()

        val posted = shadowOf(context.getSystemService<NotificationManager>()!!).allNotifications
        assertEquals(1, posted.size)
        assertNotNull(store.current().lastNotifiedDue)
    }

    @Test
    fun everyStateChangeRedrawsTheWidget() = runBlocking {
        settings.setSchedule(WorkSchedule.AlwaysOn)
        repo.onAppStart()
        val afterStart = widgetUpdates.count
        assertTrue(afterStart > 0)

        repo.done(BreakType.WALK)
        assertTrue(widgetUpdates.count > afterStart)
        val afterDone = widgetUpdates.count

        repo.skip(BreakType.NECK)
        assertTrue(widgetUpdates.count > afterDone)
    }

    /**
     * M7 review: the widget's 15-minute refresh runs only during work hours. Each step is what
     * the reminder alarm runs at that boundary (checkReminder); moving the schedule stands in
     * for the clock moving.
     */
    @Test
    fun periodicWidgetRefreshRunsOnlyDuringWorkHours() = runBlocking {
        val zone = ZoneId.systemDefault()
        val shadow = shadowOf(alarms)

        // Before work (starts in an hour): no refresh, and the alarm is set for the start.
        val beforeWork = sessionFromNow(startIn = 60, endIn = 120)
        settings.setSchedule(beforeWork)
        repo.onAppStart()
        assertEquals(false, widgetUpdates.periodicRefresh)
        val start = beforeWork.nextSessionAfter(Instant.now(), zone)!!.start
        assertEquals(start.toEpochMilli(), shadow.peekNextScheduledAlarm()!!.triggerAtMs)

        // The session has started (ends in 10 min, before the 45-min reminder): refresh on,
        // and the alarm is set for the end.
        val atWork = sessionFromNow(startIn = -60, endIn = 10)
        settings.setSchedule(atWork)
        repo.checkReminder()
        assertEquals(true, widgetUpdates.periodicRefresh)
        val end = atWork.sessionAt(Instant.now(), zone)!!.end
        assertEquals(end.toEpochMilli(), shadow.peekNextScheduledAlarm()!!.triggerAtMs)

        // The session has ended: refresh off.
        settings.setSchedule(sessionFromNow(startIn = -120, endIn = -60))
        repo.checkReminder()
        assertEquals(false, widgetUpdates.periodicRefresh)

        // Immediate redraws still happen outside work hours.
        val redraws = widgetUpdates.count
        repo.skip(BreakType.NECK)
        assertTrue(widgetUpdates.count > redraws)
    }

    /** Every day, from [startIn] to [endIn] minutes from now in local time (wrapping past midnight). */
    private fun sessionFromNow(startIn: Int, endIn: Int): WorkSchedule {
        val now = LocalTime.now()
        val minute = now.hour * 60 + now.minute
        return WorkSchedule(
            days = DayOfWeek.entries.toSet(),
            startMinute = Math.floorMod(minute + startIn, WorkSchedule.MINUTES_PER_DAY),
            endMinute = Math.floorMod(minute + endIn, WorkSchedule.MINUTES_PER_DAY),
        )
    }
}

/** Counts redraw requests and records the periodic-refresh switch, instead of touching Glance. */
internal class CountingWidgetRefresher : WidgetRefresher {
    @Volatile
    var count = 0
        private set

    /** The last [syncPeriodicRefresh] value; null until the first sync. */
    @Volatile
    var periodicRefresh: Boolean? = null
        private set

    override fun requestUpdate() {
        count++
    }

    override suspend fun syncPeriodicRefresh(isWorkTime: Boolean) {
        periodicRefresh = isWorkTime
    }
}
