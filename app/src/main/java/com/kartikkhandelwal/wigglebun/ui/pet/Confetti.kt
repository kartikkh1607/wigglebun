package com.kartikkhandelwal.wigglebun.ui.pet

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.kartikkhandelwal.wigglebun.ui.theme.BreakEyes
import com.kartikkhandelwal.wigglebun.ui.theme.BreakNeck
import com.kartikkhandelwal.wigglebun.ui.theme.BreakStand
import com.kartikkhandelwal.wigglebun.ui.theme.BreakWalk
import com.kartikkhandelwal.wigglebun.ui.theme.BreakWater
import com.kartikkhandelwal.wigglebun.ui.theme.BreakWrist

/**
 * One confetti piece in viewBox units: a rounded rect at ([x], [y]) rotated by [rotationDeg]
 * about ([pivotX], [pivotY]), same as SVG `<rect ... transform="rotate(deg px py)">`.
 * Circles are rects with corner radius = half the size.
 */
@Immutable
data class ConfettiPiece(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val cornerRadius: Float,
    val pivotX: Float,
    val pivotY: Float,
    val rotationDeg: Float,
    val color: Color,
    /** Degrees per second while falling. */
    val spin: Float,
    /** Part of the still pose in design/pet_moods/bun_celebrate.svg (the rest only show while animating). */
    val inStillPose: Boolean,
)

private fun rect(
    x: Float, y: Float, w: Float, h: Float, px: Float, py: Float, rot: Float, color: Color, spin: Float,
    still: Boolean = true,
) = ConfettiPiece(x, y, w, h, 2f, px, py, rot, color, spin, still)

private fun dot(cx: Float, cy: Float, r: Float, color: Color, still: Boolean = true) =
    ConfettiPiece(cx - r, cy - r, 2 * r, 2 * r, r, cx, cy, 0f, color, 0f, still)

internal object Confetti {
    /** Seconds of confetti after entering CELEBRATE (CLAUDE.md §7). */
    const val DURATION_SEC = 3f

    /** Pieces burst from here (the pet's chest) out to their rest position. */
    const val ORIGIN_X = 100f
    const val ORIGIN_Y = 110f

    val pieces: List<ConfettiPiece> = listOf(
        // The six pieces from bun_celebrate.svg, at their exact rest positions.
        rect(22f, 38f, 10f, 6f, 27f, 41f, -25f, BreakWrist, spin = -220f),
        rect(168f, 46f, 10f, 6f, 173f, 49f, 30f, BreakNeck, spin = 260f),
        dot(18f, 98f, 5f, BreakWalk),
        dot(184f, 104f, 5f, BreakEyes),
        rect(44f, 14f, 9f, 5f, 48f, 16f, 20f, BreakWater, spin = 180f),
        rect(148f, 10f, 9f, 5f, 152f, 12f, -15f, BreakWalk, spin = -200f),
        // Extra pieces for the animated burst only.
        dot(8f, 150f, 4f, BreakStand, still = false),
        rect(184f, 148f, 9f, 5f, 188f, 150f, 40f, BreakStand, spin = 240f, still = false),
        rect(92f, 4f, 9f, 5f, 96f, 6f, 10f, BreakNeck, spin = -160f, still = false),
        dot(72f, 22f, 3.5f, BreakWrist, still = false),
        dot(128f, 26f, 3.5f, BreakEyes, still = false),
        rect(186f, 28f, 8f, 5f, 190f, 30f, -30f, BreakWater, spin = 200f, still = false),
    )
}
