package com.kartikkhandelwal.wigglebun.ui.onboarding

import android.app.Application
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
class AppGateTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsNothingUntilDecidedThenTheRightSide() {
        var done by mutableStateOf<Boolean?>(null)
        composeRule.setContent {
            AppGate(done, onboarding = { Text("onboarding") }, app = { Text("home") })
        }
        composeRule.onNodeWithText("onboarding").assertDoesNotExist()
        composeRule.onNodeWithText("home").assertDoesNotExist()

        done = false
        composeRule.onNodeWithText("onboarding").assertIsDisplayed()
        composeRule.onNodeWithText("home").assertDoesNotExist()

        done = true
        composeRule.onNodeWithText("home").assertIsDisplayed()
        composeRule.onNodeWithText("onboarding").assertDoesNotExist()
    }

    @Test
    fun loading() {
        composeRule.setContent { AppGateLoadingPreview() }
        composeRule.onRoot().captureRoboImage("app_gate_loading.png")
    }
}
