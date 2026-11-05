package com.kartikkhandelwal.wigglebun.ui.setup

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kartikkhandelwal.wigglebun.ui.settings.SettingsActions
import com.kartikkhandelwal.wigglebun.ui.settings.SettingsPreviewStates
import com.kartikkhandelwal.wigglebun.ui.settings.SettingsScreen
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** CLAUDE.md §10: the explanation is shown before the system dialog, and declining changes nothing. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w390dp-h844dp-xxhdpi", application = Application::class)
class SetupFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun hasClickLabel(label: String) = SemanticsMatcher("click label $label") {
        it.config.getOrElseNullable(SemanticsActions.OnClick) { null }?.label == label
    }

    private val performed = mutableListOf<SetupCardUi>()

    private fun start() {
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                WigglebunTheme {
                    SetupCardsSheetHost(SettingsPreviewStates.AllOff, perform = { performed += it }) { open ->
                        SettingsScreen(SettingsPreviewStates.Default, SettingsPreviewStates.AllOff, SettingsActions(onOpenSetup = open))
                    }
                }
            }
        }
    }

    @Test
    fun tappingACardExplainsFirstAndAsksOnlyOnContinue() {
        start()
        composeRule.onNode(hasText("Notifications") and hasClickAction()).performClick()
        composeRule.onNode(hasText("So your pet can remind you when", substring = true)).assertIsDisplayed()
        assertTrue(performed.isEmpty())

        composeRule.onNode(hasText("Continue") and hasClickAction()).performClick()
        composeRule.waitForIdle()
        assertEquals(listOf(SettingsPreviewStates.Notifications), performed)
    }

    /**
     * Rows keep their short name in every state (design/ui_v2/Settings.dc.html): one that's on
     * shows a chip and can't be tapped; one refused in the phone's settings says so and still
     * leads on ("Turn on", through the explanation).
     */
    @Test
    fun rowsShowTheirStateOnTheRight() {
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                WigglebunTheme { SettingsScreen(SettingsPreviewStates.Default, SettingsPreviewStates.Mixed, SettingsActions()) }
            }
        }
        composeRule.onNode(hasText("Notifications") and hasText("On")).assertIsDisplayed()
        composeRule.onNode(hasText("Notifications") and hasClickAction()).assertDoesNotExist()
        composeRule.onNode(hasText("Widget") and hasText("Added")).assertIsDisplayed()
        composeRule.onNode(hasText("Movement detection") and hasText("Turned off in your phone’s settings") and hasClickAction())
            .assertIsDisplayed()
        composeRule.onNodeWithText("Enable notifications").assertDoesNotExist()
        composeRule.onNodeWithText("Enable movement detection").assertDoesNotExist()
        composeRule.onNodeWithText("Add the widget").assertDoesNotExist()
    }

    /** An off row's action is "Turn on" ("Add" for the widget), read by TalkBack as the row's. */
    @Test
    fun offRowsSayWhatATapDoes() {
        start()
        composeRule.onNode(hasText("Notifications") and hasClickAction()).assert(hasClickLabel("Turn on"))
        composeRule.onNode(hasText("Widget") and hasClickAction()).assert(hasClickLabel("Add"))
    }

    @Test
    fun notNowInTheExplanationAsksNothing() {
        start()
        composeRule.onNode(hasText("Movement detection") and hasClickAction()).performClick()
        composeRule.onNode(hasText("Not now") and hasClickAction()).performClick()
        composeRule.waitForIdle()
        assertTrue(performed.isEmpty())
    }
}
