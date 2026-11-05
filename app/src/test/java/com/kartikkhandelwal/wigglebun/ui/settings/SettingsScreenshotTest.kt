package com.kartikkhandelwal.wigglebun.ui.settings

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.kartikkhandelwal.wigglebun.ui.pet.TestPetNames
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme
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
    fun hoursEditing() = capture("settings_hours_editing") { SettingsHoursEditingPreview() }

    @Test
    fun hoursProblem() = capture("settings_hours_problem") { SettingsHoursProblemPreview() }

    /** No work days: Save is off at once and the reason shows above it. */
    @Test
    fun hoursNoDays() = capture("settings_hours_no_days") { SettingsHoursNoDaysPreview() }

    /** Next to design/ui_v2/Settings.dc.html. */
    @Config(qualifiers = "w390dp-h844dp-xxhdpi")
    @Test
    fun mockup() = capture("settings_mockup") { SettingsMockupPreview() }

    /** The widest 12-letter name: only the name shortens, the "& you" card keeps its height. */
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    @Test
    fun widestNameSmallPhone() = capture("settings_widest_name_small_phone") {
        WigglebunTheme {
            SettingsScreen(SettingsPreviewStates.named(TestPetNames.WIDEST), SettingsPreviewStates.Mockup, SettingsActions())
        }
    }

    /** 200% font on a small phone: rows wrap, the buttons and chips stay whole. */
    @Config(qualifiers = "w360dp-h1500dp-xxhdpi", fontScale = 2.0f)
    @Test
    fun smallPhoneFont2() = capture("settings_small_phone_font_2_0") {
        WigglebunTheme {
            SettingsScreen(SettingsPreviewStates.named(TestPetNames.LONG), SettingsPreviewStates.Mockup, SettingsActions())
        }
    }

    @Config(qualifiers = "w390dp-h844dp-xxhdpi")
    @Test
    fun timeSheet() = capture("settings_time_sheet") { SettingsTimeSheetPreview() }

    @Config(qualifiers = "w390dp-h844dp-xxhdpi")
    @Test
    fun intervalSheet() = capture("settings_interval_sheet") { SettingsIntervalSheetPreview() }
}
