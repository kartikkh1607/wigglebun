package com.kartikkhandelwal.wigglebun.ui.home

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.kartikkhandelwal.wigglebun.domain.model.ActivePet
import com.kartikkhandelwal.wigglebun.domain.model.PetId
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Final Home (M5) at the mockup's 390×844 phone size, plus a small phone and a large font.
 * Reference PNGs: app/src/test/screenshots/home_*.png.
 *
 * Large fonts use @Config(fontScale): the system setting, like a real phone (non-linear scaling
 * on API 34+), and it also reaches the focused card's dialog window, which a LocalDensity
 * override would not.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w390dp-h844dp-xxhdpi", application = Application::class)
class HomeScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(content: @Composable () -> Unit) {
        composeRule.setContent {
            // Inspection mode: still pet pose and no entrance animation, so frames are stable.
            CompositionLocalProvider(LocalInspectionMode provides true) { content() }
        }
    }

    private fun capture(name: String, content: @Composable () -> Unit) {
        setContent(content)
        composeRule.onRoot().captureRoboImage("home_$name.png")
    }

    /** The whole screen including dialog windows: the real [FocusCardDialog] path. */
    @OptIn(ExperimentalRoborazziApi::class)
    private fun captureScreen(name: String, content: @Composable () -> Unit) {
        setContent(content)
        composeRule.waitForIdle()
        captureScreenRoboImage("home_$name.png")
    }

    private fun home(state: HomeUiState): @Composable () -> Unit = { WigglebunTheme { HomeScreen(state) } }

    @Test fun happy() = capture("happy", home(HomePreviewStates.Happy))

    @Test fun tired() = capture("tired", home(HomePreviewStates.Tired))

    @Test fun stiffWithCta() = capture("stiff", home(HomePreviewStates.Stiff))

    @Test fun onWalk() = capture("on_walk", home(HomePreviewStates.OnWalk))

    @Test fun asleep() = capture("asleep", home(HomePreviewStates.Asleep))

    @Test fun celebrate() = capture("celebrate", home(HomePreviewStates.Celebrate))

    @Test fun focusDue() = capture("focus_due") { HomeWithFocusPreview(HomePreviewStates.FocusDue) }

    @Test fun focusNotDue() = capture("focus_not_due") { HomeWithFocusPreview(HomePreviewStates.FocusNotDue) }

    @Test fun focusDoneToday() = capture("focus_done_today") { HomeWithFocusPreview(HomePreviewStates.FocusDoneToday) }

    @Test
    fun focusInRealDialog() = captureScreen("focus_dialog") { HomeWithFocusPreview(HomePreviewStates.FocusDue, inDialog = true) }

    /** Tall window so all seven cards are visible. */
    @Test
    @Config(qualifiers = "w390dp-h1400dp-xxhdpi")
    fun allDone() = capture("all_done", home(HomePreviewStates.AllDone))

    @Test
    @Config(qualifiers = "w360dp-h640dp-xhdpi")
    fun smallPhone() = capture("small_phone", home(HomePreviewStates.Stiff))

    @Test
    @Config(qualifiers = "w360dp-h640dp-xhdpi", fontScale = 1.3f)
    fun smallPhoneFontScale130() = capture("small_phone_font_1_3", home(HomePreviewStates.Stiff))

    @Test
    @Config(qualifiers = "w360dp-h640dp-xhdpi", fontScale = 1.3f)
    fun smallPhoneFontScale130Focus() = captureScreen("small_phone_font_1_3_focus") {
        HomeWithFocusPreview(HomePreviewStates.FocusDue, inDialog = true)
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xhdpi", fontScale = 1.3f)
    fun smallPhoneFontScale130FocusNotDue() = captureScreen("small_phone_font_1_3_focus_not_due") {
        HomeWithFocusPreview(HomePreviewStates.FocusNotDue, inDialog = true)
    }

    @Test
    @Config(fontScale = 1.3f)
    fun fontScale130() = capture("font_1_3", home(HomePreviewStates.Stiff))

    @Test
    @Config(fontScale = 1.3f)
    fun fontScale130Focus() = captureScreen("font_1_3_focus") { HomeWithFocusPreview(HomePreviewStates.FocusDue, inDialog = true) }

    // ---- M8: the collection pets on Home ----

    private fun withPet(state: HomeUiState, pet: PetId) = home(state.copy(pet = ActivePet(pet, chosenName = null)))

    @Test fun tiredPip() = capture("tired_pip", withPet(HomePreviewStates.Tired, PetId.PIP))

    @Test fun tiredToast() = capture("tired_toast", withPet(HomePreviewStates.Tired, PetId.TOAST))

    @Test fun tiredNimbus() = capture("tired_nimbus", withPet(HomePreviewStates.Tired, PetId.NIMBUS))

    /** The z's float on the left, clear of the plant pot, for every pet (sleepZAnchorLeft). */
    @Test fun asleepPip() = capture("asleep_pip", withPet(HomePreviewStates.Asleep, PetId.PIP))

    @Test fun asleepToast() = capture("asleep_toast", withPet(HomePreviewStates.Asleep, PetId.TOAST))

    @Test fun asleepNimbus() = capture("asleep_nimbus", withPet(HomePreviewStates.Asleep, PetId.NIMBUS))
}
