package com.kartikkhandelwal.wigglebun.data.repo

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kartikkhandelwal.wigglebun.data.db.AppDatabase
import com.kartikkhandelwal.wigglebun.data.prefs.SettingsRepository
import com.kartikkhandelwal.wigglebun.data.prefs.SitStateStore
import com.kartikkhandelwal.wigglebun.data.time.AppClock
import com.kartikkhandelwal.wigglebun.domain.model.BreakOutcome
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.domain.sitting.SittingEngine
import com.kartikkhandelwal.wigglebun.domain.sitting.SnoozeResult
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Instant

/** Repository wiring: engine + DataStore + Room + notifier + alarm, as the receivers use it. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class)
class SitRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dataStore = PreferenceDataStoreFactory.create(scope = scope) { tmp.newFile("test.preferences_pb") }
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    private val clock = AppClock()
    private val settings = SettingsRepository(dataStore)
    private val store = SitStateStore(dataStore)
    private val alarms = context.getSystemService<AlarmManager>()!!
    private val repo = SitRepository(
        engine = SittingEngine(clock),
        clock = clock,
        settingsRepository = settings,
        store = store,
        breaks = BreakRepository(db.breakLogDao()),
        scheduler = ReminderScheduler(context, alarms, clock),
        notifier = ReminderNotifier(context, NotificationManagerCompat.from(context)).apply { createChannel() },
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
        repo.onAppStart()
        repeat(3) { assertTrue(repo.snooze() is SnoozeResult.Snoozed) }
        assertEquals(SnoozeResult.LimitReached, repo.snooze())
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
}
