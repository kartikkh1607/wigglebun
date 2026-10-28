package com.kartikkhandelwal.wigglebun.ui.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.domain.model.ActivePet
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.domain.model.PetId
import com.kartikkhandelwal.wigglebun.ui.components.ChunkySurface
import com.kartikkhandelwal.wigglebun.ui.components.SparkIcon
import com.kartikkhandelwal.wigglebun.ui.components.SparksPill
import com.kartikkhandelwal.wigglebun.ui.pet.LockedPetView
import com.kartikkhandelwal.wigglebun.ui.pet.PetView
import com.kartikkhandelwal.wigglebun.ui.pet.displayName
import com.kartikkhandelwal.wigglebun.ui.pet.spec
import com.kartikkhandelwal.wigglebun.ui.theme.Card
import com.kartikkhandelwal.wigglebun.ui.theme.CardEdge
import com.kartikkhandelwal.wigglebun.ui.theme.Cream
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.InkMuted
import com.kartikkhandelwal.wigglebun.ui.theme.MoodHappyChip
import com.kartikkhandelwal.wigglebun.ui.theme.MoodTiredChip
import com.kartikkhandelwal.wigglebun.ui.theme.MovementChipText
import com.kartikkhandelwal.wigglebun.ui.theme.Track
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunShapes
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTextStyles
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme

private val C = Dimens.Collection

@Composable
fun CollectionScreenRoute(modifier: Modifier = Modifier, viewModel: CollectionViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val loaded = state
    if (loaded == null) {
        Box(modifier.fillMaxSize().background(Cream))
        return
    }
    CollectionScreen(loaded, onOpenPet = viewModel::openPet, modifier = modifier)
    loaded.sheet?.let { sheet ->
        PetSheet(onDismiss = viewModel::closeSheet) {
            PetSheetContent(
                sheet = sheet,
                sparks = loaded.sparks,
                chosenName = loaded.chosenName,
                actions = PetSheetActions(
                    onChoose = viewModel::choose,
                    onAskToAdopt = viewModel::askToAdopt,
                    onConfirmAdopt = viewModel::confirmAdopt,
                    onCancelAdopt = viewModel::cancelAdopt,
                    onClose = viewModel::closeSheet,
                ),
            )
        }
    }
}

/**
 * Collection tab (CLAUDE.md §6.5, design/mockups_source/Collection.dc.html): header with the
 * sparks pill, then every pet in a two-column grid. Tapping a pet opens its sheet.
 */
@Composable
fun CollectionScreen(state: CollectionUiState, modifier: Modifier = Modifier, onOpenPet: (PetId) -> Unit = {}) {
    Column(
        modifier
            .fillMaxSize()
            .background(Cream)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = C.HeaderPaddingHorizontal, end = C.HeaderPaddingHorizontal, top = C.HeaderPaddingTop, bottom = C.HeaderPaddingBottom),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.tab_collection),
                style = WigglebunTextStyles.ScreenTitle,
                color = Ink,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            SparksPill(state.sparks)
        }
        Text(
            stringResource(R.string.collection_subtitle),
            style = WigglebunTextStyles.FocusSub,
            color = InkMuted,
            modifier = Modifier.padding(start = C.HeaderPaddingHorizontal, end = C.HeaderPaddingHorizontal, bottom = C.SubtitlePaddingBottom),
        )
        Column(
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(C.GridGap),
        ) {
            state.pets.chunked(2).forEach { row ->
                // Same height for both cards of a row, whatever the name and pill wrap to.
                Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(C.GridGap)) {
                    row.forEach { pet ->
                        PetCard(
                            pet = pet,
                            chosenName = state.chosenName,
                            onClick = { onOpenPet(pet.id) },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(C.ListBottomPadding))
    }
}

/**
 * One pet: in colour when owned (the active one framed in Ink, as in the mockup), a grey
 * silhouette with its price when locked. Pets stand still here; the sheet animates them.
 */
@Composable
private fun PetCard(pet: CollectionPetUi, chosenName: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val active = pet.asActivePet(chosenName)
    val name = active.displayName()
    val description = when (pet.status) {
        PetStatus.ACTIVE -> stringResource(R.string.collection_cd_active, name)
        PetStatus.OWNED -> stringResource(R.string.collection_cd_owned, name)
        PetStatus.LOCKED -> pluralStringResource(R.plurals.collection_cd_locked, pet.price, name, pet.price)
    }
    val clickLabel = stringResource(R.string.collection_open_pet)
    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = C.CardPaddingHorizontal, end = C.CardPaddingHorizontal, top = C.CardPaddingTop, bottom = C.CardPaddingBottom)
                // One TalkBack stop per card: "Pip, adopt for 150 sparks".
                .clearAndSetSemantics { contentDescription = description },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(C.CardGap),
        ) {
            if (pet.status == PetStatus.LOCKED) {
                LockedPetView(active.spec, description = name, modifier = Modifier.size(C.PetSize))
            } else {
                PetView(energy = 1f, mood = Mood.HAPPY, spec = active.spec, name = name, animated = false, modifier = Modifier.size(C.PetSize))
            }
            // One line, so a long name never makes this row of cards taller.
            Text(
                name,
                style = WigglebunTextStyles.PetCardName,
                color = Ink,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            StatusPill(pet)
        }
    }
    if (pet.status == PetStatus.ACTIVE) {
        // Collection.dc.html: the pet that is with you has a 3 dp Ink frame and no edge. The bottom
        // padding keeps its face level with the chunky cards beside it.
        Box(
            modifier
                .padding(bottom = C.CardEdge)
                .clip(WigglebunShapes.Card)
                .background(Card)
                .border(C.ActiveBorder, Ink, WigglebunShapes.Card)
                .clickable(onClickLabel = clickLabel, role = Role.Button, onClick = onClick),
        ) { content() }
    } else {
        ChunkySurface(
            shape = WigglebunShapes.Card,
            faceColor = Card,
            edgeColor = CardEdge,
            edgeHeight = C.CardEdge,
            onClick = onClick,
            onClickLabel = clickLabel,
            modifier = modifier,
        ) { content() }
    }
}

