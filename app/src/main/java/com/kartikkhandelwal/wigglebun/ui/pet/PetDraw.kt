package com.kartikkhandelwal.wigglebun.ui.pet

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** viewBox units over which feet/arms blend grey ↔ body colour as fillTop passes them. */
private const val COLOR_BLEND_BAND = 6f

/** Sleeping z's drift up and fade on this loop; sweat drips on its own. */
private const val Z_LOOP_SEC = 2.4f
/** The small z sits this far from the big one, both across and up (bun_sleep.svg). */
private const val Z_STEP = 20f
private const val SWEAT_LOOP_SEC = 1.8f
private const val CONFETTI_POP_SEC = 0.45f

/**
 * Draws one frame of the pet, layer by layer in SVG order (feet, arms, silhouette fill,
 * ears_inner, eyes, cheeks, mouth, extras). Everything is drawn in viewBox units after one
 * fit-to-size transform. No allocations: only inline transforms, value-class geometry and
 * the pre-built paths in [geometry].
 *
 * @param still draw the exact pose of design/pet_moods/bun_<mood>.svg (no idle motion).
 * @param pose break-session pose: its arm / eye / feet layers replace the mood's. Its transform
 *   comes in as [poseRotation], [poseScaleX], [poseScaleY], [poseLift] (spring-animated by the
 *   caller); with no pose they are 0, 1, 1, 0 and the drawing is exactly the mood's.
 */
internal fun DrawScope.drawPet(
    geometry: PetGeometry,
    spec: PetSpec,
    mood: MoodSpec,
    energy: Float,
    leanDeg: Float,
    motion: PetMotion,
    still: Boolean,
    sleepZSide: SleepZSide,
    pose: PoseSpec? = null,
    poseRotation: Float = 0f,
    poseScaleX: Float = 1f,
    poseScaleY: Float = 1f,
    poseLift: Float = 0f,
) {
    val scale = min(size.width / spec.viewBoxWidth, size.height / spec.viewBoxHeight)
    val left = (size.width - spec.viewBoxWidth * scale) / 2f
    val top = (size.height - spec.viewBoxHeight * scale) / 2f
    val fillTop = spec.fillTop(energy)

    withTransform({
        translate(left, top)
        scale(scale, scale, Offset.Zero)
    }) {
        if (mood.confetti) drawConfetti(motion, still)

        withTransform({
            translate(0f, motion.jumpY)
            scale(motion.jumpScaleX, motion.jumpScaleY, Offset(spec.pivot.x, spec.groundY))
        }) {
            // Feet stay planted: they don't lean, breathe or take the pose transform (CLAUDE.md §7).
            val feet = pose?.feet ?: FeetPose.Standing
            drawPath(geometry.feet[feet.ordinal], partColor(spec, spec.feetColoredAbove, fillTop))

            val breath = motion.breath
            withTransform({
                // Mood lean tips the whole body over the feet; a pose rotation (neck stretch)
                // turns only the upper body about the lower torso, as in neck_stretch_frames.svg.
                rotate(leanDeg, spec.pivot)
                // Skipped at 0: an identity rotate about another pivot still shifts anti-aliasing.
                if (poseRotation != 0f) rotate(poseRotation, spec.torsoPivot)
                translate(0f, poseLift - motion.bobAmplitude * breath)
                scale(poseScaleX, poseScaleY * (1f + motion.breathAmplitude * breath), spec.pivot)
            }) {
                drawBody(geometry, spec, mood, pose, energy, fillTop, motion, still)
            }
        }

        if (mood.sleepZ) drawSleepZs(geometry, spec, sleepZSide, motion, still)
    }
}

private fun DrawScope.drawBody(
    geometry: PetGeometry,
    spec: PetSpec,
    mood: MoodSpec,
    pose: PoseSpec?,
    energy: Float,
    fillTop: Float,
    motion: PetMotion,
    still: Boolean,
) {
    val armPose = pose?.arms ?: mood.arms
    val arms = geometry.arms[armPose.ordinal]
    // Rolled-back arms sit behind the body in the darker edge colour (more_stretch_frames.svg).
    val armColor = if (armPose == ArmPose.RollBack) spec.bodyEdgeColor else partColor(spec, spec.armsColoredAbove, fillTop)
    val armsInFront = pose?.armsInFront == true
    if (!armsInFront) drawPath(arms, armColor)

    // Energy fill: grey silhouette, body colour from fillTop down, darker edge line at fillTop.
    // The fill rects are drawn SrcAtop inside a layer holding only the silhouette, instead of
    // clipping: a rotated clip is not anti-aliased, which made the fill line jagged when leaning.
    drawIntoCanvas { canvas -> canvas.saveLayer(geometry.viewBox, geometry.layerPaint) }
    drawPath(geometry.silhouette, spec.drainedColor)
    drawRect(
        color = spec.bodyColor,
        topLeft = Offset(0f, fillTop),
        size = Size(spec.viewBoxWidth, spec.viewBoxHeight - fillTop),
        blendMode = BlendMode.SrcAtop,
    )
    if (energy < 1f) {
        drawRect(
            color = spec.bodyEdgeColor,
            topLeft = Offset(0f, fillTop),
            size = Size(spec.viewBoxWidth, spec.fillEdgeThickness),
            blendMode = BlendMode.SrcAtop,
        )
    }
    drawIntoCanvas { canvas -> canvas.restore() }
    // Hands on the lap (sit pose) are drawn over the body.
    if (armsInFront) drawPath(arms, armColor)

    drawPath(geometry.earsInner, spec.earInnerColor)

    val eyeLine = geometry.eyeLines[(pose?.eyes ?: mood.eyes).ordinal]
    if (eyeLine == null) {
        scale(1f, 1f - 0.9f * motion.blink, Offset(spec.pivot.x, spec.eyeLeft.y)) {
            drawPath(geometry.eyeDots, spec.featureColor)
        }
    } else {
        drawPath(eyeLine, spec.featureColor, style = geometry.eyeStroke)
    }

    drawPath(geometry.cheeks, spec.cheekColor)

    val mouth = geometry.mouths[mood.mouth.ordinal]
    if (mood.mouth.filled) drawPath(mouth, spec.featureColor)
    drawPath(mouth, spec.featureColor, style = geometry.mouthStroke)

    if (mood.sweat) {
        var dy = 0f
        var alpha = 1f
        if (!still) {
            val t = (motion.loopSec % SWEAT_LOOP_SEC) / SWEAT_LOOP_SEC
            dy = 6f * t
            alpha = if (t < 0.15f) t / 0.15f else 1f - ((t - 0.15f) / 0.85f).let { it * it }
        }
        translate(0f, dy) { drawPath(geometry.sweatDrop, spec.sweatColor, alpha = alpha) }
    }
}

