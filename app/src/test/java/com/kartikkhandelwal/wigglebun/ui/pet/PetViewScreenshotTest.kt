package com.kartikkhandelwal.wigglebun.ui.pet

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.ui.theme.Cream
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders every mood at 384 dpi, so the 200×210 dp pet is exactly 480×504 px, the same size
 * as design/widget_png/bun_mood_<mood>.png, for a pixel comparison.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-384dpi", application = Application::class)
class PetViewScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test fun happy() = captureMood(Mood.HAPPY)

    @Test fun tired() = captureMood(Mood.TIRED)

    @Test fun stiff() = captureMood(Mood.STIFF)

    @Test fun stretch() = captureMood(Mood.STRETCH)

    @Test fun celebrate() = captureMood(Mood.CELEBRATE)

    @Test fun sleeping() = captureMood(Mood.SLEEPING)

    @Test
    fun allMoods() {
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) { PetAllMoodsPreview() }
        }
        composeRule.onRoot().captureRoboImage("pet_all_moods.png")
    }

    /** One live frame mid-hop with confetti flying, to sanity-check the animation drawing. */
    @Test
    fun celebrateAnimatedFrame() {
        var mood by mutableStateOf(Mood.HAPPY)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            WigglebunTheme {
                PetView(
                    energy = 1f,
                    mood = mood,
                    animated = true,
                    modifier = Modifier.testTag("pet").background(Cream).size(200.dp, 210.dp),
                )
            }
        }
        composeRule.mainClock.advanceTimeBy(500)
        mood = Mood.CELEBRATE
        composeRule.mainClock.advanceTimeBy(300)
        composeRule.onNodeWithTag("pet").captureRoboImage("pet_celebrate_frame.png")
    }

    private fun captureMood(mood: Mood) {
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) { PetMoodPreview(mood) }
        }
        composeRule.onNodeWithTag("pet")
            .captureRoboImage("pet_${mood.name.lowercase()}.png")
    }
}
