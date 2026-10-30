package com.kartikkhandelwal.wigglebun.ui.pet

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.kartikkhandelwal.wigglebun.domain.model.AccessoryId
import com.kartikkhandelwal.wigglebun.domain.model.AccessorySlot
import com.kartikkhandelwal.wigglebun.ui.theme.AccessoryBeanie
import com.kartikkhandelwal.wigglebun.ui.theme.AccessoryBeanieBrim
import com.kartikkhandelwal.wigglebun.ui.theme.AccessoryLeaf
import com.kartikkhandelwal.wigglebun.ui.theme.AccessoryPetal
import com.kartikkhandelwal.wigglebun.ui.theme.AccessoryScarf
import com.kartikkhandelwal.wigglebun.ui.theme.AccessoryScarfStripe
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.Sun

/** One filled (or, with [strokeWidth], stroked) shape of an accessory: SVG path data and/or ovals. */
data class AccessoryShape(
    val color: Color,
    val pathData: String? = null,
    val ovals: List<OvalSpec> = emptyList(),
    val strokeWidth: Float? = null,
)

/** Where a slot's accessory goes on a pet: its reference point lands on [position], scaled and rotated about it. */
data class AccessoryAnchor(val position: Offset, val scale: Float = 1f, val rotationDeg: Float = 0f)

/**
 * Accessory art, copied from design/accessories/acc_<key>.svg (group #accessory). Each is drawn
 * on Bun around its slot's [referencePoint]; PetSpec.accessoryAnchors moves it onto each pet.
 */
object AccessorySpecs {

    /** The point on Bun (viewBox 0 0 200 210) each slot's art is drawn around. */
    fun referencePoint(slot: AccessorySlot): Offset = when (slot) {
        AccessorySlot.HEAD -> Offset(100f, 46f) // top of the head, between the ears
        AccessorySlot.FACE -> Offset(100f, 106f) // between the eyes
        AccessorySlot.NECK -> Offset(100f, 128f) // below the cheeks
    }

    /**
     * Every pet shares Bun's eye and cheek positions, so FACE and NECK fit all of them as drawn;
     * only the top of the head differs (PetSpec.accessoryAnchors overrides HEAD).
     */
    val DefaultAnchors: Map<AccessorySlot, AccessoryAnchor> =
        AccessorySlot.entries.associateWith { AccessoryAnchor(referencePoint(it)) }

    fun of(id: AccessoryId): List<AccessoryShape> = when (id) {
        AccessoryId.FLOWER_CLIP -> FlowerClip
        AccessoryId.ROUND_GLASSES -> RoundGlasses
        AccessoryId.KNIT_SCARF -> KnitScarf
        AccessoryId.BEANIE -> Beanie
    }

    private val FlowerClip = listOf(
        AccessoryShape(AccessoryLeaf, ovals = listOf(OvalSpec(99f, 50f, 10f, 4.5f, rotationDeg = -25f))),
        AccessoryShape(
            AccessoryPetal,
            ovals = listOf(
                OvalSpec(114f, 33f, 7f),
                OvalSpec(122.56f, 39.22f, 7f),
                OvalSpec(119.29f, 49.28f, 7f),
                OvalSpec(108.71f, 49.28f, 7f),
                OvalSpec(105.44f, 39.22f, 7f),
            ),
        ),
        AccessoryShape(Sun, ovals = listOf(OvalSpec(114f, 42f, 5f))),
    )

    /** Stroked like the pets' mouths (3 wide, round). */
    private val RoundGlasses = listOf(
        AccessoryShape(
            Ink,
            pathData = "M95.5 104 Q100 100.5 104.5 104 M72.5 104 L64 100 M127.5 104 L136 100",
            ovals = listOf(OvalSpec(84f, 106f, 11.5f), OvalSpec(116f, 106f, 11.5f)),
            strokeWidth = 3f,
        ),
    )

    private val KnitScarf = listOf(
        AccessoryShape(
            AccessoryScarf,
            pathData = "M46 119 Q100 141 154 119 L156 137 Q100 161 44 137 Z " +
                "M118 140 L136 134 L143 170 L125 175 Z",
        ),
        AccessoryShape(
            AccessoryScarfStripe,
            pathData = "M121.5 151 L139.5 145.5 L140.3 149.5 L122.3 155 Z " +
                "M123.5 161.5 L141.5 156 L142.3 160 L124.3 165.5 Z",
        ),
    )

    private val Beanie = listOf(
        AccessoryShape(AccessoryBeanie, pathData = "M71 50 C71 31 84 22 100 22 C116 22 129 31 129 50 Z"),
        AccessoryShape(
            AccessoryBeanieBrim,
            pathData = "M74 44 H126 Q132 44 132 50 V52 Q132 58 126 58 H74 Q68 58 68 52 V50 Q68 44 74 44 Z",
        ),
        AccessoryShape(
            AccessoryBeanie,
            pathData = "M78 46 H80.5 V56 H78 Z M89 46 H91.5 V56 H89 Z M99 46 H101.5 V56 H99 Z " +
                "M109 46 H111.5 V56 H109 Z M120 46 H122.5 V56 H120 Z",
        ),
        AccessoryShape(Sun, ovals = listOf(OvalSpec(100f, 19f, 7f))),
    )
}

/** An accessory's shapes as ready-to-draw paths, built once (drawing happens in viewBox units). */
internal class AccessoryGeometry(shapes: List<AccessoryShape>) {
    class Part(val path: Path, val color: Color, val stroke: Stroke?)

    val parts: List<Part> = shapes.map { shape ->
        val path = Path().apply {
            shape.pathData?.let { addPath(PetGeometry.parse(it)) }
            if (shape.ovals.isNotEmpty()) addPath(PetGeometry.ovals(shape.ovals))
        }
        val stroke = shape.strokeWidth?.let { Stroke(width = it, cap = StrokeCap.Round, join = StrokeJoin.Round) }
        Part(path, shape.color, stroke)
    }

    companion object {
        private val cache = AccessoryId.entries.associateWith { lazy { AccessoryGeometry(AccessorySpecs.of(it)) } }

        fun of(id: AccessoryId): AccessoryGeometry = cache.getValue(id).value
    }
}
