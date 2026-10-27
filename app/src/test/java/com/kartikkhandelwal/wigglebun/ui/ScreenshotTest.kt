package com.kartikkhandelwal.wigglebun.ui

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButton
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButtonPreview
import com.kartikkhandelwal.wigglebun.ui.components.PillCardPreview
import com.kartikkhandelwal.wigglebun.ui.components.WigglebunTabBarPreview
import com.kartikkhandelwal.wigglebun.ui.navigation.WigglebunNavHostPreview
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the @Previews to PNGs under app/build/outputs/roborazzi.
 * Record with `./gradlew recordRoborazziDebug`, compare with `./gradlew compareRoborazziDebug`.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Plain Application: these tests need no Hilt graph.
@Config(sdk = [36], qualifiers = "w390dp-h844dp-xxhdpi", application = Application::class)
class ScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun chunkyButtons() {
        composeRule.setContent { ChunkyButtonPreview() }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun chunkyButtonPressed() {
        composeRule.setContent {
            WigglebunTheme { ChunkyButton(text = "Pressed", onClick = {}) }
        }
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithText("Pressed").performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun pillCards() {
        composeRule.setContent { PillCardPreview() }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun tabBar() {
        composeRule.setContent { WigglebunTabBarPreview() }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun appShellSwitchesTabs() {
        // Inspection mode = still pet pose; a live animation would keep Compose busy forever.
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) { WigglebunNavHostPreview() }
        }
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/app_shell_home.png")

        listOf("Collection", "Bun", "Settings", "Home").forEach { label ->
            // Screen titles repeat the tab label, so match the selectable tab node only.
            val tab = composeRule.onNode(hasText(label) and isSelectable())
            tab.performClick()
            composeRule.waitForIdle()
            tab.assertIsSelected()
            composeRule.onRoot().captureRoboImage(
                "build/outputs/roborazzi/app_shell_${label.lowercase()}.png",
            )
        }
    }
}
