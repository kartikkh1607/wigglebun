package com.kartikkhandelwal.wigglebun.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.kartikkhandelwal.wigglebun.domain.model.MovementActivity
import com.kartikkhandelwal.wigglebun.domain.model.MovementTransition
import com.kartikkhandelwal.wigglebun.notify.runAsync
import java.time.Instant

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
            MovementTransition(
                activity = activity,
                entered = event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER,
                at = Instant.ofEpochMilli(nowWallMillis - ageMillis),
            )
        }
        if (events.isEmpty()) return
        runAsync(context) { it.onTransitions(events, realTime = true) }
    }
}
