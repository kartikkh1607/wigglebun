package com.kartikkhandelwal.wigglebun.ui.collection

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.domain.model.PetId
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButton
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButtonStyle
import com.kartikkhandelwal.wigglebun.ui.components.SparkIcon
import com.kartikkhandelwal.wigglebun.ui.pet.PetNameSentence
import com.kartikkhandelwal.wigglebun.ui.pet.PetView
import com.kartikkhandelwal.wigglebun.ui.pet.displayName
import com.kartikkhandelwal.wigglebun.ui.pet.spec
import com.kartikkhandelwal.wigglebun.ui.theme.Card
import com.kartikkhandelwal.wigglebun.ui.theme.CheckIdle
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.InkMuted
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunShapes
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme

private val C = Dimens.Collection

/** What the sheet's buttons do; each gets the pet the sheet is about. */
class PetSheetActions(
    val onChoose: (PetId) -> Unit = {},
    val onAskToAdopt: (PetId) -> Unit = {},
    val onConfirmAdopt: (PetId) -> Unit = {},
    val onCancelAdopt: (PetId) -> Unit = {},
    val onClose: () -> Unit = {},
)

/** The Material bottom sheet (28 dp top corners, CLAUDE.md §11); drag down or tap outside to close. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = WigglebunShapes.SheetTop,
        containerColor = Card,
        contentColor = Ink,
        dragHandle = { BottomSheetDefaults.DragHandle(color = CheckIdle) },
    ) { content() }
}

/**
 * One pet up close: the pet moving in HAPPY, its name and line, and one button that says what
 * can happen (choose, already with you, adopt, or how many sparks are missing). Adopting asks
 * first, then welcomes the pet with a celebration.
 */
@Composable
fun PetSheetContent(
    sheet: PetSheetUi,
    sparks: Int,
    chosenName: String?,
    actions: PetSheetActions,
    modifier: Modifier = Modifier,
) {
    val pet = sheet.pet.asActivePet(chosenName)
    val name = pet.displayName()
    val id = sheet.pet.id
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = C.SheetPaddingHorizontal, end = C.SheetPaddingHorizontal, bottom = C.SheetPaddingBottom),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(C.SheetGap),
    ) {
        when (sheet.step) {
            SheetStep.DETAILS -> {
                PetView(energy = 1f, mood = Mood.HAPPY, spec = pet.spec, name = name, accessories = pet.wearing, modifier = Modifier.size(C.SheetPetSize))
                SheetTitle(name)
                SheetBody(stringResource(pet.spec.blurb))
                MainAction(sheet.action, name, enabled = !sheet.busy, onChoose = { actions.onChoose(id) }, onAdopt = { actions.onAskToAdopt(id) })
            }
            SheetStep.CONFIRM -> {
                PetView(energy = 1f, mood = Mood.HAPPY, spec = pet.spec, name = name, accessories = pet.wearing, modifier = Modifier.size(C.SheetSmallPetSize))
                SheetTitleSentence(R.string.adopt_confirm_title, name)
                SheetBody(pluralStringResource(R.plurals.adopt_confirm_body, sheet.pet.price, sheet.pet.price, sparks))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = C.SheetButtonTop),
                    horizontalArrangement = Arrangement.spacedBy(C.GridGap),
                ) {
                    ChunkyButton(
                        text = stringResource(R.string.adopt_confirm_no),
                        onClick = { actions.onCancelAdopt(id) },
                        style = ChunkyButtonStyle.Secondary,
                        enabled = !sheet.busy,
                        modifier = Modifier.weight(1f),
                    )
                    ChunkyButton(
                        text = stringResource(R.string.adopt_confirm_yes),
                        onClick = { actions.onConfirmAdopt(id) },
                        style = ChunkyButtonStyle.Done,
                        enabled = !sheet.busy,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            SheetStep.ADOPTED -> {
                // Jumps once with confetti as it appears (PetView's CELEBRATE).
                PetView(energy = 1f, mood = Mood.CELEBRATE, spec = pet.spec, name = name, accessories = pet.wearing, modifier = Modifier.size(C.SheetPetSize))
                Column(
                    // Announced once when the welcome appears.
                    modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(C.SheetGap),
                ) {
                    SheetTitleSentence(R.string.adopted_title, name)
                    PetNameSentence(
                        R.string.adopted_body,
                        name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = InkMuted,
                        textAlign = TextAlign.Center,
                    )
                }
                ChunkyButton(
                    text = stringResource(R.string.adopted_done),
                    onClick = actions.onClose,
                    modifier = Modifier.fillMaxWidth().padding(top = C.SheetButtonTop),
                )
            }
        }
    }
}

