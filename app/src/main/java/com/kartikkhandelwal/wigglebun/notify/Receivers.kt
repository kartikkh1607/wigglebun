package com.kartikkhandelwal.wigglebun.notify

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kartikkhandelwal.wigglebun.data.ApplicationScope
import com.kartikkhandelwal.wigglebun.data.repo.SitRepository
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface ReceiverEntryPoint {
    fun sitRepository(): SitRepository

    @ApplicationScope
    fun scope(): CoroutineScope
}

/**
 * Runs [block] off the main thread. goAsync() keeps the process alive until finish()
 * (the system allows ~10 s), which is plenty for a DataStore write and an alarm.
 */
internal fun BroadcastReceiver.runAsync(context: Context, block: suspend (SitRepository) -> Unit) {
    val pending = goAsync()
    val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, ReceiverEntryPoint::class.java)
    entryPoint.scope().launch {
        try {
            block(entryPoint.sitRepository())
        } finally {
            pending.finish()
        }
    }
}

/** The reminder alarm fired: notify if a break is due, then arm the next alarm. */
class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = runAsync(context) { it.checkReminder() }
}

/**
 * Re-arms the alarm after reboot (alarms don't survive it), app update, and clock or timezone
 * changes (work hours are local time). These broadcasts are exempt from Android 8's
 * implicit-broadcast limits, so a manifest receiver still gets them.
 */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        runAsync(context) { it.checkReminder() }
    }

    private companion object {
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
    }
}

/** Done / Snooze / Skip buttons on the reminder notification. */
class BreakActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(EXTRA_BREAK_TYPE)
            ?.let { name -> BreakType.entries.firstOrNull { it.name == name } }
            ?: BreakType.STAND_UP
        runAsync(context) { repo ->
            when (intent.action) {
                ACTION_DONE -> repo.done(type)
                ACTION_SNOOZE -> repo.snooze()
                ACTION_SKIP -> repo.skip(type)
            }
        }
    }

    companion object {
        const val ACTION_DONE = "com.kartikkhandelwal.wigglebun.action.DONE"
        const val ACTION_SNOOZE = "com.kartikkhandelwal.wigglebun.action.SNOOZE"
        const val ACTION_SKIP = "com.kartikkhandelwal.wigglebun.action.SKIP"
        const val EXTRA_BREAK_TYPE = "break_type"

        fun intent(context: Context, action: String, type: BreakType): PendingIntent = PendingIntent.getBroadcast(
            context,
            action.hashCode(), // distinct request codes so the three actions don't overwrite each other
            Intent(context, BreakActionReceiver::class.java).setAction(action).putExtra(EXTRA_BREAK_TYPE, type.name),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
