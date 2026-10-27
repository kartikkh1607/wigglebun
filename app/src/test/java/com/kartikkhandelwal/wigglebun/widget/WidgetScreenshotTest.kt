package com.kartikkhandelwal.wigglebun.widget

import android.app.Application
import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.unit.DpSize
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.roundToInt

/**
 * The widget as the launcher would show it: Glance content → RemoteViews → inflated views.
 * Reference PNGs: app/src/test/screenshots/widget_*.png.
 */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w390dp-h844dp-xxhdpi", application = Application::class)
class WidgetScreenshotTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun capture(name: String, state: WidgetUiState, size: DpSize) {
        val medium = size.width >= WigglebunWidget.MEDIUM.width
        val remoteViews = runBlocking {
            GlanceRemoteViews().compose(context, size) { WidgetContent(state, medium) }.remoteViews
        }
        val density = context.resources.displayMetrics.density
        val width = (size.width.value * density).roundToInt()
        val height = (size.height.value * density).roundToInt()
        val host = FrameLayout(context)
        host.addView(remoteViews.apply(context, host), FrameLayout.LayoutParams(width, height))
        host.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        host.layout(0, 0, width, height)
        host.captureRoboImage("widget_$name.png")
    }

    @Test
    fun smallHappy() = capture("small_happy", WidgetPreviewStates.Happy, WigglebunWidget.SMALL)

    @Test
    fun smallStiff() = capture("small_stiff", WidgetPreviewStates.Stiff, WigglebunWidget.SMALL)

    @Test
    fun mediumTired() = capture("medium_tired", WidgetPreviewStates.Tired, WigglebunWidget.MEDIUM)

    @Test
    fun mediumSleeping() = capture("medium_sleeping", WidgetPreviewStates.Sleeping, WigglebunWidget.MEDIUM)
}
