package com.kartikkhandelwal.wigglebun.ui.breaksession

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Break session (M6) at the mockup's 390×844, one mid-step frame per break type, plus paused,
 * last step, both celebrations and a small phone at font scale 1.3.
 * Reference PNGs: app/src/test/screenshots/session_*.png.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w390dp-h844dp-xxhdpi", application = Application::class)
class BreakSessionScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(name: String, state: BreakSessionUiState) {
        composeRule.setContent {
            // Inspection mode: the exact still pose, no idle motion, so frames are stable.
            CompositionLocalProvider(LocalInspectionMode provides true) {
                WigglebunTheme { BreakSessionScreen(state) }
            }
        }
        composeRule.onRoot().captureRoboImage("session_$name.png")
    }

    @Test fun standUp() = capture("stand_up", BreakSessionPreviewStates.StandUp)

    @Test fun walk() = capture("walk", BreakSessionPreviewStates.Walk)

    @Test fun neck() = capture("neck", BreakSessionPreviewStates.Neck)

    @Test fun shoulders() = capture("shoulders", BreakSessionPreviewStates.Shoulders)

    @Test fun wrist() = capture("wrist", BreakSessionPreviewStates.Wrist)

    @Test fun eyes() = capture("eyes", BreakSessionPreviewStates.Eyes)

    @Test fun water() = capture("water", BreakSessionPreviewStates.Water)

    @Test fun paused() = capture("paused", BreakSessionPreviewStates.Paused)

    @Test fun lastStepDone() = capture("last_step", BreakSessionPreviewStates.LastStepDone)

    @Test fun celebrateWithBadge() = capture("celebrate_badge", BreakSessionPreviewStates.CelebrateWithBadge)

    @Test fun celebrateWithoutBadge() = capture("celebrate_no_badge", BreakSessionPreviewStates.CelebrateNoBadge)

    @Test
    @Config(qualifiers = "w360dp-h640dp-xhdpi", fontScale = 1.3f)
    fun smallPhoneFontScale130() = capture("small_phone_font_1_3", BreakSessionPreviewStates.Neck)

    @Test
    @Config(qualifiers = "w360dp-h640dp-xhdpi", fontScale = 1.3f)
    fun smallPhoneFontScale130Celebrate() = capture("small_phone_font_1_3_celebrate", BreakSessionPreviewStates.CelebrateWithBadge)
}
