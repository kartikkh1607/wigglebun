package com.kartikkhandelwal.wigglebun.ui.collection

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.kartikkhandelwal.wigglebun.domain.model.ActivePet
import com.kartikkhandelwal.wigglebun.domain.model.PetId
import com.kartikkhandelwal.wigglebun.ui.pet.TestPetNames
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Collection tab (M8) at the mockup's 390×844, plus a small phone with large fonts.
 * Reference PNGs: app/src/test/screenshots/collection_*.png.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w390dp-h844dp-xxhdpi", application = Application::class)
class CollectionScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val states = CollectionPreviewStates

    private fun capture(name: String, content: @Composable () -> Unit) {
        composeRule.setContent {
            // Inspection mode: still pets, so frames are stable.
            CompositionLocalProvider(LocalInspectionMode provides true) { content() }
        }
        composeRule.onRoot().captureRoboImage("collection_$name.png")
    }

    private fun grid(state: CollectionUiState): @Composable () -> Unit = { WigglebunTheme { CollectionScreen(state) } }

    private fun sheet(state: CollectionUiState): @Composable () -> Unit = { CollectionWithSheetPreview(state) }

    @Test fun onlyBun() = capture("only_bun", grid(states.OnlyBun))

    @Test fun oneAffordable() = capture("one_affordable", grid(states.OneAffordable))

    @Test fun allOwned() = capture("all_owned", grid(states.AllOwned))

    @Test fun sheetLockedAffordable() = capture("sheet_locked_affordable", sheet(states.sheet(states.OneAffordable, PetId.PIP)))

    @Test fun sheetLockedShort() = capture("sheet_locked_short", sheet(states.sheet(states.OneAffordable, PetId.NIMBUS)))

    @Test fun sheetOwned() = capture("sheet_owned", sheet(states.sheet(states.AllOwned, PetId.PIP)))

    @Test fun sheetActive() = capture("sheet_active", sheet(states.sheet(states.OnlyBun, PetId.BUN)))

    @Test fun sheetConfirm() = capture("sheet_confirm", sheet(states.sheet(states.OneAffordable, PetId.PIP, SheetStep.CONFIRM)))

    /** Just adopted: Pip is with you, celebrating in the sheet (the still frame shows the confetti). */
    @Test
    fun adopted() = capture(
        "adopted",
        sheet(
            toCollectionUiState(
                owned = setOf(PetId.BUN, PetId.PIP),
                active = ActivePet(PetId.PIP, chosenName = null),
                sparks = 10,
                selection = SheetSelection(PetId.PIP, SheetStep.ADOPTED),
            ),
        ),
    )

    /** The real Material bottom sheet in its own window, over the grid. */
    @OptIn(ExperimentalRoborazziApi::class)
    @Test
    fun realSheet() {
        val state = states.sheet(states.OneAffordable, PetId.TOAST)
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                WigglebunTheme {
                    CollectionScreen(state)
                    PetSheet(onDismiss = {}) { PetSheetContent(state.sheet!!, state.sparks, state.chosenName, PetSheetActions()) }
                }
            }
        }
        composeRule.waitForIdle()
        captureScreenRoboImage("collection_sheet_real.png")
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xhdpi", fontScale = 1.3f)
    fun smallPhoneFontScale130() = capture("small_font", grid(states.OneAffordable))

    @Test
    @Config(qualifiers = "w360dp-h640dp-xhdpi", fontScale = 1.3f)
    fun smallPhoneFontScale130Sheet() = capture("small_font_sheet", sheet(states.sheet(states.OneAffordable, PetId.NIMBUS)))

    /**
     * Bun named with the widest allowed name, owned but not with you: the card name and sheet title
     * stay on one line, "Choose …" shortens only the name.
     */
    @Test
    fun longNameSheet() = capture(
        "long_name_sheet",
        sheet(
            toCollectionUiState(
                owned = PetId.entries.toSet(),
                active = ActivePet(PetId.TOAST, chosenName = TestPetNames.WIDEST),
                sparks = 40,
                selection = SheetSelection(PetId.BUN, SheetStep.DETAILS),
            ),
        ),
    )
}
