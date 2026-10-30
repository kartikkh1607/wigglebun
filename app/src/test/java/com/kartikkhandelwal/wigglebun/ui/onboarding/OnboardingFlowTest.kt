package com.kartikkhandelwal.wigglebun.ui.onboarding

import android.app.Application
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.hasSetTextAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kartikkhandelwal.wigglebun.domain.sitting.SittingSettings
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/** Walks the steps with real navigation. Inspection mode keeps the pet still, so Compose goes idle. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w390dp-h844dp-xxhdpi", application = Application::class)
class OnboardingFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val saved = mutableListOf<String>()
    private var finished = 0
    private var backDispatcher: OnBackPressedDispatcher? = null

    private fun start() {
        var draft by mutableStateOf("")
        var hours by mutableStateOf(WorkHoursDraft())
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                WigglebunTheme {
                    OnboardingFlow(
                        nameDraft = draft,
                        workHours = hours,
                        intervalMinutes = SittingSettings.DEFAULT_INTERVAL_MINUTES,
                        actions = OnboardingActions(
                            onNameChange = { draft = it },
                            onSaveName = { saved += "name" },
                            onToggleDay = { hours = hours.toggled(it) },
                            onSaveWorkHours = { saved += "hours" },
                            onSaveInterval = { saved += "interval" },
                            onFinish = { finished++ },
                        ),
                        sessionContent = { onExit, onCompleted ->
                            Column {
                                Text("session")
                                Button(onClick = onExit) { Text("close session") }
                                Button(onClick = onCompleted) { Text("session done") }
                            }
                        },
                    )
                    backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
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
        composeRule.onNodeWithText("Hi! I’m Bun.\nThat was some nap!").assertIsDisplayed()

        tap("Nice to meet you")
        composeRule.onNodeWithText("What will you call me?").assertIsDisplayed()

        composeRule.onNode(hasSetTextAction()).performTextInput("Biscuit")
        composeRule.onNodeWithText("7/12").assertIsDisplayed()
        tap("Continue")

        composeRule.onNodeWithText("When do you work?").assertIsDisplayed()
        tap("Continue")

        composeRule.onNodeWithText("How often should I nudge you?").assertIsDisplayed()
        tap("Continue")

        composeRule.onNodeWithText("Stretch with Biscuit").assertIsDisplayed()
        tap("Start stretching")
        composeRule.onNodeWithText("session").assertIsDisplayed()
        tap("session done")

        composeRule.onNodeWithText("You’re all set!").assertIsDisplayed()
        assertEquals(0, finished) // not until the paywall is left behind
        tap("Continue")

        assertEquals(listOf("name", "hours", "interval"), saved)
        assertEquals(1, finished)
    }

    /** Closing the session earns nothing and goes back to the offer, not on to the paywall. */
    @Test
    fun closingTheStretchGoesBackToTheOffer() {
        toTheStretchOffer()
        tap("Start stretching")
        tap("close session")
        composeRule.onNodeWithText("Start stretching").assertIsDisplayed()
        composeRule.onNodeWithText("You’re all set!").assertDoesNotExist()
    }

    /** Leaving the paywall with Back finishes onboarding, as closing the real one will (§5.5). */
    @Test
    fun backOnThePaywallFinishes() {
        toTheStretchOffer()
        tap("Skip")
        composeRule.waitForIdle() // the paywall has to be on screen for its Back to be registered
        composeRule.runOnUiThread { backDispatcher!!.onBackPressed() }
        composeRule.waitForIdle()
        assertEquals(1, finished)
    }

    private fun toTheStretchOffer() {
        start()
        repeat(4) {
            tap("Skip")
            composeRule.waitForIdle()
        }
    }

    /** No work days means the pet would never wake, so the answer has to be fixed or skipped. */
    @Test
    fun noWorkDaysBlocksContinueButNotSkip() {
        start()
        tap("Skip")
        tap("Skip")
        DayOfWeek.entries.take(5).forEach { day ->
            composeRule.onNodeWithContentDescription(day.getDisplayName(TextStyle.FULL, Locale.getDefault())).performClick()
        }
        composeRule.onNodeWithText("Pick at least one work day.").assertIsDisplayed()
        composeRule.onNodeWithText("Continue").assertIsNotEnabled()
        tap("Skip")
        composeRule.onNodeWithText("How often should I nudge you?").assertIsDisplayed()
    }

    /** "Skip" is on every step and keeps the default: nothing is saved. */
    @Test
    fun skippingEveryStepSavesNothing() {
        start()
        tap("Skip")
        composeRule.onNodeWithText("What will you call me?").assertIsDisplayed()
        tap("Skip")
        composeRule.onNodeWithText("When do you work?").assertIsDisplayed()
        tap("Skip")
        composeRule.onNodeWithText("How often should I nudge you?").assertIsDisplayed()
        tap("Skip")
        composeRule.onNodeWithText("Stretch with Bun").assertIsDisplayed()
        tap("Skip")
        composeRule.onNodeWithText("You’re all set!").assertIsDisplayed()
        tap("Continue")
        assertTrue(saved.isEmpty())
        assertEquals(1, finished)
    }
}
