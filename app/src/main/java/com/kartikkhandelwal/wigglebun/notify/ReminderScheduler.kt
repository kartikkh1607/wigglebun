package com.kartikkhandelwal.wigglebun.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.kartikkhandelwal.wigglebun.data.time.AppClock
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Arms one alarm for the next reminder.
 *
 * Battery rules (CLAUDE.md §2.7):
 * - Inexact only: setAndAllowWhileIdle lets the system batch the alarm, so it can fire
 *   seconds to minutes late. That's fine for "time to stretch" and needs no
 *   SCHEDULE_EXACT_ALARM permission.
 * - "AllowWhileIdle" means it may still fire in Doze, but Doze rate-limits these to
 *   roughly one per app every ~9 minutes.
 * - No foreground service: nothing runs between alarms.
 * - Alarms are wiped on reboot and on force-stop; RescheduleReceiver re-arms after boot,
 *   and the app re-arms on every start.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager,
    private val clock: AppClock,
) {
    /** Schedule the reminder check at [at] (app time), or cancel it when null. */
    fun schedule(at: Instant?) {
        val pending = alarmIntent()
        if (at == null) {
            alarmManager.cancel(pending)
            return
        }
        // Convert app time (may be the debug fast clock) to real wall-clock time.
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, clock.toRealEpochMillis(at), pending)
    }

    private fun alarmIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, ReminderAlarmReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private companion object {
        const val REQUEST_CODE = 100
    }
}
