package com.kartikkhandelwal.wigglebun.notify

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.kartikkhandelwal.wigglebun.data.ApplicationScope
import com.kartikkhandelwal.wigglebun.data.repo.RecordBreakDone
import com.kartikkhandelwal.wigglebun.data.repo.SitRepository
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "Receivers"

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface ReceiverEntryPoint {
    fun sitRepository(): SitRepository

    fun recordBreakDone(): RecordBreakDone

    @ApplicationScope
    fun scope(): CoroutineScope
}

/**
 * Runs [block] off the main thread. goAsync() keeps the process alive until finish()
 * (the system allows ~10 s), which is plenty for a DataStore write, a few Room rows and an alarm.
 */
internal fun BroadcastReceiver.runAsync(context: Context, block: suspend (SitRepository) -> Unit) =
    runWithApp(context) { block(it.sitRepository()) }

/** [runAsync] with access to everything in [ReceiverEntryPoint]. */
internal fun BroadcastReceiver.runWithApp(context: Context, block: suspend (ReceiverEntryPoint) -> Unit) {
    val pending = goAsync()
    val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, ReceiverEntryPoint::class.java)
    entryPoint.scope().launch {
        try {
            block(entryPoint)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // An uncaught error here would crash the whole app from the background (e.g. disk
            // full while saving). Log it; the next alarm or app start runs the check again.
            Log.e(TAG, "Background work failed", e)
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
        val type = BreakType.fromName(intent.getStringExtra(EXTRA_BREAK_TYPE)) ?: BreakType.STAND_UP
        runWithApp(context) { app ->
            when (intent.action) {
                // Completes straight away (no session) and pays like an in-app Done, at most once.
                ACTION_DONE -> app.recordBreakDone()(type)
                ACTION_SNOOZE -> app.sitRepository().snooze()
                ACTION_SKIP -> app.sitRepository().skip(type)
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