private fun DrawScope.drawSleepZs(geometry: PetGeometry, spec: PetSpec, side: SleepZSide, motion: PetMotion, still: Boolean) {
    val large = geometry.sleepZLarge
    val small = geometry.sleepZSmall
    when (side) {
        SleepZSide.RIGHT -> {
            // Positions from bun_sleep.svg: big z at the anchor, small z up and to the right.
            val x = spec.sleepZAnchor.x
            val y = spec.sleepZAnchor.y
            drawSleepZ(large, x, y, driftX = 1f, motion, still, loopOffsetSec = 0f)
            drawSleepZ(small, x + Z_STEP, y - Z_STEP, driftX = 1f, motion, still, loopOffsetSec = Z_LOOP_SEC / 2f)
        }
        SleepZSide.LEFT -> {
            // Mirror image: the anchor is the big z's right end, small z up and to the left.
            val x = spec.sleepZAnchorLeft.x
            val y = spec.sleepZAnchorLeft.y
            drawSleepZ(large, x - large.size.width, y, driftX = -1f, motion, still, loopOffsetSec = 0f)
            drawSleepZ(small, x - Z_STEP - small.size.width, y - Z_STEP, driftX = -1f, motion, still, loopOffsetSec = Z_LOOP_SEC / 2f)
        }
    }
}

/** @param driftX 1 drifts right while rising, -1 left (away from the head either way). */
private fun DrawScope.drawSleepZ(
    layout: TextLayoutResult,
    baselineX: Float,
    baselineY: Float,
    driftX: Float,
    motion: PetMotion,
    still: Boolean,
    loopOffsetSec: Float,
) {
    var dx = 0f
    var dy = 0f
    var alpha = 1f
    if (!still) {
        // Rest position (the SVG pose) is the middle of the loop.
        val t = ((motion.loopSec + loopOffsetSec) % Z_LOOP_SEC) / Z_LOOP_SEC
        dx = driftX * 6f * (t - 0.5f)
        dy = -16f * (t - 0.5f)
        alpha = sin(PI.toFloat() * t)
    }
    drawText(layout, topLeft = Offset(baselineX + dx, baselineY + dy - layout.firstBaseline), alpha = alpha)
}

private fun DrawScope.drawConfetti(motion: PetMotion, still: Boolean) {
    val c = motion.celebrateSec
    if (!still && (c < 0f || c >= Confetti.DURATION_SEC)) return
    val pieces = Confetti.pieces
    for (i in pieces.indices) {
        val piece = pieces[i]
        if (still) {
            if (piece.inStillPose) drawPiece(piece, 0f, 0f, 0f, 1f, 1f)
            continue
        }
        // Burst out from the chest with a little overshoot, then fall, spin and fade.
        val pop = easeOutBack((c / CONFETTI_POP_SEC).coerceIn(0f, 1f))
        val fall = max(0f, c - CONFETTI_POP_SEC)
        val dx = (piece.pivotX - Confetti.ORIGIN_X) * (pop - 1f)
        val dy = (piece.pivotY - Confetti.ORIGIN_Y) * (pop - 1f) + 22f * fall * fall
        val alpha = 1f - ((c - 2.2f) / 0.8f).coerceIn(0f, 1f)
        drawPiece(piece, dx, dy, piece.spin * fall, pop.coerceAtLeast(0f), alpha)
    }
}

private fun DrawScope.drawPiece(piece: ConfettiPiece, dx: Float, dy: Float, extraRotation: Float, scale: Float, alpha: Float) {
    val pivot = Offset(piece.pivotX + dx, piece.pivotY + dy)
    withTransform({
        rotate(piece.rotationDeg + extraRotation, pivot)
        scale(scale, scale, pivot)
    }) {
        drawRoundRect(
            color = piece.color,
            topLeft = Offset(piece.x + dx, piece.y + dy),
            size = Size(piece.width, piece.height),
            cornerRadius = CornerRadius(piece.cornerRadius),
            alpha = alpha,
        )
    }
}

/** Feet and arms are outside the clip: body colour only while fillTop is above them. */
internal fun partColor(spec: PetSpec, coloredAbove: Float, fillTop: Float) = lerp(
    spec.drainedColor,
    spec.bodyColor,
    ((coloredAbove - fillTop) / COLOR_BLEND_BAND + 0.5f).coerceIn(0f, 1f),
)

private fun easeOutBack(t: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    val x = t - 1f
    return 1f + c3 * x * x * x + c1 * x * x
}
