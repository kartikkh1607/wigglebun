package com.kartikkhandelwal.wigglebun.ui.home

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kartikkhandelwal.wigglebun.ui.theme.RoomPalette

/** The mockup's artboard: 390 wide, the room 340 high (design/ui_v2/Main.dc.html). */
private const val ART_WIDTH = 390f
private const val ART_HEIGHT = 340f
private const val ART_CENTRE = ART_WIDTH / 2f

/** How far the shelf plant may slide left before it would touch the books (they end near x 70). */
private const val SHELF_PLANT_MAX_SHIFT = 20f

/** Where the floor starts and where the wall's bands are, in artboard units. */
private const val LOWER_WALL_TOP = 196f
private const val RAIL_TOP = 192f
private const val SKIRTING_TOP = 272f
private const val SKIRTING_SHADOW_TOP = 285f
private const val FLOOR_TOP = 289f

/**
 * Home's room (design/ui_v2/Main.dc.html), drawn from [palette] in artboard units (1 unit = 1 dp).
 * The 390-wide artboard is split into groups anchored to the left edge (shelf, desk), the centre
 * (clock, rug) or the right edge (window, plant), so the room fits any width from 360 to 430 dp.
 * Below 390 dp the shelf plant slides left with the centre (at most [SHELF_PLANT_MAX_SHIFT]), so
 * it keeps its 390 dp gap from the pet's speech bubble instead of ending up under it.
 * The clock hangs [topInset] below the top; everything else stands on the floor at the bottom, so
 * extra height (the status bar, a taller bubble at large font sizes) is plain wall.
 */
@Composable
fun RoomScene(
    modifier: Modifier = Modifier,
    palette: RoomPalette = RoomPalette.Day,
    topInset: Dp = 0.dp,
    /** Onboarding's naming screen (Name.dc.html) is the same room without the clock and the shelf. */
    showClockAndShelf: Boolean = true,
) {
    val paths = remember { RoomPaths() }
    Spacer(
        modifier.drawWithCache {
            val unit = 1.dp.toPx()
            val width = size.width / unit
            val height = size.height / unit
            val top = topInset.toPx() / unit
            // Artboard y of the bottom-anchored groups: the artboard's 340 is the scene's bottom.
            val floorShift = height - ART_HEIGHT
            val rightShift = width - ART_WIDTH
            val centreShift = width / 2f - ART_CENTRE
            val shelfPlantShift = centreShift.coerceIn(-SHELF_PLANT_MAX_SHIFT, 0f)
            onDrawBehind {
                scale(unit, unit, Offset.Zero) {
                    drawWall(palette, width, height, floorShift)
                    if (showClockAndShelf) translate(left = centreShift, top = top) { drawClock(palette, paths) }
                    translate(left = rightShift, top = floorShift) { drawWindow(palette, paths) }
                    translate(top = floorShift) {
                        if (showClockAndShelf) {
                            drawShelf(palette)
                            translate(left = shelfPlantShift) { drawShelfPlant(palette, paths) }
                        }
                        drawDesk(palette, paths)
                    }
                    translate(left = rightShift, top = floorShift) { drawPlant(palette, paths) }
                    translate(left = centreShift, top = floorShift) { drawRug(palette) }
                }
            }
        },
    )
}

/** Path data straight from the mockup, parsed once. */
private class RoomPaths {
    private fun parse(data: String) = PathParser().parsePathString(data).toPath()

    val clockHands = parse("M195 38 V29.5 M195 38 L201 41.5")
    val windowFrame = parse("M262 190 V112 A52 52 0 0 1 366 112 V190 Z")
    val windowSky = parse("M271 184 V113 A43 43 0 0 1 357 113 V184 Z")
    val shelfPot = parse("M92 92 H116 L113 108 H95 Z")
    val glare = parse("M44 190 L68 164 H80 L50 199 H44 Z")
    val mug = parse("M6 184 H22 V200 C22 202.2 20.2 204 18 204 H10 C7.8 204 6 202.2 6 200 Z")
    val mugHandle = parse("M22 188 C27 188 27 197 22 197")
    val steam = parse("M11 178 C9 174 13 172 11 168 M17 178 C15 174 19 172 17 168")
    val plantPot = parse("M334 240 H378 L372 286 C371.6 289 369.4 291 366.4 291 H345.6 C342.6 291 340.4 289 340 286 Z")
}

