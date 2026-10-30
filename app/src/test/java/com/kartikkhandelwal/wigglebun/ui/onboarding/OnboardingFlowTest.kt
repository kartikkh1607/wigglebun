package com.kartikkhandelwal.wigglebun.ui.onboarding

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.hasSetTextAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Walks the steps with real navigation. Inspection mode keeps the pet still, so Compose goes idle. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w390dp-h844dp-xxhdpi", application = Application::class)
class OnboardingFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var saved = 0
    private var finished = 0

    private fun start() {
        var draft by mutableStateOf("")
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                WigglebunTheme {
                    OnboardingFlow(
                        nameDraft = draft,
                        onNameChange = { draft = it },
                        onSaveName = { saved++ },
                        onFinish = { finished++ },
                    )
                }
            }
        }
    }

    private fun tap(text: String) = composeRule.onNodeWithText(text).performClick()

    @Test
    fun wakingThePetThenNamingIt() {
        start()
        composeRule.onNodeWithText("Tap to wake them up").assertIsDisplayed()

        tap("Tap to wake them up")
        composeRule.onNodeWithText("Hi!\nThat was some nap!").assertIsDisplayed()

        tap("Nice to meet you")
        composeRule.onNodeWithText("What will you call me?").assertIsDisplayed()

        composeRule.onNode(hasSetTextAction()).performTextInput("Biscuit")
        composeRule.onNodeWithText("7/12").assertIsDisplayed()
        tap("Continue")
        assertEquals(1, saved)
        assertEquals(1, finished)
    }

    /** "Skip" is on every step and keeps the default: nothing is saved. */
    @Test
    fun skippingEveryStepSavesNothing() {
        start()
        tap("Skip")
        composeRule.onNodeWithText("What will you call me?").assertIsDisplayed()
        tap("Skip")
        assertEquals(0, saved)
        assertTrue(finished == 1)
    }
}
