package com.kartikkhandelwal.wigglebun.ui.pet

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Accessory art on every pet (M8b). Reference PNGs: accessories_*.png. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w760dp-h900dp-xxhdpi", application = Application::class)
class AccessoryScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(name: String, content: @Composable () -> Unit) {
        composeRule.setContent { CompositionLocalProvider(LocalInspectionMode provides true) { content() } }
        composeRule.onRoot().captureRoboImage("accessories_$name.png")
    }

    @Test fun everyPetWithEveryAccessory() = capture("grid") { AccessoryGridPreview() }

    @Test fun outfitThroughEveryMood() = capture("moods") { AccessoryMoodsPreview() }

    @Test fun outfitInBreakPoses() = capture("poses") { AccessoryPosesPreview() }
}
