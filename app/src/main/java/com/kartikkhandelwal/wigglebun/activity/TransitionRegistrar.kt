package com.kartikkhandelwal.wigglebun.activity

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.tasks.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private const val TAG = "Movement"

/** Subscribes/unsubscribes to Activity Recognition transitions (delivered to [MovementReceiver]). */
// Open so tests can stand in for Play services (registration success or failure).
@Singleton
open class TransitionRegistrar @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissions: MovementPermissions,
) {
    private val client by lazy { ActivityRecognition.getClient(context) }

    /**
     * Supported Transition API activities are IN_VEHICLE, ON_BICYCLE, RUNNING, STILL and WALKING.
     * ON_FOOT is *not* supported, so walking/running enter+exit plus STILL enter+exit it is.
     */
    private val request = ActivityTransitionRequest(
        listOf(DetectedActivity.WALKING, DetectedActivity.RUNNING, DetectedActivity.STILL).flatMap { activity ->
            listOf(ActivityTransition.ACTIVITY_TRANSITION_ENTER, ActivityTransition.ACTIVITY_TRANSITION_EXIT).map { type ->
                ActivityTransition.Builder().setActivityType(activity).setActivityTransition(type).build()
            }
        },
    )

    /**
     * Registers (or re-registers: the same PendingIntent replaces the old request). Registrations
     * are lost on reboot, app update and force-stop, so callers simply call this again then.
     * Returns false on any failure (no permission, Play services missing/disabled).
     */
    @SuppressLint("MissingPermission") // checked via permissions.granted()
    open suspend fun register(): Boolean {
        if (!permissions.granted()) return false
        return runCatching { client.requestActivityTransitionUpdates(request, pendingIntent()).awaitSuccess() }
            .onFailure { Log.w(TAG, "Transition registration failed", it) }
            .getOrDefault(false)
    }

    @SuppressLint("MissingPermission")
    open suspend fun unregister() {
        runCatching { client.removeActivityTransitionUpdates(pendingIntent()).awaitSuccess() }
            .onFailure { Log.w(TAG, "Transition unregister failed", it) }
    }

    /**
     * Must be FLAG_MUTABLE (API 31+ requires an explicit mutability flag): Play services fills
     * the ActivityTransitionResult extras into our intent, like requestLocationUpdates does.
     * Android 14+ throws for a mutable PendingIntent without a component, so it is explicit.
     */
    internal fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, MovementReceiver::class.java),
        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private companion object {
        const val REQUEST_CODE = 200
    }
}

/** Minimal Task → suspend bridge (avoids a kotlinx-coroutines-play-services dependency). */
private suspend fun Task<Void>.awaitSuccess(): Boolean = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { if (cont.isActive) cont.resume(true) }
    addOnFailureListener { e ->
        Log.w(TAG, "Play services task failed", e)
        if (cont.isActive) cont.resume(false)
    }
}
