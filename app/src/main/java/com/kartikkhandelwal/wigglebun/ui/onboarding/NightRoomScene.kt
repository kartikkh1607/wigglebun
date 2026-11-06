package com.kartikkhandelwal.wigglebun.ui.onboarding

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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kartikkhandelwal.wigglebun.ui.theme.NightRoomFloor
import com.kartikkhandelwal.wigglebun.ui.theme.NightRoomFloorLine
import com.kartikkhandelwal.wigglebun.ui.theme.NightRoomLedge
import com.kartikkhandelwal.wigglebun.ui.theme.NightRoomLowerWall
import com.kartikkhandelwal.wigglebun.ui.theme.NightRoomMoon
import com.kartikkhandelwal.wigglebun.ui.theme.NightRoomRug
import com.kartikkhandelwal.wigglebun.ui.theme.NightRoomRugEdge
import com.kartikkhandelwal.wigglebun.ui.theme.NightRoomRugStitch
import com.kartikkhandelwal.wigglebun.ui.theme.NightRoomStar
import com.kartikkhandelwal.wigglebun.ui.theme.NightRoomTrim
import com.kartikkhandelwal.wigglebun.ui.theme.NightRoomWall
import com.kartikkhandelwal.wigglebun.ui.theme.NightRoomWindowGlass
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme

/** The mockup's artboard (design/ui_v2/WakeUp.dc.html): 390 wide, the wall 420 high, the floor below. */
private const val ART_WIDTH = 390f
private const val ART_CENTRE = ART_WIDTH / 2f
private const val WALL_BOTTOM = 420f
private const val TRIM_TOP = 410f
private const val FLOOR_TOP = 423f

/** Below this squeeze the sparkle at the top right collides with Skip. */
private const val SPARKLE_MIN_SQUEEZE = 0.7f

/**
 * Onboarding's night room (design/ui_v2/WakeUp.dc.html), drawn from tokens in artboard units.
 * [squeeze] scales the whole scene (1 = the mockup's 1 unit per dp) so it leaves room on a short phone; the
 * artboard is split about its centre, so any width from 360 to 430 dp works. The scene's
 * top is [topInset] below the top of the box, and the wall runs up behind the status bar.
 */
@Composable
fun NightRoomScene(modifier: Modifier = Modifier, squeeze: Float = 1f, topInset: Dp = 0.dp) {
    val paths = remember { NightPaths() }
    Spacer(
        modifier.drawWithCache {
            val unit = squeeze * 1.dp.toPx()
            val width = size.width / unit
            val height = size.height / unit
            val top = topInset.toPx() / unit
            val centreShift = width / 2f - ART_CENTRE
            onDrawBehind {
                scale(unit, unit, Offset.Zero) {
                    translate(top = top) {
                        drawRoom(width, height - top, paths)
                        translate(left = centreShift) { drawSky(paths, showTopRightSparkle = squeeze >= SPARKLE_MIN_SQUEEZE) }
                        translate(left = centreShift) { drawRug() }
                    }
                }
            }
        },
    )
}

private class NightPaths {
    private fun parse(data: String) = PathParser().parsePathString(data).toPath()

    val windowFrame = parse("M134 300 V176 A61 61 0 0 1 256 176 V300 Z")
    val windowGlass = parse("M144 292 V178 A51 51 0 0 1 246 178 V292 Z")
    val sparkles = listOf(
        parse("M54 92 l3 7 7 3 -7 3 -3 7 -3 -7 -7 -3 7 -3 z"),
        parse("M330 70 l2.4 5.6 5.6 2.4 -5.6 2.4 -2.4 5.6 -2.4 -5.6 -5.6 -2.4 5.6 -2.4 z"),
        parse("M318 248 l2 4.6 4.6 2 -4.6 2 -2 4.6 -2 -4.6 -4.6 -2 4.6 -2 z"),
    )
}

