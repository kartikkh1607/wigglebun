package com.kartikkhandelwal.wigglebun.data.prefs

import com.kartikkhandelwal.wigglebun.domain.model.MovementTransition

/** A transition as delivered by Play services, with its elapsed-realtime (time since boot) stamp. */
data class ReceivedTransition(
    val transition: MovementTransition,
    val elapsedRealtimeNanos: Long,
)

/** The newest transition event already processed, and the boot it belongs to. */
data class TransitionWatermark(
    val elapsedRealtimeNanos: Long,
    val bootCount: Int,
)

/**
 * Play services replays the current state (with its original timestamp) every time we register,
 * and may redeliver a batch. Only events strictly newer than the watermark are processed.
 * elapsedRealtime restarts at 0 on reboot, so a watermark from another boot is discarded.
 *
 * Returns the accepted events (in delivery order) and the watermark to store.
 */
fun acceptNewerThanWatermark(
    events: List<ReceivedTransition>,
    watermark: TransitionWatermark?,
    bootCount: Int,
): Pair<List<ReceivedTransition>, TransitionWatermark?> {
    val floor = watermark?.takeIf { it.bootCount == bootCount }?.elapsedRealtimeNanos
    val accepted = events.filter { floor == null || it.elapsedRealtimeNanos > floor }
    val newest = accepted.maxOfOrNull { it.elapsedRealtimeNanos }
        ?: return accepted to watermark?.takeIf { it.bootCount == bootCount }
    return accepted to TransitionWatermark(maxOf(newest, floor ?: Long.MIN_VALUE), bootCount)
}