private fun DrawScope.rect(color: Color, x: Float, y: Float, w: Float, h: Float, r: Float = 0f) =
    if (r == 0f) {
        drawRect(color, Offset(x, y), Size(w, h))
    } else {
        drawRoundRect(color, Offset(x, y), Size(w, h), CornerRadius(r))
    }

private fun DrawScope.oval(color: Color, cx: Float, cy: Float, rx: Float, ry: Float, degrees: Float = 0f) =
    rotate(degrees, Offset(cx, cy)) { drawOval(color, Offset(cx - rx, cy - ry), Size(rx * 2, ry * 2)) }

/** Walls, wainscot, skirting and the strip of floor, full width; the wall runs up to the top. */
private fun DrawScope.drawWall(p: RoomPalette, width: Float, height: Float, floorShift: Float) {
    fun y(artY: Float) = artY + floorShift
    rect(p.wall, 0f, 0f, width, y(SKIRTING_TOP))
    rect(p.wallLower, 0f, y(LOWER_WALL_TOP), width, y(SKIRTING_TOP) - y(LOWER_WALL_TOP))
    rect(p.trim, 0f, y(RAIL_TOP), width, 5f)
    // Wainscot panel lines: two from the left, two from the right.
    val rightShift = width - ART_WIDTH
    listOf(20f, 120f, 268f + rightShift, 368f + rightShift).forEach { x -> rect(p.wallEdge, x, y(210f), 2f, 62f) }
    rect(p.trim, 0f, y(SKIRTING_TOP), width, 13f)
    rect(p.skirtingShadow, 0f, y(SKIRTING_SHADOW_TOP), width, 4f)
    rect(p.floor, 0f, y(FLOOR_TOP), width, height - y(FLOOR_TOP))
    // Floorboard seams in the scene only (design decision D8: plain floor under the list).
    rect(p.floorSeam, 96f, y(FLOOR_TOP), 2f, ART_HEIGHT - FLOOR_TOP)
    rect(p.floorSeam, 290f + rightShift, y(FLOOR_TOP), 2f, ART_HEIGHT - FLOOR_TOP)
}

private fun DrawScope.drawClock(p: RoomPalette, paths: RoomPaths) {
    drawCircle(p.clockRimShadow, 21f, Offset(ART_CENTRE, 40f))
    drawCircle(p.clockRim, 21f, Offset(ART_CENTRE, 38f))
    drawCircle(p.clockFace, 15f, Offset(ART_CENTRE, 38f))
    drawPath(paths.clockHands, p.clockHands, style = Stroke(width = 3f, cap = StrokeCap.Round))
    drawCircle(p.clockHands, 2.3f, Offset(ART_CENTRE, 38f))
}

private fun DrawScope.drawWindow(p: RoomPalette, paths: RoomPaths) {
    drawPath(paths.windowFrame, p.windowFrame)
    drawPath(paths.windowSky, p.sky)
    // The view outside stays inside the glass (the mockup's hills spill onto the wall).
    clipPath(paths.windowSky) {
        oval(p.hillNear, 300f, 186f, 52f, 28f)
        oval(p.hillFar, 346f, 190f, 44f, 24f)
        rect(p.treeTrunk, 322f, 146f, 4f, 22f, 2f)
        drawCircle(p.treeTop, 12f, Offset(324f, 142f))
        drawCircle(p.cloud, 7f, Offset(290f, 114f))
        drawCircle(p.cloud, 9f, Offset(300f, 110f))
        rect(p.cloud, 282f, 113f, 28f, 9f, 4.5f)
    }
    rect(p.windowFrame, 311f, 66f, 6f, 122f)
    rect(p.windowFrame, 271f, 132f, 86f, 5f)
    rect(p.sill, 254f, 186f, 120f, 10f, 5f)
}

