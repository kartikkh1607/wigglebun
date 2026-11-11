package com.kartikkhandelwal.wigglebun.ui.paywall

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
class PaywallScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(name: String, content: @Composable () -> Unit) {
        // Inspection mode = still pet pose; a live animation would keep Compose busy forever.
        composeRule.setContent { CompositionLocalProvider(LocalInspectionMode provides true, content = content) }
        composeRule.onRoot().captureRoboImage("$name.png")
    }

    @Test
    fun yearly() = capture("paywall_yearly") { PaywallYearlyPreview() }

    @Test
    fun monthly() = capture("paywall_monthly") { PaywallMonthlyPreview() }

    @Test
    fun lifetime() = capture("paywall_lifetime") { PaywallLifetimePreview() }

    @Test
    fun yearlyWithoutATrial() = capture("paywall_yearly_no_trial") { PaywallYearlyNoTrialPreview() }

    @Test
    fun longPrices() = capture("paywall_long_prices") { PaywallLongPricesPreview() }

    @Test
    fun loading() = capture("paywall_loading") { PaywallLoadingPreview() }

    @Test
    fun problem() = capture("paywall_problem") { PaywallProblemPreview() }

    @Test
    fun notice() = capture("paywall_notice") { PaywallNoticePreview() }

    @Test
    fun longName() = capture("paywall_long_name") { PaywallLongNamePreview() }

    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    @Test
    fun smallPhone() = capture("paywall_small_phone") { PaywallSmallPhonePreview() }

    @Config(qualifiers = "w360dp-h640dp-xxhdpi", fontScale = 1.3f)
    @Test
    fun smallPhoneLargeFont() = capture("paywall_small_font_1_3") { PaywallSmallLargeFontPreview() }

    @Config(qualifiers = "w360dp-h640dp-xxhdpi", fontScale = 2.0f)
    @Test
    fun smallPhoneHugeFont() = capture("paywall_small_font_2_0") { PaywallSmallHugeFontPreview() }
}
