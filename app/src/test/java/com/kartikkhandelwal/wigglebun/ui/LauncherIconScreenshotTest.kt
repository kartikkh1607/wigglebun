package com.kartikkhandelwal.wigglebun.ui

import android.app.Application
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.PathParser
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.ui.theme.Cream
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.InkMuted
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import androidx.compose.foundation.Canvas as ComposeCanvas

/**
 * The adaptive launcher icon (mipmap/ic_launcher) as launchers show it: both layers with the
 * safe zone marked, under the common OEM masks, and as a themed (monochrome) icon.
 * Reference PNG: app/src/test/screenshots/launcher_icon.png.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w390dp-h844dp-xxhdpi", application = Application::class)
class LauncherIconScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun launcherIcon() {
        composeRule.setContent { WigglebunTheme { LauncherIconSheet() } }
        composeRule.onRoot().captureRoboImage("launcher_icon.png")
    }
}

/** Mask outlines in a 100×100 box, as in the platform's config_icon_mask and OEM overlays. */
private enum class IconMask(val label: String, val pathData: String) {
    Circle("Circle", "M50,0 A50,50,0,1,1,50,100 A50,50,0,1,1,50,0 Z"),
    Squircle("Squircle", "M50,0 C10,0 0,10 0,50 0,90 10,100 50,100 90,100 100,90 100,50 100,10 90,0 50,0 Z"),
    RoundedSquare(
        "Rounded square",
        "M50,0 L92,0 C96.42,0 100,3.58 100,8 L100,92 C100,96.42 96.42,100 92,100 L8,100 C3.58,100 0,96.42 0,92 L0,8 C0,3.58 3.58,0 8,0 Z",
    ),
    Teardrop("Teardrop", "M50,0 A50,50,0,0,1,100,50 L100,85 A15,15,0,0,1,85,100 L50,100 A50,50,0,0,1,50,0 Z"),
}

/** Roughly what a Material You launcher uses for themed icons (test-only colours). */
private enum class ThemedColors(val label: String, val background: Int, val foreground: Int) {
    Light("Themed light", 0xFFD8E2FF.toInt(), 0xFF1F3A73.toInt()),
    Dark("Themed dark", 0xFF2B2E38.toInt(), 0xFFD8E2FF.toInt()),
}

private val Cell = 96.dp
private val Small = 48.dp

@Composable
private fun LauncherIconSheet() {
    val context = LocalContext.current
    val icon = remember { ContextCompat.getDrawable(context, R.mipmap.ic_launcher) as AdaptiveIconDrawable }
    Column(
        modifier = Modifier.background(Cream).padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.ListGap),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ListGap)) {
            Labeled("Layers + safe zone") { IconCanvas(Cell) { drawLayers(icon, it) } }
            Labeled(IconMask.Circle.label) { IconCanvas(Cell) { drawMasked(icon, IconMask.Circle, null, it) } }
            Labeled(IconMask.Squircle.label) { IconCanvas(Cell) { drawMasked(icon, IconMask.Squircle, null, it) } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ListGap)) {
            Labeled(IconMask.RoundedSquare.label) { IconCanvas(Cell) { drawMasked(icon, IconMask.RoundedSquare, null, it) } }
            Labeled(IconMask.Teardrop.label) { IconCanvas(Cell) { drawMasked(icon, IconMask.Teardrop, null, it) } }
            Labeled(ThemedColors.Light.label) { IconCanvas(Cell) { drawMasked(icon, IconMask.Circle, ThemedColors.Light, it) } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ListGap)) {
            Labeled(ThemedColors.Dark.label) { IconCanvas(Cell) { drawMasked(icon, IconMask.Circle, ThemedColors.Dark, it) } }
            Labeled("Home screen size (48 dp)") {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ListGap)) {
                    IconCanvas(Small) { drawMasked(icon, IconMask.Circle, null, it) }
                    IconCanvas(Small) { drawMasked(icon, IconMask.Squircle, null, it) }
                    IconCanvas(Small) { drawMasked(icon, IconMask.Circle, ThemedColors.Light, it) }
                    IconCanvas(Small) { drawMasked(icon, IconMask.Circle, ThemedColors.Dark, it) }
                }
            }
        }
    }
}

@Composable
private fun Labeled(label: String, content: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        content()
        Text(label, style = MaterialTheme.typography.labelMedium, color = InkMuted)
    }
}

/** [draw] gets the platform canvas and the square's side in px. */
@Composable
private fun IconCanvas(size: Dp, draw: Canvas.(Float) -> Unit) {
    ComposeCanvas(Modifier.size(size)) {
        drawIntoCanvas { it.nativeCanvas.draw(this.size.width) }
    }
}

/** The full 108-unit layers, with the 72-unit visible area and the 66-unit safe zone marked. */
private fun Canvas.drawLayers(icon: AdaptiveIconDrawable, size: Float) {
    icon.background.setBounds(0, 0, size.toInt(), size.toInt())
    icon.background.draw(this)
    icon.foreground.setBounds(0, 0, size.toInt(), size.toInt())
    icon.foreground.draw(this)
    val unit = size / 108f
    val guide = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = unit * 0.6f
        color = InkMuted.toArgb()
        pathEffect = DashPathEffect(floatArrayOf(unit * 2f, unit * 2f), 0f)
    }
    drawRect(18 * unit, 18 * unit, 90 * unit, 90 * unit, guide)
    drawCircle(54 * unit, 54 * unit, 33 * unit, guide)
}

/**
 * Like AdaptiveIconDrawable: the mask covers the central 72 of the 108 units, so the layers are
 * drawn at 1.5× the visible size, centred. [themed] draws the monochrome layer tinted instead.
 */
private fun Canvas.drawMasked(icon: AdaptiveIconDrawable, mask: IconMask, themed: ThemedColors?, size: Float) {
    val outline = Path(PathParser.createPathFromPathData(mask.pathData)).apply {
        transform(Matrix().apply { setScale(size / 100f, size / 100f) })
    }
    save()
    clipPath(outline)
    val inset = (size / 4f).toInt()
    val layerBounds = Rect(-inset, -inset, size.toInt() + inset, size.toInt() + inset)
    if (themed == null) {
        icon.background.bounds = layerBounds
        icon.background.draw(this)
        icon.foreground.bounds = layerBounds
        icon.foreground.draw(this)
    } else {
        drawColor(themed.background)
        val mono = requireNotNull(icon.monochrome).mutate()
        mono.setTint(themed.foreground)
        mono.bounds = layerBounds
        mono.draw(this)
    }
    restore()
}
