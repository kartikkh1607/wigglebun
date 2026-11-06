package com.kartikkhandelwal.wigglebun.ui.breaksession

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.kartikkhandelwal.wigglebun.domain.model.AccessoryId
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.ui.motion.LocalReducedMotion
import com.kartikkhandelwal.wigglebun.ui.pet.TestPetNames
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

    private fun capture(name: String, state: BreakSessionUiState, reducedMotion: Boolean = false) {
        composeRule.setContent {
            // Inspection mode: the exact still pose, no idle motion, so frames are stable.
            CompositionLocalProvider(
                LocalInspectionMode provides true,
                LocalReducedMotion provides reducedMotion,
            ) {
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

    // ---- The first stretch's celebration (design/ui_v2/Celebrate.dc.html): +5 sparks, 1 of 3 breaks ----

    /** Sparks, a streak bonus and the streak card: all three cards on screen at once, no scrolling, at 390 x 844. */
    @Test fun celebrateThreeCards() = capture("celebrate_three_cards", BreakSessionPreviewStates.CelebrateWithBadge)

    @Test fun celebrateFirst() = capture("celebrate_first", BreakSessionPreviewStates.CelebrateFirst)

    // ---- 360 × 640: nothing important cut off at normal font; 130% and 200% shrink the ring and scroll ----

    @Test
    @Config(qualifiers = "w360dp-h640dp-xhdpi")
    fun smallPhone() = capture("small_phone", BreakSessionPreviewStates.StandUp)

    @Test
    @Config(qualifiers = "w360dp-h640dp-xhdpi", fontScale = 1.3f)
    fun smallPhoneFontScale130() = capture("small_phone_font_1_3", BreakSessionPreviewStates.Neck)

    @Test
    @Config(qualifiers = "w360dp-h640dp-xhdpi", fontScale = 2f)
    fun smallPhoneFontScale200() = capture("small_phone_font_2_0", BreakSessionPreviewStates.Neck)

    @Test
    @Config(qualifiers = "w360dp-h640dp-xhdpi")
    fun smallPhoneCelebrate() = capture("small_phone_celebrate", BreakSessionPreviewStates.CelebrateFirst)

    @Test
    @Config(qualifiers = "w360dp-h640dp-xhdpi", fontScale = 1.3f)
    fun smallPhoneFontScale130Celebrate() = capture("small_phone_font_1_3_celebrate", BreakSessionPreviewStates.CelebrateWithBadge)

    @Test
    @Config(qualifiers = "w360dp-h640dp-xhdpi", fontScale = 2f)
    fun smallPhoneFontScale200Celebrate() = capture("small_phone_font_2_0_celebrate", BreakSessionPreviewStates.CelebrateWithBadge)

    // ---- The longest pet name (12 letters), paused with the chip saying so ----

    private fun BreakSessionUiState.named(name: String) = copy(pet = pet?.copy(chosenName = name))

    @Test
    @Config(qualifiers = "w360dp-h640dp-xhdpi")
    fun longPetName() = capture("long_name", BreakSessionPreviewStates.StandUp.named(TestPetNames.WIDEST))

    @Test
    @Config(qualifiers = "w360dp-h640dp-xhdpi")
    fun longPetNameCelebrate() = capture("long_name_celebrate", BreakSessionPreviewStates.CelebrateFirst.named(TestPetNames.WIDEST))

    @Test
    @Config(qualifiers = "w360dp-h640dp-xhdpi", fontScale = 1.3f)
    fun longPetNameFontScale130Celebrate() =
        capture("long_name_font_1_3_celebrate", BreakSessionPreviewStates.CelebrateFirst.named(TestPetNames.WIDEST))

    // ---- "Remove animations" on: each step holds its first pose ----

    @Test fun reducedMotion() = capture("reduced_motion", BreakSessionPreviewStates.Shoulders, reducedMotion = true)

    @Test fun reducedMotionCelebrate() = capture("reduced_motion_celebrate", BreakSessionPreviewStates.CelebrateFirst, reducedMotion = true)

    // ---- The pet's colour fills over the session: first step, last step ----

    @Test fun colourAtTheStart() = capture("colour_start", BreakSessionPreviewStates.midStep(BreakType.NECK, step = 0, remainingSeconds = 15))

    @Test fun colourBack() = capture("colour_back", BreakSessionPreviewStates.LastStepDone)

    // ---- Accessories (M8b): worn during the session and in the celebration ----

    private val outfit = listOf(AccessoryId.FLOWER_CLIP, AccessoryId.KNIT_SCARF)

    @Test
    fun neckWearingAccessories() =
        capture("neck_accessories", BreakSessionPreviewStates.Neck.copy(pet = BreakSessionPreviewStates.Neck.pet?.copy(wearing = outfit)))

    @Test
    fun celebrateWearingAccessories() = capture(
        "celebrate_accessories",
        BreakSessionPreviewStates.CelebrateWithBadge.copy(pet = BreakSessionPreviewStates.CelebrateWithBadge.pet?.copy(wearing = outfit)),
    )
}
