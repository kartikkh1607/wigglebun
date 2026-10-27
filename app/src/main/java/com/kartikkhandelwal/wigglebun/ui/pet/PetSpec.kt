package com.kartikkhandelwal.wigglebun.ui.pet

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

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
 * Everything needed to draw one pet with [PetView]. All coordinates are in the pet's SVG
 * viewBox (Bun: 0 0 200 210). Collection pets (Pip, Toast, Nimbus, ...) get their own spec;
 * face shapes ([FaceShapes]) are relative to the eye/mouth anchors so they are reused.
 */
@Immutable
data class PetSpec(
    val id: String,
    @StringRes val defaultName: Int,
    val viewBoxWidth: Float,
    val viewBoxHeight: Float,

    /** `silhouette` layer: body path + round ears. This shape is what drains/fills. */
    val bodyPath: String,
    val ears: List<OvalSpec>,
    val feet: List<OvalSpec>,
    val armsSide: List<OvalSpec>,
    val armsUp: List<OvalSpec>,
    val earsInner: List<OvalSpec>,
    val cheeks: List<OvalSpec>,

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
    /** Lowest point of the feet; squash and stretch scale about this. */
    val groundY: Float,
    val sweatAnchor: Offset,
    /** Baseline-left of the big sleeping z; the small one floats up and to the right. */
    val sleepZAnchor: Offset,
    /**
     * [SleepZSide.LEFT]: baseline-right of the big z; the small one floats up and to the left.
     * Lower and further out than a straight mirror so the z's sit on plain wall in the desk scene.
     */
    val sleepZAnchorLeft: Offset,
) {
    fun fillTop(energy: Float): Float = fillTopFull + (1f - energy.coerceIn(0f, 1f)) * fillRange
}
