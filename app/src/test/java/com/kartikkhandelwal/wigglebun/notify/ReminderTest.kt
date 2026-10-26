package com.kartikkhandelwal.wigglebun.notify

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kartikkhandelwal.wigglebun.data.time.AppClock
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class)
class ReminderTest {

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val notifier = ReminderNotifier(context, NotificationManagerCompat.from(context)).apply { createChannel() }
    private val systemNotifications = shadowOf(context.getSystemService<NotificationManager>()!!)

    @Test
    fun withoutPermissionReminderIsSkippedQuietly() {
        shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertFalse(notifier.canPost())
        assertFalse(notifier.showReminder("Bun", BreakType.NECK, snoozesLeft = 3))
        assertTrue(systemNotifications.allNotifications.isEmpty())
    }

    @Test
    fun reminderHasDoneSnoozeSkip() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertTrue(notifier.showReminder("Mochi", BreakType.NECK, snoozesLeft = 3))
        val n = systemNotifications.allNotifications.single()
        assertEquals(ReminderNotifier.CHANNEL_ID, n.channelId)
        assertEquals(listOf("Done", "Snooze 10 min", "Skip"), n.actions.map { it.title.toString() })
        assertTrue(n.extras.getCharSequence("android.text").toString().contains("Mochi"))
        assertEquals("Time for a Bun Break", n.extras.getCharSequence("android.title").toString())
    }

    @Test
    fun snoozeHiddenWhenNoneLeft() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        notifier.showReminder(null, BreakType.WALK, snoozesLeft = 0)
        val n = systemNotifications.allNotifications.single()
        assertEquals(listOf("Done", "Skip"), n.actions.map { it.title.toString() })
        assertTrue(n.extras.getCharSequence("android.text").toString().contains("Bun"))
    }

    @Test
    fun schedulesOneInexactAlarmInRealTime() {
        val alarmManager = context.getSystemService<AlarmManager>()!!
        val clock = AppClock()
        val scheduler = ReminderScheduler(context, alarmManager, clock)
        val shadow = shadowOf(alarmManager)

        val at = clock.instant().plus(Duration.ofMinutes(45))
        scheduler.schedule(at)
        val alarm = shadow.scheduledAlarms.single()
        assertEquals(AlarmManager.RTC_WAKEUP, alarm.type)
        assertEquals(at.toEpochMilli(), alarm.triggerAtMs)
        assertTrue(alarm.isAllowWhileIdle)

        // Fast clock: 45 app-minutes away is ~45 real seconds away.
        clock.setSpeed(AppClock.FAST_SPEED)
        val realNow = System.currentTimeMillis()
        scheduler.schedule(clock.instant().plus(Duration.ofMinutes(45)))
        val fast = shadow.scheduledAlarms.single() // same PendingIntent replaces the old alarm
        assertTrue(fast.triggerAtMs - realNow in 44_000..46_000)

        scheduler.schedule(null)
        assertNull(shadow.peekNextScheduledAlarm())
    }
}
