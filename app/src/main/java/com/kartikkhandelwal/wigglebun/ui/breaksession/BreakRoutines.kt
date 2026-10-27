package com.kartikkhandelwal.wigglebun.ui.breaksession

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.ui.pet.PoseSpec
import com.kartikkhandelwal.wigglebun.ui.pet.Poses

/** Each pose of a step is held this long before springing to the next (CLAUDE.md §7: ~1.2 s). */
const val POSE_HOLD_MILLIS = 1_200L

/**
 * One instruction of a break session. [poses] loop, each held [POSE_HOLD_MILLIS]; empty means
 * no pose (the mood's own idle). The type's duration is split evenly across its steps.
 */
@Immutable
data class SessionStep(@StringRes val text: Int, val poses: List<PoseSpec>)

/** The guided steps for this break type (built once; the session reads them every tick). */
fun BreakType.sessionSteps(): List<SessionStep> = routines.getValue(this)

/** Stretch (full energy, pose frames take over, CLAUDE.md §5.2); drinking water is a happy idle. */
val BreakType.sessionMood: Mood get() = if (this == BreakType.WATER) Mood.HAPPY else Mood.STRETCH

/** "Move gently. Stop if anything hurts." is for breaks that move the body, not eyes rest or water. */
val BreakType.hasSafetyNote: Boolean get() = when (this) {
    BreakType.STAND_UP, BreakType.WALK, BreakType.NECK, BreakType.SHOULDERS, BreakType.WRIST -> true
    BreakType.EYES, BreakType.WATER -> false
}

private val march = listOf(Poses.MarchA, Poses.MarchB, Poses.MarchC, Poses.MarchB)
private val shoulderRoll = listOf(Poses.ShouldersNeutral, Poses.ShouldersUp, Poses.ShouldersBack, Poses.ShouldersDown)

private val routines: Map<BreakType, List<SessionStep>> = BreakType.entries.associateWith { type ->
    when (type) {
        BreakType.STAND_UP -> listOf(
            SessionStep(R.string.step_stand_up_1, listOf(Poses.Sit)),
            SessionStep(R.string.step_stand_up_2, listOf(Poses.PushUp)),
            SessionStep(R.string.step_stand_up_3, listOf(Poses.Stand)),
            SessionStep(R.string.step_stand_up_4, listOf(Poses.ReachUp)),
        )
        BreakType.WALK -> listOf(
            SessionStep(R.string.step_walk_1, march),
            SessionStep(R.string.step_walk_2, march),
        )
        BreakType.NECK -> listOf(
            SessionStep(R.string.step_neck_1, listOf(Poses.NeckCentre)),
            SessionStep(R.string.step_neck_2, listOf(Poses.NeckTiltRight)),
            SessionStep(R.string.step_neck_3, listOf(Poses.NeckCentre)),
            SessionStep(R.string.step_neck_4, listOf(Poses.NeckTiltLeft)),
        )
        BreakType.SHOULDERS -> listOf(
            SessionStep(R.string.step_shoulders_1, shoulderRoll),
            SessionStep(R.string.step_shoulders_2, shoulderRoll.reversed()),
        )
        BreakType.WRIST -> listOf(
            SessionStep(R.string.step_wrist_1, listOf(Poses.WristRight)),
            SessionStep(R.string.step_wrist_2, listOf(Poses.WristLeft)),
        )
        // 20-20-20: relaxed / closed eyes, no body pose.
        BreakType.EYES -> listOf(
            SessionStep(R.string.step_eyes_1, listOf(Poses.EyesOpen)),
            SessionStep(R.string.step_eyes_2, listOf(Poses.EyesOpen, Poses.EyesClosed)),
            SessionStep(R.string.step_eyes_3, listOf(Poses.EyesClosed)),
        )
        // No cup art: the pet idles happily next to the instruction.
        BreakType.WATER -> listOf(
            SessionStep(R.string.step_water_1, emptyList()),
            SessionStep(R.string.step_water_2, emptyList()),
        )
    }
}
