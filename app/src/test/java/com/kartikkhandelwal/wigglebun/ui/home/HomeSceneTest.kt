package com.kartikkhandelwal.wigglebun.ui.home

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The speech bubble took over from the mood chip, so TalkBack must read it, with the pet. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w390dp-h844dp-xxhdpi", application = Application::class)
class HomeSceneTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(state: HomeUiState) = composeRule.setContent {
        CompositionLocalProvider(LocalInspectionMode provides true) { WigglebunTheme { HomeScreen(state) } }
    }

    @Test
    fun thePetAndItsBubbleAreOneItem() {
        show(HomePreviewStates.Tired)
        // The merged node that holds the bubble's words also describes the pet.
        val node = composeRule.onNodeWithText("Getting sleepy… stretch with me?").fetchSemanticsNode()
        val description = node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
        assertEquals(listOf("Bun, energy draining from sitting"), description)
    }

    @Test
    fun eachMoodHasItsLine() {
        mapOf(
            HomePreviewStates.Happy to "Feeling bouncy!",
            HomePreviewStates.Stiff to "So stiff… walk with me?",
            HomePreviewStates.OnWalk to "Ooh, a walk!",
            HomePreviewStates.Celebrate to "Ahh, much better!",
        ).forEach { (state, line) ->
            assertEquals(line, state.line?.let { lineText(it) })
        }
    }

    @Test
    fun noBubbleWhileAsleep() {
        show(HomePreviewStates.Asleep)
        listOf("Feeling bouncy!", "Getting sleepy… stretch with me?").forEach {
            assertEquals(0, composeRule.onAllNodesWithText(it).fetchSemanticsNodes().size)
        }
    }

    private fun lineText(line: PetLine): String = when (line) {
        PetLine.HAPPY -> "Feeling bouncy!"
        PetLine.TIRED -> "Getting sleepy… stretch with me?"
        PetLine.STIFF -> "So stiff… walk with me?"
        PetLine.WALK -> "Ooh, a walk!"
        PetLine.CELEBRATE -> "Ahh, much better!"
    }
}
