package com.kartikkhandelwal.wigglebun.ui.pet

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.kartikkhandelwal.wigglebun.domain.model.PetId

/** An ellipse in viewBox units, optionally rotated about its own centre (SVG `rotate(deg cx cy)`). */
@Immutable
data class OvalSpec(
    val cx: Float,
    val cy: Float,
    val rx: Float,
    val ry: Float = rx,
    val rotationDeg: Float = 0f,
)

/** Which side of the head the sleeping z's float on. */
enum class SleepZSide { RIGHT, LEFT }

/**
 * A beak that replaces the mouth layer (Pip): [closedPath] in every mood, [openPath] (upper and
 * lower beak) where the mood has an open mouth (CELEBRATE). Absolute viewBox path data, filled.
 */
@Immutable
data class BeakSpec(val closedPath: String, val openPath: String, val color: Color)

/**
 * A band along the inside of the silhouette (Toast's crust): the silhouette stroked [width] wide
 * and clipped to itself. It drains with the fill: [color] below fillTop, [drainedColor] above.
 */
@Immutable
data class CrustSpec(val width: Float, val color: Color, val drainedColor: Color)

/**
 * Everything needed to draw one pet with [PetView]. All coordinates are in the pet's SVG
 * viewBox (0 0 200 210 for every pet so far). Face shapes ([EyeShape], [MouthShape]) are
 * relative to the eye/mouth anchors and break-session arms come from [PoseLayers], so every pet
 * reuses them.
 */
@Immutable
data class PetSpec(
    val id: PetId,
    @StringRes val defaultName: Int,
    /** One short line about the pet (Collection sheet). */
    @StringRes val blurb: Int,
    val viewBoxWidth: Float,
    val viewBoxHeight: Float,

    /** `silhouette` layer: this shape is what drains/fills. */
    val bodyPath: String,
    /** Shapes unioned with [bodyPath]: Bun's ears, Pip's feather tuft, Nimbus's three puffs. */
    val silhouetteOvals: List<OvalSpec>,
    val feet: List<OvalSpec>,
    /** Left arm first, then right (pose layers rely on the order). */
    val armsSide: List<OvalSpec>,
    val armsUp: List<OvalSpec>,
    /**
     * Moves a break-session arm layer where the pet's body would hide it (e.g. Toast's dome).
     * The offset is for the left arm; the right arm gets it mirrored.
     */
    val poseArmOffsets: Map<ArmPose, Offset> = emptyMap(),
    val earsInner: List<OvalSpec>,
    val cheeks: List<OvalSpec>,
    val beak: BeakSpec? = null,
    val crust: CrustSpec? = null,

    val bodyColor: Color,
    val bodyEdgeColor: Color,
    val drainedColor: Color,
    val earInnerColor: Color,
    val cheekColor: Color,
    val featureColor: Color,
    val sweatColor: Color,
    val sleepZColor: Color,

    /** fillTop = fillTopFull + (1 - energy) * fillRange (CLAUDE.md §7). */
    val fillTopFull: Float,
    val fillRange: Float,
    val fillEdgeThickness: Float,
    /** Feet / side arms sit outside the clip; they are body-coloured only while fillTop is above these. */
    val feetColoredAbove: Float,
    val armsColoredAbove: Float,

    val eyeLeft: Offset,
    val eyeRight: Offset,
    val eyeDotRadius: Float,
    val mouthAnchor: Offset,

    /** Lean and breathing pivot (on the floor between the feet). */
    val pivot: Offset,
    /** Break-session pose rotations turn the upper body about this point on the lower torso. */
    val torsoPivot: Offset,
    /** Lowest point of the feet; squash and stretch scale about this. */
    val groundY: Float,
    val sweatAnchor: Offset,
    /** Baseline-left of the big sleeping z; the small one floats up and to the right. */
    val sleepZAnchor: Offset,
    /** Where the small z sits relative to the big one (right side; mirrored for the left). */
    val sleepZStep: Offset,
    /**
     * [SleepZSide.LEFT]: baseline-right of the big z; the small one floats up and to the left.
     * Lower and further out than a straight mirror so the z's sit on plain wall in the desk scene.
     */
    val sleepZAnchorLeft: Offset,
) {
    fun fillTop(energy: Float): Float = fillTopFull + (1f - energy.coerceIn(0f, 1f)) * fillRange
}