@Composable
private fun StatusPill(pet: CollectionPetUi) {
    val (background, textColor) = when (pet.status) {
        PetStatus.ACTIVE -> MoodHappyChip to MovementChipText
        PetStatus.OWNED -> Track to Ink
        PetStatus.LOCKED -> MoodTiredChip to Ink
    }
    Row(
        modifier = Modifier
            .clip(WigglebunShapes.Pill)
            .background(background)
            .padding(horizontal = C.PillPaddingHorizontal, vertical = C.PillPaddingVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(C.PillIconGap),
    ) {
        if (pet.status == PetStatus.LOCKED) SparkIcon(C.PillIcon)
        val text = when (pet.status) {
            PetStatus.ACTIVE -> stringResource(R.string.collection_with_you)
            PetStatus.OWNED -> stringResource(R.string.collection_owned)
            PetStatus.LOCKED -> pet.price.toString()
        }
        Text(text, style = WigglebunTextStyles.Chip, color = textColor)
    }
}

/** Sample states for previews and screenshot tests, built through the real mapper. */
internal object CollectionPreviewStates {
    /** Fresh install: only Bun, 120 sparks (nothing affordable yet). */
    val OnlyBun = toCollectionUiState(setOf(PetId.BUN), ActivePet.Default, sparks = 120, selection = null)

    /** 160 sparks: Pip (150) is affordable, Toast and Nimbus are not. */
    val OneAffordable = toCollectionUiState(setOf(PetId.BUN), ActivePet.Default, sparks = 160, selection = null)

    /** Every pet adopted, Toast with you. */
    val AllOwned = toCollectionUiState(PetId.entries.toSet(), ActivePet(PetId.TOAST, null), sparks = 40, selection = null)

    fun sheet(state: CollectionUiState, pet: PetId, step: SheetStep = SheetStep.DETAILS): CollectionUiState =
        toCollectionUiState(
            owned = state.pets.filter { it.status != PetStatus.LOCKED }.map { it.id }.toSet(),
            active = ActivePet(state.pets.first { it.status == PetStatus.ACTIVE }.id, state.chosenName),
            sparks = state.sparks,
            selection = SheetSelection(pet, step),
        )
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun CollectionOnlyBunPreview() {
    WigglebunTheme { CollectionScreen(CollectionPreviewStates.OnlyBun) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun CollectionOneAffordablePreview() {
    WigglebunTheme { CollectionScreen(CollectionPreviewStates.OneAffordable) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun CollectionAllOwnedPreview() {
    WigglebunTheme { CollectionScreen(CollectionPreviewStates.AllOwned) }
}