/** The pet's name alone: one line, cut with "…" if it must be. */
@Composable
private fun SheetTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.headlineMedium,
        color = Ink,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** A title with the name in it ("Adopt Pip?"): only the name is ever shortened. */
@Composable
private fun SheetTitleSentence(@StringRes template: Int, name: String) {
    PetNameSentence(template, name, style = MaterialTheme.typography.headlineMedium, color = Ink, textAlign = TextAlign.Center)
}

@Composable
private fun SheetBody(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = InkMuted, textAlign = TextAlign.Center)
}

@Composable
private fun MainAction(action: SheetAction, name: String, enabled: Boolean, onChoose: () -> Unit, onAdopt: () -> Unit) {
    val modifier = Modifier.fillMaxWidth().padding(top = C.SheetButtonTop)
    when (action) {
        // One line in the button: a long name is shortened ("Choose Wigglebo…").
        SheetAction.Choose -> ChunkyButton(onClick = onChoose, enabled = enabled, modifier = modifier) {
            PetNameSentence(R.string.sheet_choose, name, maxLines = 1)
        }
        SheetAction.WithYou -> ChunkyButton(
            text = stringResource(R.string.sheet_with_you),
            onClick = {},
            style = ChunkyButtonStyle.Inactive,
            enabled = false,
            modifier = modifier,
        )
        is SheetAction.Adopt -> {
            val description = pluralStringResource(R.plurals.sheet_adopt_cd, action.price, action.price)
            ChunkyButton(
                onClick = onAdopt,
                style = ChunkyButtonStyle.Highlight,
                enabled = enabled,
                modifier = modifier.semantics { contentDescription = description },
            ) {
                Text(stringResource(R.string.sheet_adopt, action.price), maxLines = 1, modifier = Modifier.clearAndSetSemantics {})
                // In Ink like the label: the yellow star would vanish on the Sun button.
                Box(Modifier.padding(start = C.SheetButtonIconGap)) { SparkIcon(C.SheetButtonIcon, tint = Ink) }
            }
        }
        is SheetAction.NeedMore -> ChunkyButton(
            text = pluralStringResource(R.plurals.sheet_more_sparks, action.missing, action.missing),
            onClick = {},
            style = ChunkyButtonStyle.Inactive,
            enabled = false,
            modifier = modifier,
        )
    }
}

/**
 * The sheet as it looks open over the Collection: a still frame for previews and screenshot tests
 * (the real [ModalBottomSheet] animates in from its own window).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CollectionWithSheetPreview(state: CollectionUiState) {
    WigglebunTheme {
        Box(Modifier.fillMaxSize()) {
            CollectionScreen(state)
            Box(Modifier.fillMaxSize().background(BottomSheetDefaults.ScrimColor))
            val accessorySheet = state.accessorySheet
            val sheet = state.sheet
            if (sheet == null && accessorySheet == null) return@Box
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clip(WigglebunShapes.SheetTop)
                    .background(Card),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BottomSheetDefaults.DragHandle(color = CheckIdle)
                if (sheet != null) {
                    PetSheetContent(sheet, state.sparks, state.chosenName, PetSheetActions())
                } else if (accessorySheet != null) {
                    AccessorySheetContent(accessorySheet, state.sparks, AccessorySheetActions())
                }
            }
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun SheetLockedAffordablePreview() =
    CollectionWithSheetPreview(CollectionPreviewStates.sheet(CollectionPreviewStates.OneAffordable, PetId.PIP))

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun SheetConfirmPreview() =
    CollectionWithSheetPreview(CollectionPreviewStates.sheet(CollectionPreviewStates.OneAffordable, PetId.PIP, SheetStep.CONFIRM))
