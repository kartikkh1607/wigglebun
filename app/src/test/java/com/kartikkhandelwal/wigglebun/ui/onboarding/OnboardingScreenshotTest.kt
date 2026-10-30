package com.kartikkhandelwal.wigglebun.ui.onboarding

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

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w390dp-h844dp-xxhdpi", application = Application::class)
class OnboardingScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(name: String, content: @Composable () -> Unit) {
        // Inspection mode = still pet pose; a live animation would keep Compose busy forever.
        composeRule.setContent { CompositionLocalProvider(LocalInspectionMode provides true, content = content) }
        composeRule.onRoot().captureRoboImage("$name.png")
    }

    @Test
    fun wakeUpAsleep() = capture("onboarding_wake_asleep") { WakeUpAsleepPreview() }

    @Test
    fun wakeUpStretching() = capture("onboarding_wake_stretching") { WakeUpStretchingPreview() }

    @Test
    fun wakeUpAwake() = capture("onboarding_wake_awake") { WakeUpAwakePreview() }

    @Config(qualifiers = "w360dp-h640dp-xxhdpi", fontScale = 1.3f)
    @Test
    fun wakeUpSmallPhoneLargeFont() = capture("onboarding_wake_small_font") { WakeUpSmallLargeFontPreview() }

    @Test
    fun nameEmpty() = capture("onboarding_name_empty") { NamePetEmptyPreview() }

    @Test
    fun nameTyped() = capture("onboarding_name_typed") { NamePetTypedPreview() }

    @Config(qualifiers = "w360dp-h640dp-xxhdpi", fontScale = 1.3f)
    @Test
    fun nameSmallPhoneLargeFontWidestName() = capture("onboarding_name_small_font") { NamePetSmallLargeFontPreview() }
}