private fun DrawScope.drawShelf(p: RoomPalette) {
    rect(p.shelf, 14f, 108f, 112f, 9f, 4f)
    rect(p.shelfShadow, 14f, 114f, 112f, 5f, 2.5f)
    rect(p.books[0], 22f, 80f, 10f, 28f, 2f)
    rect(p.books[1], 34f, 86f, 9f, 22f, 2f)
    rect(p.books[2], 45f, 78f, 11f, 30f, 2f)
    rotate(12f, Offset(62f, 98f)) { rect(p.books[3], 58f, 88f, 9f, 20f, 2f) }
}

/** The pot plant on the shelf, drawn on its own so it can slide on narrow screens. */
private fun DrawScope.drawShelfPlant(p: RoomPalette, paths: RoomPaths) {
    drawPath(paths.shelfPot, p.shelfPot)
    oval(p.leafMid, 98f, 84f, 5f, 10f, -30f)
    oval(p.leafDark, 110f, 82f, 5f, 11f, 28f)
    oval(p.leafLight, 104f, 78f, 5f, 12f)
}

private fun DrawScope.drawDesk(p: RoomPalette, paths: RoomPaths) {
    rect(p.deskLeg, 8f, 216f, 9f, 74f, 3f)
    rect(p.deskLeg, 104f, 216f, 9f, 74f, 3f)
    rect(p.deskEdge, -12f, 212f, 140f, 11f, 5f)
    rect(p.deskTop, -12f, 205f, 140f, 11f, 5f)
    rect(p.laptop, 40f, 160f, 60f, 43f, 5f)
    rect(p.laptopScreen, 44f, 164f, 52f, 35f, 2.5f)
    drawPath(paths.glare, p.glare)
    rect(p.laptopBase, 32f, 200f, 76f, 7f, 3f)
    drawPath(paths.mug, p.mug)
    drawPath(paths.mugHandle, p.mug, style = Stroke(width = 3f))
    drawPath(paths.steam, p.steam, style = Stroke(width = 2.2f, cap = StrokeCap.Round))
}

private fun DrawScope.drawPlant(p: RoomPalette, paths: RoomPaths) {
    oval(p.leafDark, 342f, 198f, 11f, 26f, -30f)
    oval(p.leafDark, 370f, 198f, 11f, 26f, 30f)
    oval(p.leafLight, 356f, 182f, 12f, 32f)
    oval(p.leafLight, 336f, 222f, 10f, 20f, -62f)
    oval(p.leafLight, 376f, 222f, 10f, 20f, 62f)
    drawPath(paths.plantPot, p.plantPot)
    rect(p.plantPotRim, 328f, 232f, 56f, 12f, 6f)
}

/** The rug the pet stands on, with the shadow under its feet. */
private fun DrawScope.drawRug(p: RoomPalette) {
    oval(p.rugEdge, ART_CENTRE, 311f, 128f, 25f)
    oval(p.rug, ART_CENTRE, 308f, 128f, 24f)
    drawOval(
        p.rugStitch,
        topLeft = Offset(ART_CENTRE - 106f, 308f - 16f),
        size = Size(212f, 32f),
        style = Stroke(width = 2.5f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(1f, 8f))),
    )
    oval(p.rugShadow, ART_CENTRE, 310f, 62f, 9f)
}

/** The room alone at the narrowest and widest phone widths it is laid out for. */
@Preview(widthDp = 360, heightDp = 340)
@Preview(widthDp = 430, heightDp = 340)
@Composable
internal fun RoomScenePreview() {
    RoomScene(Modifier.fillMaxSize())
}
