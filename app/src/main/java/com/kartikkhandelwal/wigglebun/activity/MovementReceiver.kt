package com.kartikkhandelwal.wigglebun.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.kartikkhandelwal.wigglebun.BuildConfig
import com.kartikkhandelwal.wigglebun.data.prefs.ReceivedTransition
import com.kartikkhandelwal.wigglebun.domain.model.MovementActivity
import com.kartikkhandelwal.wigglebun.domain.model.MovementTransition
import com.kartikkhandelwal.wigglebun.notify.runAsync
import java.time.Instant

private const val TAG = "Movement"

/** Receives Activity Recognition transitions (registered by [TransitionRegistrar]). */
class MovementReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = ActivityTransitionResult.extractResult(intent) ?: return
        // Event times are elapsed-realtime (time since boot); convert to wall-clock time.
        val nowWallMillis = System.currentTimeMillis()
        val nowElapsedNanos = SystemClock.elapsedRealtimeNanos()
        val events = result.transitionEvents.mapNotNull { event ->
            val activity = when (event.activityType) {
                DetectedActivity.WALKING -> MovementActivity.WALKING
                DetectedActivity.RUNNING -> MovementActivity.RUNNING
                DetectedActivity.STILL -> MovementActivity.STILL
                else -> return@mapNotNull null
            }
            val ageMillis = (nowElapsedNanos - event.elapsedRealTimeNanos) / 1_000_000
            ReceivedTransition(
                transition = MovementTransition(
                    activity = activity,
                    entered = event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER,
                    at = Instant.ofEpochMilli(nowWallMillis - ageMillis),
                ),
                elapsedRealtimeNanos = event.elapsedRealTimeNanos,
            )
        }
        if (events.isEmpty()) return
        // Debug builds only, like MovementTracker.sync(): shows each delivery next to re-registrations.
        if (BuildConfig.DEBUG) Log.i(TAG, "received $events")
        val bootCount = context.bootCount()
        val arrivedAt = Instant.ofEpochMilli(nowWallMillis)
        runAsync(context) { it.onReceivedTransitions(events, bootCount, arrivedAt) }
    }
}
