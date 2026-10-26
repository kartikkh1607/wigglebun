package com.kartikkhandelwal.wigglebun.domain.model

import java.time.Instant

/**
 * Activities we subscribe to with the Activity Recognition Transition API. ON_FOOT is not
 * one of the Transition API's supported activities, so WALKING covers on-foot movement.
 */
enum class MovementActivity {
    WALKING,
    RUNNING,
    STILL,
    ;

    val isMoving: Boolean get() = this != STILL
}

/** One enter/exit transition, at the time it actually happened (events can arrive late, batched). */
data class MovementTransition(
    val activity: MovementActivity,
    val entered: Boolean,
    val at: Instant,
)

/** Why the sit timer last restarted (shown in the Pet lab; later useful for stats). */
enum class ResetReason { DONE, SKIP, WALKING, STEPS, MANUAL }
