package com.kartikkhandelwal.wigglebun.ui.pet.profile

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

/** Pet profile tab (M8). Reference PNGs: app/src/test/screenshots/pet_profile_*.png. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w390dp-h844dp-xxhdpi", application = Application::class)
class PetProfileScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(name: String, state: PetProfileUiState) {
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) { WigglebunTheme { PetProfileScreen(state) } }
        }
        composeRule.onRoot().captureRoboImage("pet_profile_$name.png")
    }

    @Test fun emptyWeek() = capture("empty_week", PetProfilePreviewStates.EmptyWeek)

    @Test fun busyWeek() = capture("busy_week", PetProfilePreviewStates.BusyWeek)

    @Test
    @Config(qualifiers = "w360dp-h640dp-xhdpi", fontScale = 1.3f)
    fun smallPhoneFontScale130() = capture("small_font", PetProfilePreviewStates.BusyWeek)
}
