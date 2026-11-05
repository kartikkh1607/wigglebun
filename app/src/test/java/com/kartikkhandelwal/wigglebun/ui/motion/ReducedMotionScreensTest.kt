package com.kartikkhandelwal.wigglebun.ui.motion

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kartikkhandelwal.wigglebun.domain.model.ActivePet
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.domain.session.SessionTimer
import com.kartikkhandelwal.wigglebun.ui.breaksession.BreakSessionScreen
import com.kartikkhandelwal.wigglebun.ui.breaksession.BreakSessionUiState
import com.kartikkhandelwal.wigglebun.ui.breaksession.sessionSteps
import com.kartikkhandelwal.wigglebun.ui.breaksession.sessionUiState
import com.kartikkhandelwal.wigglebun.ui.home.HomePreviewStates
import com.kartikkhandelwal.wigglebun.ui.home.HomeScreen
import com.kartikkhandelwal.wigglebun.ui.onboarding.WakeUpScreenRoute
import com.kartikkhandelwal.wigglebun.ui.pet.PetView
import com.kartikkhandelwal.wigglebun.ui.theme.Cream
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * With the phone's "Remove animations" on ([LocalReducedMotion]) the app's own motion stops.
 * Each check has a control with animations on, so it would catch the motion coming back.
 *
 * The clock only moves when a test moves it (autoAdvance off), so "at once" means within one
 * frame, and a live pet can't keep Compose busy.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w390dp-h844dp-xxhdpi", application = Application::class)
class ReducedMotionScreensTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(reducedMotion: Boolean, still: Boolean = false, content: @Composable () -> Unit) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CompositionLocalProvider(
                LocalReducedMotion provides reducedMotion,
                LocalInspectionMode provides still,
            ) {
                WigglebunTheme(content = content)
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun SemanticsNodeInteraction.pixels(): IntArray {
        val image: ImageBitmap = captureToImage()
        return IntArray(image.width * image.height).also { image.readPixels(it) }
    }

    // --- The pet: no breathing, blinking or bob -------------------------------------------

    /** Two frames of an idle pet, half a breath (~2.5 s) apart. */
    private fun idlePetFrames(reducedMotion: Boolean): Pair<IntArray, IntArray> {
        setContent(reducedMotion) {
            PetView(energy = 1f, mood = Mood.HAPPY, modifier = Modifier.testTag("pet").background(Cream).size(200.dp, 210.dp))
        }
        val first = composeRule.onNodeWithTag("pet").pixels()
        composeRule.mainClock.advanceTimeBy(HALF_BREATH_MILLIS)
        return first to composeRule.onNodeWithTag("pet").pixels()
    }

    @Test
    fun thePetHoldsStill() {
        val (first, later) = idlePetFrames(reducedMotion = true)
        assertTrue("the pet moved", first.contentEquals(later))
    }

    @Test
    fun controlThePetBreathesWithAnimationsOn() {
        val (first, later) = idlePetFrames(reducedMotion = false)
        assertFalse("the live pet should move", first.contentEquals(later))
    }

    // --- Home: the break cards are there at once, no stagger ------------------------------

    /** Whether the walk card (second in the list) shows its Ink title one frame after Home opens. */
    private fun walkCardShowsAtOnce(reducedMotion: Boolean): Boolean {
        setContent(reducedMotion) { HomeScreen(HomePreviewStates.Happy) }
        // A staggered card is still fully transparent here: only the Cream list background shows.
        return composeRule.onNodeWithText("2-min walk").pixels().any(::isDark)
    }

    @Test
    fun homeCardsShowAtOnce() = assertTrue(walkCardShowsAtOnce(reducedMotion = true))

    @Test
    fun controlHomeCardsStaggerInWithAnimationsOn() = assertFalse(walkCardShowsAtOnce(reducedMotion = false))

    // --- Home's top: room, pet, speech bubble and streak row are still --------------------

    /**
     * Home (Tired: a bubble and a streak chip) once everything has entered, and again half a
     * breath later. The room scene, bubble and streak row have no motion of their own; this
     * pins that nothing on the screen moves with animations removed.
     */
    private fun homeFrames(reducedMotion: Boolean): Pair<IntArray, IntArray> {
        setContent(reducedMotion) { HomeScreen(HomePreviewStates.Tired) }
        composeRule.onNodeWithText("Streak goal", useUnmergedTree = true).assertExists()
        composeRule.mainClock.advanceTimeBy(ENTRANCE_DONE_MILLIS)
        val first = composeRule.onRoot().pixels()
        composeRule.mainClock.advanceTimeBy(HALF_BREATH_MILLIS)
        return first to composeRule.onRoot().pixels()
    }

    @Test
    fun homeHoldsStill() {
        val (first, later) = homeFrames(reducedMotion = true)
        assertTrue("something on Home moved", first.contentEquals(later))
    }

    @Test
    fun controlHomeMovesWithAnimationsOn() {
        val (first, later) = homeFrames(reducedMotion = false)
        assertFalse("the live pet on Home should move", first.contentEquals(later))
    }

    // --- Wake-up: with animations removed the tap goes straight on --------------------------

    private var continued = 0

    /** Taps the wake button and moves one frame on, far short of the 1.6 s stretch. */
    private fun tapToWake(reducedMotion: Boolean) {
        continued = 0
        setContent(reducedMotion) { WakeUpScreenRoute(onContinue = { continued++ }, onSkip = {}) }
        composeRule.onNodeWithText("Tap to wake them up").performClick()
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun petShows(description: String): Boolean =
        composeRule.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun theTapSkipsTheStretch() {
        tapToWake(reducedMotion = true)
        assertEquals(1, continued)
        assertFalse(petShows(STRETCHING))
    }

    @Test
    fun controlThePetStretchesFirstWithAnimationsOn() {
        tapToWake(reducedMotion = false)
        assertTrue(petShows(STRETCHING))
        assertEquals(0, continued)
        composeRule.mainClock.advanceTimeBy(2_000)
        assertEquals(1, continued)
    }

    // --- Break session: each step holds one pose ------------------------------------------

    /** Shoulder rolls, step 1, [millis] in: its four poses cycle every 1.2 s. */
    private fun shouldersAt(millis: Long): BreakSessionUiState {
        val type = BreakType.SHOULDERS
        val timer = SessionTimer.start(type.durationMinutes * MILLIS_PER_MINUTE, type.sessionSteps().size, now = 0L)
        // The ring held still, so only the pet can differ between the two frames.
        return sessionUiState(type, ActivePet.Default, timer, now = millis, result = null).copy(progress = 0f)
    }

    /** The pet at the start of the step and 2.5 s later (third pose of the cycle). */
    private fun sessionPetFrames(reducedMotion: Boolean): Pair<IntArray, IntArray> {
        var state by mutableStateOf(shouldersAt(0))
        // Still pet (no idle motion), so a difference can only come from the pose.
        setContent(reducedMotion, still = true) { BreakSessionScreen(state) }
        val pet = composeRule.onNodeWithContentDescription(STRETCHING)
        val first = pet.pixels()
        // Applied at once, so the paused clock's next frames recompose with it.
        Snapshot.withMutableSnapshot { state = shouldersAt(POSE_CYCLE_CHECK_MILLIS) }
        // A few frames: recompose, then let the (snapped) pose transforms apply.
        composeRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
        return first to pet.pixels()
    }

    @Test
    fun theSessionHoldsOnePosePerStep() {
        val (first, later) = sessionPetFrames(reducedMotion = true)
        assertTrue("the pose changed within a step", first.contentEquals(later))
    }

    @Test
    fun controlTheSessionCyclesPosesWithAnimationsOn() {
        val (first, later) = sessionPetFrames(reducedMotion = false)
        assertFalse("the pose should cycle", first.contentEquals(later))
    }

    private companion object {
        const val HALF_BREATH_MILLIS = 1_250L
        const val ENTRANCE_DONE_MILLIS = 2_000L
        const val POSE_CYCLE_CHECK_MILLIS = 2_500L
        const val SETTLE_MILLIS = 100L
        const val MILLIS_PER_MINUTE = 60_000L
        const val STRETCHING = "Bun, stretching"

        /** Ink text (#2B2A3A) is dark; the Cream background and white card are not. */
        fun isDark(argb: Int): Boolean {
            val r = argb shr 16 and 0xFF
            val g = argb shr 8 and 0xFF
            val b = argb and 0xFF
            return r + g + b < DARK_SUM
        }

        const val DARK_SUM = 3 * 0x60
    }
}
