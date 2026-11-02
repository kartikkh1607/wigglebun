package com.kartikkhandelwal.wigglebun.ui.settings

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
@Config(sdk = [36], qualifiers = "w390dp-h1500dp-xxhdpi", application = Application::class)
class SettingsScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(name: String, content: @Composable () -> Unit) {
        composeRule.setContent { CompositionLocalProvider(LocalInspectionMode provides true, content = content) }
        composeRule.onRoot().captureRoboImage("$name.png")
    }

    @Test
    fun full() = capture("settings_full") { SettingsFullPreview() }

    @Test
    fun overnightOneBreakLeft() = capture("settings_overnight_one_break") { SettingsOvernightOneBreakPreview() }

    @Test
    fun hoursProblem() = capture("settings_hours_problem") { SettingsHoursProblemPreview() }

    @Config(qualifiers = "w390dp-h844dp-xxhdpi")
    @Test
    fun timeSheet() = capture("settings_time_sheet") { SettingsTimeSheetPreview() }

    @Config(qualifiers = "w390dp-h844dp-xxhdpi")
    @Test
    fun intervalSheet() = capture("settings_interval_sheet") { SettingsIntervalSheetPreview() }
}
