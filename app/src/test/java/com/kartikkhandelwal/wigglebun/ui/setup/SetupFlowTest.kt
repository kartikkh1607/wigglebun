package com.kartikkhandelwal.wigglebun.ui.setup

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
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

    private val performed = mutableListOf<SetupCardUi>()

    private fun start() {
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                WigglebunTheme {
                    SetupCardsSheetHost(SettingsPreviewStates.AllOff, perform = { performed += it }) { open ->
                        SettingsScreen(SettingsPreviewStates.AllOff, open)
                    }
                }
            }
        }
    }

    @Test
    fun tappingACardExplainsFirstAndAsksOnlyOnContinue() {
        start()
        composeRule.onNodeWithText("Enable notifications").performClick()
        composeRule.onNode(hasText("So your pet can remind you when", substring = true)).assertIsDisplayed()
        assertTrue(performed.isEmpty())

        composeRule.onNode(hasText("Continue") and hasClickAction()).performClick()
        composeRule.waitForIdle()
        assertEquals(listOf(SettingsPreviewStates.Notifications), performed)
    }

    /** Once it's on there's nothing left to enable; a refused one still needs enabling. */
    @Test
    fun cardsThatAreOnDropTheWordEnable() {
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                WigglebunTheme { SettingsScreen(SettingsPreviewStates.Mixed, {}) }
            }
        }
        composeRule.onNodeWithText("Notifications").assertIsDisplayed()
        composeRule.onNodeWithText("Enable notifications").assertDoesNotExist()
        composeRule.onNodeWithText("Widget").assertIsDisplayed()
        composeRule.onNodeWithText("Add the widget").assertDoesNotExist()
        composeRule.onNodeWithText("Enable movement detection").assertIsDisplayed() // refused, so still off
    }

    @Test
    fun notNowInTheExplanationAsksNothing() {
        start()
        composeRule.onNodeWithText("Enable movement detection").performClick()
        composeRule.onNode(hasText("Not now") and hasClickAction()).performClick()
        composeRule.waitForIdle()
        assertTrue(performed.isEmpty())
    }
}
