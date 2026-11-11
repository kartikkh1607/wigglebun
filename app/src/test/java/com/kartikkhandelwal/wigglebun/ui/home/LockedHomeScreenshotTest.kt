package com.kartikkhandelwal.wigglebun.ui.home

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w390dp-h760dp-xxhdpi", application = Application::class)
class LockedHomeScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(name: String, content: @Composable () -> Unit) {
        // Inspection mode = still pet pose; a live animation would keep Compose busy forever.
        composeRule.setContent { CompositionLocalProvider(LocalInspectionMode provides true, content = content) }
        composeRule.onRoot().captureRoboImage("$name.png")
    }

    @Test
    fun trial() = capture("locked_home_trial") { LockedHomeTrialPreview() }

    @Test
    fun noTrial() = capture("locked_home_no_trial") { LockedHomeNoTrialPreview() }

    @Test
    fun fresh() = capture("locked_home_fresh") { LockedHomeFreshPreview() }

    @Config(qualifiers = "w360dp-h560dp-xxhdpi", fontScale = 1.3f)
    @Test
    fun smallPhoneLargeFont() = capture("locked_home_small_font_1_3") { LockedHomeSmallLargeFontPreview() }

    /** The button only promises a trial the store's offer has. */
    @Test
    fun theButtonSaysWhatTheStoreOffers() {
        var opened = 0
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                WigglebunTheme { LockedHomeScreen(LockedHomePreviewStates.noTrial, onOpenPaywall = { opened++ }) }
            }
        }
        composeRule.onNodeWithText("Start 7-day free trial").assertDoesNotExist()
        composeRule.onNodeWithText("Then \$29.99 a year. Cancel anytime in Google Play.").assertDoesNotExist()
        composeRule.onNodeWithText("See plans").performClick()
        assertEquals(1, opened)
    }
}
