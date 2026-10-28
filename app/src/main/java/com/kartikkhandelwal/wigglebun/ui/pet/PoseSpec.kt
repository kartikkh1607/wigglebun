package com.kartikkhandelwal.wigglebun.ui.pet

import androidx.compose.runtime.Immutable

/** Feet layers from the pose frames (sit: feet forward; march: one foot lifted). */
enum class FeetPose { Standing, Sit, LiftLeft, LiftRight }

/**
 * One break-session pose (CLAUDE.md §7): the pet's own layers with a transform and different
 * arm / eye / feet layers, never a new body shape. Rotation is about [PetSpec.torsoPivot] (the
 * lower torso), scale about [PetSpec.pivot] (between the feet); the feet stay planted.
 */
@Immutable
data class PoseSpec(
    val rotationDeg: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    /** Raises the body (negative = up) while the feet stay down: march in place. */
    val liftY: Float = 0f,
    val arms: ArmPose = ArmPose.Side,
    /** Arms drawn over the body (hands on the lap) instead of behind it. */
    val armsInFront: Boolean = false,
    /** Null keeps the mood's eyes. */
    val eyes: EyeShape? = null,
    val feet: FeetPose = FeetPose.Standing,
)

/**
 * Both arms of one break-session pose. A null side keeps the pet's own resting arm
 * ([PetSpec.armsSide]), so a one-arm pose (wrist stretch, march) leaves Pip's wing a wing.
 */
@Immutable
data class PoseArms(val left: List<OvalSpec>?, val right: List<OvalSpec>?)

/**
 * Arm and feet layers of the break-session poses, read off design/stand_up_frames.svg and
 * more_stretch_frames.svg (drawn for Bun). Every pet reuses them, nudged by
 * [PetSpec.poseArmOffsets] only where its body would hide an arm.
 */
object PoseLayers {
    val arms: Map<ArmPose, PoseArms> = mapOf(
        ArmPose.Lap to PoseArms(
            left = listOf(OvalSpec(52f, 150f, 10f, 16f, rotationDeg = 50f)),
            right = listOf(OvalSpec(148f, 150f, 10f, 16f, rotationDeg = -50f)),
        ),
        ArmPose.Shrug to PoseArms(
            left = listOf(OvalSpec(44f, 104f, 10f, 18f, rotationDeg = -8f)),
            right = listOf(OvalSpec(156f, 104f, 10f, 18f, rotationDeg = 8f)),
        ),
        ArmPose.RollBack to PoseArms(
            left = listOf(OvalSpec(44f, 126f, 8f, 14f, rotationDeg = 40f)),
            right = listOf(OvalSpec(156f, 126f, 8f, 14f, rotationDeg = -40f)),
        ),
        ArmPose.RollDown to PoseArms(
            left = listOf(OvalSpec(38f, 140f, 10f, 17f, rotationDeg = 18f)),
            right = listOf(OvalSpec(162f, 140f, 10f, 17f, rotationDeg = -18f)),
        ),
        ArmPose.ReachRight to PoseArms(
            left = null,
            right = listOf(OvalSpec(180f, 126f, 24f, 10f), OvalSpec(203f, 114f, 8f, 12f)),
        ),
        ArmPose.ReachLeft to PoseArms(
            left = listOf(OvalSpec(20f, 126f, 24f, 10f), OvalSpec(-3f, 114f, 8f, 12f)),
            right = null,
        ),
        ArmPose.MarchLeft to PoseArms(left = listOf(OvalSpec(38f, 124f, 10f, 17f, rotationDeg = 10f)), right = null),
        ArmPose.MarchRight to PoseArms(left = null, right = listOf(OvalSpec(162f, 124f, 10f, 17f, rotationDeg = -10f))),
    )

    val feet: Map<FeetPose, List<OvalSpec>> = mapOf(
        FeetPose.Sit to listOf(OvalSpec(74f, 194f, 15f, 9f), OvalSpec(126f, 194f, 15f, 9f)),
        FeetPose.LiftLeft to listOf(OvalSpec(80f, 180f, 12f, 9f), OvalSpec(120f, 192f, 13f, 10f)),
        FeetPose.LiftRight to listOf(OvalSpec(80f, 192f, 13f, 10f), OvalSpec(120f, 180f, 12f, 9f)),
    )
}

/** Key poses read off design/neck_stretch_frames.svg, stand_up_frames.svg and more_stretch_frames.svg. */
object Poses {
    // Neck stretch: relaxed closed eyes, upper body tilts ±20°.
    val NeckCentre = PoseSpec(eyes = EyeShape.Closed)
    val NeckTiltRight = PoseSpec(rotationDeg = 20f, eyes = EyeShape.Closed)
    val NeckTiltLeft = PoseSpec(rotationDeg = -20f, eyes = EyeShape.Closed)

    // Stand up: squash and stretch about the feet.
    val Sit = PoseSpec(scaleX = 1.10f, scaleY = 0.80f, arms = ArmPose.Lap, armsInFront = true, eyes = EyeShape.Dots, feet = FeetPose.Sit)
    val PushUp = PoseSpec(scaleX = 1.05f, scaleY = 0.90f, arms = ArmPose.Lap, eyes = EyeShape.Dots)
    val Stand = PoseSpec(eyes = EyeShape.Dots)
    val ReachUp = PoseSpec(scaleX = 0.96f, scaleY = 1.06f, arms = ArmPose.Up, eyes = EyeShape.Happy)

    // Shoulder rolls: neutral → up (squeeze) → back → down.
    val ShouldersNeutral = PoseSpec(eyes = EyeShape.Dots)
    val ShouldersUp = PoseSpec(scaleX = 1.03f, scaleY = 0.96f, arms = ArmPose.Shrug, eyes = EyeShape.Squeeze)
    val ShouldersBack = PoseSpec(scaleX = 1.05f, arms = ArmPose.RollBack, eyes = EyeShape.Dots)
    val ShouldersDown = PoseSpec(arms = ArmPose.RollDown, eyes = EyeShape.Happy)

    // Wrist stretch: one arm out with the hand up.
    val WristRight = PoseSpec(arms = ArmPose.ReachRight, eyes = EyeShape.Dots)
    val WristLeft = PoseSpec(arms = ArmPose.ReachLeft, eyes = EyeShape.Dots)

    // March in place (the 2-min walk), looped A-B-C-B.
    val MarchA = PoseSpec(liftY = -6f, arms = ArmPose.MarchLeft, eyes = EyeShape.Happy, feet = FeetPose.LiftLeft)
    val MarchB = PoseSpec(scaleX = 1.03f, scaleY = 0.97f, eyes = EyeShape.Dots)
    val MarchC = PoseSpec(liftY = -6f, arms = ArmPose.MarchRight, eyes = EyeShape.Happy, feet = FeetPose.LiftRight)

    // Eyes rest (20-20-20): no body pose, only the eyes.
    val EyesOpen = PoseSpec(eyes = EyeShape.Dots)
    val EyesClosed = PoseSpec(eyes = EyeShape.Closed)
}