private fun DrawScope.rect(color: Color, x: Float, y: Float, w: Float, h: Float, r: Float = 0f) =
    if (r == 0f) drawRect(color, Offset(x, y), Size(w, h)) else drawRoundRect(color, Offset(x, y), Size(w, h), CornerRadius(r))

/** Wall, trim and floor, full width. The wall runs up past the top so it fills behind the status bar. */
private fun DrawScope.drawRoom(width: Float, height: Float, paths: NightPaths) {
    rect(NightRoomWall, 0f, -ART_WIDTH, width, WALL_BOTTOM + ART_WIDTH)
    rect(NightRoomLowerWall, 0f, 330f, width, 90f)
    rect(NightRoomTrim, 0f, 326f, width, 5f)
    rect(NightRoomTrim, 0f, TRIM_TOP, width, 13f)
    rect(NightRoomFloor, 0f, FLOOR_TOP, width, height - FLOOR_TOP)
    // Floorboard lines: rows full width, joints alternating, as in the mockup (anchored left and right).
    listOf(476f, 530f, 584f, 638f, 692f, 746f, 800f).forEach { y -> rect(NightRoomFloorLine, 0f, y, width, 2f) }
    val right = width - ART_WIDTH
    listOf(96f to 423f, 290f + right to 423f, 40f to 478f, 250f + right to 478f).forEach { (x, y) ->
        rect(NightRoomFloorLine, x, y, 2f, 53f)
    }
}

/** The window with its moon, the stars and the glow behind the pet. */
private fun DrawScope.drawSky(paths: NightPaths, showTopRightSparkle: Boolean) {
    drawPath(paths.windowFrame, NightRoomTrim)
    drawPath(paths.windowGlass, NightRoomWindowGlass)
    // The moon is a disc with a bite taken out of it, kept inside the glass.
    clipPath(paths.windowGlass) {
        drawCircle(NightRoomMoon, 20f, Offset(220f, 172f))
        drawCircle(NightRoomWindowGlass, 17f, Offset(229f, 165f))
    }
    listOf(168f to 160f to 2f, 182f to 208f to 1.5f, 160f to 246f to 1.8f, 232f to 236f to 1.4f, 206f to 270f to 1.6f, 196f to 150f to 1.3f)
        .forEach { (pos, r) -> drawCircle(NightRoomStar, r, Offset(pos.first, pos.second)) }
    rect(NightRoomTrim, 192f, 118f, 6f, 178f)
    rect(NightRoomTrim, 144f, 226f, 102f, 5f)
    rect(NightRoomLedge, 124f, 292f, 142f, 10f, 5f)
    // The top-right one would sit under the Skip link once the scene is squeezed.
    paths.sparkles.forEachIndexed { i, p -> if (i != 1 || showTopRightSparkle) drawPath(p, NightRoomStar, alpha = 0.9f) }
    listOf(90f to 200f to 2f, 70f to 300f to 1.6f, 350f to 170f to 1.8f)
        .forEach { (pos, r) -> drawCircle(NightRoomStar, r, Offset(pos.first, pos.second), alpha = 0.9f) }
}

private fun DrawScope.drawRug() {
    drawOval(NightRoomRugEdge, Offset(ART_CENTRE - 138f, 452f - 27f), Size(276f, 54f))
    drawOval(NightRoomRug, Offset(ART_CENTRE - 138f, 449f - 26f), Size(276f, 52f))
    drawOval(
        NightRoomRugStitch,
        topLeft = Offset(ART_CENTRE - 114f, 449f - 17f),
        size = Size(228f, 34f),
        style = Stroke(width = 2.5f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(1f, 8f))),
    )
    // Soft glow where the pet sleeps.
    drawCircle(NightRoomStar, 112f, Offset(ART_CENTRE, 390f), alpha = 0.04f)
    drawCircle(NightRoomStar, 88f, Offset(ART_CENTRE, 390f), alpha = 0.05f)
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun NightRoomScenePreview() {
    WigglebunTheme { NightRoomScene(Modifier.fillMaxSize()) }
}
