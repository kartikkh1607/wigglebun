package com.kartikkhandelwal.wigglebun.ui.onboarding

import android.app.Application
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.kartikkhandelwal.wigglebun.ui.home.HomePreviewStates
import com.kartikkhandelwal.wigglebun.ui.home.HomeScreen
import com.kartikkhandelwal.wigglebun.ui.paywall.PaywallPlaceholderPreview
import com.kartikkhandelwal.wigglebun.ui.settings.SettingsAllOffPreview
import com.kartikkhandelwal.wigglebun.ui.settings.SettingsExplainPreview
import com.kartikkhandelwal.wigglebun.ui.settings.SettingsMixedPreview
import com.kartikkhandelwal.wigglebun.ui.settings.SettingsPreviewStates
import com.kartikkhandelwal.wigglebun.ui.settings.SettingsSmallLargeFontPreview
import com.kartikkhandelwal.wigglebun.ui.setup.SetupHomePrompt
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme
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

    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    @Test
    fun wakeUpSmallPhone() = capture("onboarding_wake_small_phone") { WakeUpSmallPhonePreview() }

    @Config(qualifiers = "w360dp-h640dp-xxhdpi", fontScale = 2.0f)
    @Test
    fun wakeUpSmallPhoneHugeFont() = capture("onboarding_wake_small_font") { WakeUpSmallHugeFontPreview() }

    @Test
    fun nameEmpty() = capture("onboarding_name_empty") { NamePetEmptyPreview() }

    @Test
    fun nameMockup() = capture("onboarding_name_mockup") { NamePetMockupPreview() }

    @Test
    fun nameTyped() = capture("onboarding_name_typed") { NamePetTypedPreview() }

    @Test
    fun nameLong() = capture("onboarding_name_long") { NamePetLongNamePreview() }

    @Config(qualifiers = "w360dp-h640dp-xxhdpi", fontScale = 1.3f)
    @Test
    fun nameSmallPhoneLargeFont() = capture("onboarding_name_small_font") { NamePetSmallLargeFontPreview() }

    @Config(qualifiers = "w360dp-h640dp-xxhdpi", fontScale = 2.0f)
    @Test
    fun nameSmallPhoneHugeFont() = capture("onboarding_name_small_font_2_0") { NamePetSmallHugeFontPreview() }

    @Test
    fun workHours() = capture("onboarding_hours") { WorkHoursPreview() }

    /** No work days: Continue is off at once and the reason shows above it. */
    @Test
    fun workHoursNoDays() = capture("onboarding_hours_no_days") { WorkHoursNoDaysPreview() }

    /** Continue found the same start and end: Continue stays on, the reason shows above it. */
    @Test
    fun workHoursSameTime() = capture("onboarding_hours_same_time") { WorkHoursSameTimePreview() }

    @Test
    fun workHoursSameTimeWhileEditing() = capture("onboarding_hours_same_time_editing") { WorkHoursSameTimeEditingPreview() }

    @Test
    fun workHoursOvernight() = capture("onboarding_hours_overnight") { WorkHoursOvernightPreview() }

    @Test
    fun workHoursTimeSheet() = capture("onboarding_hours_time_sheet") { WorkHoursTimeSheetPreview() }

    @Test
    fun workHoursShort() = capture("onboarding_hours_short") { WorkHoursShortPreview() }

    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    @Test
    fun workHoursSmallPhone() = capture("onboarding_hours_small_phone") { WorkHoursSmallPhonePreview() }

    @Config(qualifiers = "w360dp-h640dp-xxhdpi", fontScale = 2.0f)
    @Test
    fun workHoursSmallPhoneHugeFont() = capture("onboarding_hours_small_font_2_0") { WorkHoursSmallHugeFontPreview() }

    @Config(qualifiers = "w360dp-h640dp-xxhdpi", fontScale = 1.3f)
    @Test
    fun workHoursSmallPhoneLargeFont() = capture("onboarding_hours_small_font") { WorkHoursSmallLargeFontPreview() }

    @Test
    fun interval() = capture("onboarding_interval") { IntervalPreview() }

    @Test
    fun intervalShortest() = capture("onboarding_interval_shortest") { IntervalShortestPreview() }

    @Test
    fun intervalLongest() = capture("onboarding_interval_longest") { IntervalLongestPreview() }

    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    @Test
    fun intervalSmallPhone() = capture("onboarding_interval_small_phone") { IntervalSmallPhonePreview() }

    @Config(qualifiers = "w360dp-h640dp-xxhdpi", fontScale = 2.0f)
    @Test
    fun intervalSmallPhoneHugeFont() = capture("onboarding_interval_small_font_2_0") { IntervalSmallHugeFontPreview() }

    @Config(qualifiers = "w360dp-h640dp-xxhdpi", fontScale = 1.3f)
    @Test
    fun intervalSmallPhoneLargeFont() = capture("onboarding_interval_small_font") { IntervalSmallLargeFontPreview() }

    @Test
    fun firstStretch() = capture("onboarding_stretch") { FirstStretchPreview() }

    @Test
    fun firstStretchLongName() = capture("onboarding_stretch_long_name") { FirstStretchLongNamePreview() }

    @Config(qualifiers = "w360dp-h640dp-xxhdpi", fontScale = 1.3f)
    @Test
    fun firstStretchSmallPhoneLargeFont() = capture("onboarding_stretch_small_font") { FirstStretchSmallLargeFontPreview() }

    @Test
    fun settingsAllOff() = capture("settings_all_off") { SettingsAllOffPreview() }

    @Test
    fun settingsMixed() = capture("settings_mixed") { SettingsMixedPreview() }

    @Config(qualifiers = "w360dp-h640dp-xxhdpi", fontScale = 1.3f)
    @Test
    fun settingsSmallPhoneLargeFont() = capture("settings_small_font") { SettingsSmallLargeFontPreview() }

    @Test
    fun explainNotifications() = capture("setup_explain_notifications") { SettingsExplainPreview(SettingsPreviewStates.Notifications) }

    @Test
    fun explainMovement() = capture("setup_explain_movement") { SettingsExplainPreview(SettingsPreviewStates.Movement) }

    @Test
    fun explainMovementBlocked() = capture("setup_explain_movement_blocked") { SettingsExplainPreview(SettingsPreviewStates.BlockedMovement) }

    @Test
    fun explainWidget() = capture("setup_explain_widget") { SettingsExplainPreview(SettingsPreviewStates.Widget) }

    @Test
    fun explainWidgetManual() = capture("setup_explain_widget_manual") { SettingsExplainPreview(SettingsPreviewStates.ManualWidget) }

    @Test
    fun homeWithSetupPrompt() = capture("home_setup_prompt") {
        WigglebunTheme {
            HomeScreen(
                HomePreviewStates.Tired,
                setupPrompt = {
                    SetupHomePrompt(
                        SettingsPreviewStates.Notifications,
                        onOpen = {},
                        onNotNow = {},
                        modifier = Modifier.padding(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, top = Dimens.Setup.HomePromptTop),
                    )
                },
            )
        }
    }

    @Test
    fun paywallPlaceholder() = capture("paywall_placeholder") { PaywallPlaceholderPreview() }

    @Config(qualifiers = "w360dp-h640dp-xxhdpi", fontScale = 1.3f)
    @Test
    fun nameSmallPhoneLargeFontWidestName() = capture("onboarding_name_small_font") { NamePetSmallLargeFontPreview() }
}
