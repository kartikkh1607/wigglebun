package com.kartikkhandelwal.wigglebun.ui.pet

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.TextLayoutResult

/** Sweat drop from bun_stiff.svg, relative to [PetSpec.sweatAnchor]. */
private const val SWEAT_DROP = "M0 0 Q-7 13 0 18 Q7 13 0 0 Z"

/**
 * Every [Path] and [Stroke] a pet needs, built once per [PetSpec] (and remembered), so the
 * per-frame draw code never allocates.
 */
internal class PetGeometry(
    spec: PetSpec,
    /** The two sleeping z's ("z" at 24 and 16 viewBox units). */
    val sleepZLarge: TextLayoutResult,
    val sleepZSmall: TextLayoutResult,
) {
    /** Body + ears (or tuft, puffs), unioned so overlapping shapes never leave winding holes. */
    val silhouette: Path = Path.combine(PathOperation.Union, parse(spec.bodyPath), ovals(spec.silhouetteOvals))
    private val feetStanding: Path = ovals(spec.feet)
    private val armsSide: Path = ovals(spec.armsSide)

    /** Indexed by [ArmPose.ordinal]. */
    val arms: List<Path> = ArmPose.entries.map { pose ->
        when (pose) {
            ArmPose.Side -> armsSide
            ArmPose.Up -> ovals(spec.armsUp)
            else -> PoseLayers.arms[pose]?.let { ovals(poseArmOvals(spec, it, spec.poseArmOffsets[pose])) } ?: armsSide
        }
    }

    /** Indexed by [FeetPose.ordinal]. */
    val feet: List<Path> = FeetPose.entries.map { pose ->
        if (pose == FeetPose.Standing) feetStanding else PoseLayers.feet[pose]?.let(::ovals) ?: feetStanding
    }
    val earsInner: Path = ovals(spec.earsInner)
    val cheeks: Path = ovals(spec.cheeks)

    val eyeDots: Path = ovals(
        listOf(
            OvalSpec(spec.eyeLeft.x, spec.eyeLeft.y, spec.eyeDotRadius),
            OvalSpec(spec.eyeRight.x, spec.eyeRight.y, spec.eyeDotRadius),
        ),
    )

    /** Indexed by [EyeShape.ordinal]; null for [EyeShape.Dots], which uses [eyeDots]. */
    val eyeLines: List<Path?> = EyeShape.entries.map { shape ->
        shape.pathData?.let { data ->
            val one = parse(data)
            val right = if (shape.mirrorRight) parse(data).apply { transform(Matrix().apply { scale(-1f, 1f) }) } else one
            Path().apply {
                addPath(one, spec.eyeLeft)
                addPath(right, spec.eyeRight)
            }
        }
    }

    /**
     * Indexed by [MouthShape.ordinal]. A beak (Pip) replaces every mouth: closed, or open where
     * the mood's mouth is open (CELEBRATE).
     */
    val mouths: List<Path> = MouthShape.entries.map { shape ->
        val beak = spec.beak
        when {
            beak == null -> parse(shape.pathData).apply { translate(spec.mouthAnchor) }
            shape == MouthShape.Open -> parse(beak.openPath)
            else -> parse(beak.closedPath)
        }
    }

    val sweatDrop: Path = parse(SWEAT_DROP).apply { translate(spec.sweatAnchor) }

    /** Bounds and paint for the silhouette fill layer (see drawPet). */
    val viewBox = Rect(0f, 0f, spec.viewBoxWidth, spec.viewBoxHeight)
    val layerPaint = Paint()

    /** Crust layer (Toast): only as big as the silhouette, and composited onto it (SrcAtop). */
    val silhouetteBounds: Rect = silhouette.getBounds()
    val crustLayerPaint = Paint().apply { blendMode = BlendMode.SrcAtop }

    /** Fill edge over a neck item: the silhouette composited DstIn keeps the line inside the body. */
    val silhouetteMaskPaint = Paint().apply { blendMode = BlendMode.DstIn }
    val crustStroke: Stroke? = spec.crust?.let { Stroke(width = it.width, join = StrokeJoin.Round) }

    // Stroke widths from the mood SVGs, in viewBox units (the canvas is scaled before drawing).
    val eyeStroke = Stroke(width = 3.6f, cap = StrokeCap.Round)
    val mouthStroke = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)

    internal companion object {
        fun parse(pathData: String): Path = PathParser().parsePathString(pathData).toPath()

        /**
         * A pose's ovals for [spec]: a side without its own layer keeps the pet's resting arm;
         * [offset] moves the pose's own ovals (mirrored for the right arm).
         */
        fun poseArmOvals(spec: PetSpec, arms: PoseArms, offset: Offset?): List<OvalSpec> {
            fun List<OvalSpec>.moved(dx: Float): List<OvalSpec> =
                if (offset == null) this else map { it.copy(cx = it.cx + dx, cy = it.cy + offset.y) }
            val left = arms.left?.moved(offset?.x ?: 0f) ?: listOf(spec.armsSide.first())
            val right = arms.right?.moved(-(offset?.x ?: 0f)) ?: listOf(spec.armsSide.last())
            return left + right
        }

        fun ovals(specs: List<OvalSpec>): Path = Path().apply {
            specs.forEach { oval ->
                val one = Path().apply {
                    addOval(Rect(oval.cx - oval.rx, oval.cy - oval.ry, oval.cx + oval.rx, oval.cy + oval.ry))
                }
                if (oval.rotationDeg != 0f) {
                    // SVG rotate(deg cx cy): rotate about the oval's own centre.
                    one.transform(
                        Matrix().apply {
                            translate(oval.cx, oval.cy)
                            rotateZ(oval.rotationDeg)
                            translate(-oval.cx, -oval.cy)
                        },
                    )
                }
                addPath(one, Offset.Zero)
            }
        }
    }
}
