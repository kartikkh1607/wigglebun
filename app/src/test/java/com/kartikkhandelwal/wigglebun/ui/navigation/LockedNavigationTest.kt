package com.kartikkhandelwal.wigglebun.ui.navigation

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** What the tabs show with and without an entitlement (CLAUDE.md §5.5). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w390dp-h844dp-xxhdpi", application = Application::class)
class LockedNavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var breakRequestsHandled = 0

    private fun show(locked: Boolean, startBreak: BreakType? = null) {
        composeRule.setContent {
            // Inspection mode = still pet in the tab bar; a live animation would keep Compose busy forever.
            CompositionLocalProvider(LocalInspectionMode provides true) {
                WigglebunTheme {
                    WigglebunNavHost(
                        homeContent = { Text("open home") },
                        collectionContent = { Text("collection") },
                        petContent = { Text("pet page") },
                        settingsContent = { Text("settings") },
                        startBreak = startBreak,
                        onStartBreakHandled = { breakRequestsHandled++ },
                        locked = locked,
                        lockedHomeContent = { navigation ->
                            Column {
                                Text("locked home")
                                Button(onClick = navigation.onOpenPaywall) { Text("to the paywall") }
                                Button(onClick = navigation.onOpenMenu) { Text("to the menu") }
                            }
                        },
                        paywallContent = { petName, onDone -> Button(onClick = onDone) { Text("paywall for $petName") } },
                    )
                }
            }
        }
    }

    private fun text(text: String) = composeRule.onNodeWithText(text)

    @Test
    fun withAnEntitlementHomeIsHome() {
        show(locked = false)
        text("open home").assertIsDisplayed()
        text("locked home").assertDoesNotExist()
    }

    @Test
    fun lockedHomeOpensThePaywallAndComesBack() {
        show(locked = true)
        text("locked home").assertIsDisplayed()
        text("open home").assertDoesNotExist()

        text("to the paywall").performClick()
        text("paywall for Bun").assertIsDisplayed()
        // Closed or bought, the paywall returns to Home.
        text("paywall for Bun").performClick()
        text("locked home").assertIsDisplayed()
    }

    @Test
    fun theOtherTabsStayReachableWhileLocked() {
        show(locked = true)
        text("to the menu").performClick()
        text("settings").assertIsDisplayed()
        text("Collection").performClick()
        text("collection").assertIsDisplayed()
    }

    /** The widget's "Take a break": while locked the app just opens; the request is not kept for later. */
    @Test
    fun aBreakCannotBeStartedWhileLocked() {
        show(locked = true, startBreak = BreakType.STAND_UP)
        text("locked home").assertIsDisplayed()
        assertEquals(1, breakRequestsHandled)
    }
}
