package com.kartikkhandelwal.wigglebun.ui.paywall

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kartikkhandelwal.wigglebun.billing.PlanKind
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** What the paywall must say and do for each plan (CLAUDE.md §5.5). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w390dp-h844dp-xxhdpi", application = Application::class)
class PaywallScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()

    private fun show(state: PaywallUiState) {
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                WigglebunTheme {
                    PaywallScreen(
                        state = state,
                        petName = "Bun",
                        actions = PaywallActions(
                            onClose = { calls += "close" },
                            onRestore = { calls += "restore" },
                            onPick = { calls += "pick $it" },
                            onPurchase = { calls += "purchase" },
                            onRetry = { calls += "retry" },
                            onOpenTerms = { calls += "terms" },
                            onOpenPrivacy = { calls += "privacy" },
                        ),
                    )
                }
            }
        }
    }

    private fun text(text: String) = composeRule.onNodeWithText(text)

    @Test
    fun yearlyShowsTheTrialItsPriceAfterAndHowToCancel() {
        show(PaywallPreviewStates.yearly)
        text("Yearly").assertIsSelected()
        text("7 days free").assertExists()
        text("Day 6").assertExists()
        text("Day 8").assertExists()
        text(
            "7 days free, then \$29.99 a year. We remind you 2 days before it ends. " +
                "Cancel anytime in Google Play › Subscriptions.",
        ).assertExists()
        text("Start 7-day free trial").assertIsEnabled().performClick()
        assertEquals(listOf("purchase"), calls)
    }

    /** No free phase in the store's offer: nothing on screen may promise a trial. */
    @Test
    fun yearlyWithoutAFreePhaseNeverSaysFreeTrial() {
        show(PaywallPreviewStates.yearlyNoTrial)
        text("7 days free").assertDoesNotExist()
        text("Day 6").assertDoesNotExist()
        text("Start 7-day free trial").assertDoesNotExist()
        text("Continue with yearly").assertIsEnabled()
        text("\$29.99 a year, starting today. Cancel anytime in Google Play › Subscriptions.").assertExists()
    }

    @Test
    fun monthlyAndLifetimeSayWhatTheyCharge() {
        show(PaywallPreviewStates.monthly)
        text("Continue with monthly").assertIsEnabled()
        text("\$4.99 a month, starting today. Cancel anytime in Google Play › Subscriptions.").assertExists()
        // The timeline belongs to the trial, which monthly doesn't have.
        text("Day 8").assertDoesNotExist()
    }

    @Test
    fun lifetimeIsPaidOnce() {
        show(PaywallPreviewStates.lifetime)
        text("Get lifetime access").assertIsEnabled()
        text("\$59.99 once. No subscription, nothing to cancel.").assertExists()
    }

    @Test
    fun everyControlReportsItsTap() {
        show(PaywallPreviewStates.yearly)
        text("Monthly").performClick()
        text("Restore").performClick()
        text("Terms").performClick()
        text("Privacy").performClick()
        composeRule.onNodeWithContentDescription("Close").performClick()
        assertEquals(listOf("pick ${PlanKind.MONTHLY}", "restore", "terms", "privacy", "close"), calls)
    }

    @Test
    fun whileLoadingThereIsNothingToBuy() {
        show(PaywallPreviewStates.loading)
        text("Loading plans…").assertIsNotEnabled()
    }

    @Test
    fun withNoPlansTryAgainIsTheWayOnAndCloseStillWorks() {
        show(PaywallPreviewStates.problem)
        text("Plans can’t be loaded right now. Check your connection and try again.").assertExists()
        text("Loading plans…").assertDoesNotExist()
        text("Try again").performClick()
        composeRule.onNodeWithContentDescription("Close").performClick()
        assertEquals(listOf("retry", "close"), calls)
    }

    @Test
    fun aRunningPurchaseBlocksASecondOne() {
        show(PaywallPreviewStates.busy)
        text("Start 7-day free trial").assertIsNotEnabled()
    }

    @Test
    fun aFailedTryShowsItsNotice() {
        show(PaywallPreviewStates.notice)
        text("No connection. Check your internet and try again.").assertExists()
    }
}
