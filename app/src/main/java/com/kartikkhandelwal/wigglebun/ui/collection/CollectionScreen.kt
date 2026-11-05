package com.kartikkhandelwal.wigglebun.ui.collection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.kartikkhandelwal.wigglebun.ui.theme.ActivePetHalo
import com.kartikkhandelwal.wigglebun.ui.theme.CollectionDecor
import com.kartikkhandelwal.wigglebun.ui.theme.CollectionDecorDot
import com.kartikkhandelwal.wigglebun.ui.theme.DoneGreen
import com.kartikkhandelwal.wigglebun.ui.theme.NextPetTile
import com.kartikkhandelwal.wigglebun.ui.theme.NextPetTrack
import com.kartikkhandelwal.wigglebun.ui.theme.StatusOnChip
import com.kartikkhandelwal.wigglebun.ui.theme.StatusOnText
import androidx.compose.foundation.background
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
import com.kartikkhandelwal.wigglebun.domain.model.AccessoryId
import com.kartikkhandelwal.wigglebun.domain.model.ActivePet
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.domain.model.Outfit
import com.kartikkhandelwal.wigglebun.domain.model.PetId
import com.kartikkhandelwal.wigglebun.ui.components.ChunkySurface
import com.kartikkhandelwal.wigglebun.ui.components.SparkIcon
import com.kartikkhandelwal.wigglebun.ui.components.SparksPill
import com.kartikkhandelwal.wigglebun.ui.pet.LockedPetView
import com.kartikkhandelwal.wigglebun.ui.pet.PetView
import com.kartikkhandelwal.wigglebun.ui.pet.displayName
import com.kartikkhandelwal.wigglebun.ui.pet.spec
import com.kartikkhandelwal.wigglebun.ui.theme.Card
import com.kartikkhandelwal.wigglebun.ui.theme.OnSurface
import com.kartikkhandelwal.wigglebun.ui.theme.Surfaces
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.InkMuted
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
        Box(modifier.fillMaxSize().background(Surfaces.Collection.background))
        return
    }
    CollectionScreen(
        loaded,
        onOpenPet = viewModel::openPet,
        onSelectTab = viewModel::selectTab,
        onOpenAccessory = viewModel::openAccessory,
        modifier = modifier,
    )
    loaded.accessorySheet?.let { sheet ->
        PetSheet(onDismiss = viewModel::closeAccessory) {
            AccessorySheetContent(
                sheet = sheet,
                sparks = loaded.sparks,
                actions = AccessorySheetActions(
                    onAskToBuy = viewModel::askToBuy,
                    onConfirmBuy = viewModel::confirmBuy,
                    onCancelBuy = viewModel::cancelBuy,
                    onWear = viewModel::wear,
                    onTakeOff = viewModel::takeOff,
                ),
            )
        }
    }
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
 * Collection tab (CLAUDE.md §6.5, design/ui_v2/Collection.dc.html): header with the sparks pill
 * over soft circles, a Pets | Accessories switch, on Pets the "next pet" card, then a two-column
 * grid of pets or accessories. Tapping one opens its sheet.
 */
@Composable
fun CollectionScreen(
    state: CollectionUiState,
    modifier: Modifier = Modifier,
    onOpenPet: (PetId) -> Unit = {},
    onSelectTab: (CollectionTab) -> Unit = {},
    onOpenAccessory: (AccessoryId) -> Unit = {},
) {
    OnSurface(Surfaces.Collection) { CollectionContent(state, modifier, onOpenPet, onSelectTab, onOpenAccessory) }
}

@Composable
private fun CollectionContent(
    state: CollectionUiState,
    modifier: Modifier,
    onOpenPet: (PetId) -> Unit,
    onSelectTab: (CollectionTab) -> Unit,
    onOpenAccessory: (AccessoryId) -> Unit,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(Surfaces.Collection.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            // After the scroll and the status bar: the circles scroll away with the header.
            .drawBehind { drawHeaderDecor() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = C.HeaderPaddingStart, end = C.HeaderPaddingEnd, top = C.HeaderPaddingTop, bottom = C.HeaderPaddingBottom),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.tab_collection),
                style = WigglebunTextStyles.TabScreenTitle,
                color = Ink,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            SparksPill(state.sparks)
        }
        Text(
            stringResource(if (state.tab == CollectionTab.PETS) R.string.collection_subtitle else R.string.collection_accessories_subtitle),
            style = WigglebunTextStyles.FocusSub,
            color = InkMuted,
            modifier = Modifier.padding(start = C.SubtitlePaddingHorizontal, end = C.SubtitlePaddingHorizontal, bottom = C.SubtitlePaddingBottom),
        )
        CollectionTabs(
            selected = state.tab,
            onSelect = onSelectTab,
            modifier = Modifier.padding(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, bottom = C.TabsPaddingBottom),
        )
        if (state.tab == CollectionTab.ACCESSORIES) {
            AccessoryGrid(state.accessories, state.activePet, onOpenAccessory)
            Spacer(Modifier.height(C.ListBottomPadding))
            return@Column
        }
        state.nextPet?.let { next ->
            NextPetCard(
                next = next,
                pet = ActivePet(next.id, state.chosenName),
                onClick = { onOpenPet(next.id) },
                modifier = Modifier.padding(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, bottom = C.NextCardPaddingBottom),
            )
        }
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

/** The mockup's two soft circles and three dots: the big circle hangs off the right edge. */
private fun DrawScope.drawHeaderDecor() {
    drawCircle(CollectionDecor, C.DecorLargeRadius.toPx(), Offset(size.width - C.DecorLargeFromRight.toPx(), C.DecorLargeY.toPx()))
    drawCircle(CollectionDecor, C.DecorSmallRadius.toPx(), Offset(C.DecorSmallX.toPx(), C.DecorSmallY.toPx()))
    C.DecorDots.forEach { dot ->
        val x = if (dot.fromRight) size.width - dot.x.toPx() else dot.x.toPx()
        drawCircle(CollectionDecorDot, dot.radius.toPx(), Offset(x, dot.y.toPx()))
    }
}

/**
 * "Pip is almost yours · 30 / 75 · About 3 more days of breaks": the cheapest pet not yet
 * adopted, in colour on a mint tile, and a bar of the sparks saved toward it. Opens its sheet.
 */
@Composable
private fun NextPetCard(next: NextPetUi, pet: ActivePet, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val name = pet.displayName()
    val title = stringResource(
        when {
            next.affordable -> R.string.next_pet_ready
            next.almostThere -> R.string.next_pet_almost
            else -> R.string.next_pet_saving
        },
        name,
    )
    val hint = if (next.affordable) {
        stringResource(R.string.next_pet_tap, name)
    } else {
        pluralStringResource(R.plurals.next_pet_days, next.daysLeft, next.daysLeft)
    }
    val description = listOf(title, pluralStringResource(R.plurals.next_pet_count_cd, next.price, next.saved, next.price), hint)
        .joinToString(". ")
    ChunkySurface(
        shape = RoundedCornerShape(C.NextCardRadius),
        faceColor = Card,
        edgeHeight = C.CardEdge,
        onClick = onClick,
        onClickLabel = stringResource(R.string.collection_open_pet),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = C.NextCardPaddingHorizontal, vertical = C.NextCardPaddingVertical)
                .clearAndSetSemantics { contentDescription = description },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(C.NextCardGap),
        ) {
            Box(
                Modifier
                    .size(C.NextTile)
                    .clip(RoundedCornerShape(C.NextTileRadius))
                    .background(NextPetTile),
                contentAlignment = Alignment.Center,
            ) {
                PetView(
                    energy = 1f,
                    mood = Mood.HAPPY,
                    spec = pet.spec,
                    name = name,
                    animated = false,
                    modifier = Modifier.size(C.NextTilePetWidth, C.NextTilePetHeight),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(C.NextCardTextGap)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(C.NextCardTextGap)) {
                    Text(title, style = WigglebunTextStyles.SmallCardTitle, color = Ink, modifier = Modifier.weight(1f))
                    Text(
                        stringResource(R.string.next_pet_count, next.saved, next.price),
                        style = WigglebunTextStyles.StrongLabel,
                        color = MovementChipText,
                        maxLines = 1,
                    )
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(C.NextBar)
                        .clip(WigglebunShapes.Pill)
                        .background(NextPetTrack),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(next.progress)
                            .height(C.NextBar)
                            .clip(WigglebunShapes.Pill)
                            .background(DoneGreen),
                    )
                }
                Text(hint, style = WigglebunTextStyles.Hint, color = InkMuted)
            }
        }
    }
}

/**
 * One pet: in colour when owned, a grey silhouette with its price when locked. The one with you
 * is framed in Ink with a warm glow behind it. Pets stand still here; the sheet animates them.
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
    ChunkySurface(
        shape = WigglebunShapes.Card,
        faceColor = Card,
        edgeHeight = C.CardEdge,
        border = if (pet.status == PetStatus.ACTIVE) BorderStroke(C.ActiveBorder, Ink) else null,
        onClick = onClick,
        onClickLabel = stringResource(R.string.collection_open_pet),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = C.CardMinHeight)
                .padding(start = C.CardPaddingHorizontal, end = C.CardPaddingHorizontal, top = C.CardPaddingTop, bottom = C.CardPaddingBottom)
                // One TalkBack stop per card: "Pip, adopt for 75 sparks".
                .clearAndSetSemantics { contentDescription = description },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(C.CardGap, Alignment.Bottom),
        ) {
            Box(contentAlignment = Alignment.TopCenter) {
                if (pet.status == PetStatus.ACTIVE) {
                    Box(
                        Modifier
                            .padding(top = C.HaloTopInPet)
                            .size(C.HaloWidth, C.HaloHeight)
                            .clip(RoundedCornerShape(C.HaloTopRadius, C.HaloTopRadius, C.HaloBottomRadius, C.HaloBottomRadius))
                            .background(ActivePetHalo),
                    )
                }
                if (pet.status == PetStatus.LOCKED) {
                    LockedPetView(active.spec, description = name, modifier = Modifier.size(C.PetWidth, C.PetHeight))
                } else {
                    PetView(
                        energy = 1f,
                        mood = Mood.HAPPY,
                        spec = active.spec,
                        name = name,
                        animated = false,
                        accessories = active.wearing,
                        modifier = Modifier.size(C.PetWidth, C.PetHeight),
                    )
                }
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
}

@Composable
private fun StatusPill(pet: CollectionPetUi) {
    val (background, textColor) = when (pet.status) {
        PetStatus.ACTIVE -> StatusOnChip to StatusOnText
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
    /** Fresh install: only Bun, 40 sparks (nothing affordable yet). */
    val OnlyBun = toCollectionUiState(setOf(PetId.BUN), ActivePet.Default, sparks = 40, selection = null)

    /** design/ui_v2/Collection.dc.html: 30 sparks toward Pip, about 3 more days. */
    val Mockup = toCollectionUiState(setOf(PetId.BUN), ActivePet.Default, sparks = 30, selection = null)

    /** Just started saving: "Saving up for Pip". */
    val Saving = toCollectionUiState(setOf(PetId.BUN), ActivePet.Default, sparks = 10, selection = null)

    /** 100 sparks: Pip (75) is affordable, Toast and Nimbus are not. */
    val OneAffordable = toCollectionUiState(setOf(PetId.BUN), ActivePet.Default, sparks = 100, selection = null)

    /** Every pet adopted, Toast with you. */
    val AllOwned = toCollectionUiState(PetId.entries.toSet(), ActivePet(PetId.TOAST, null), sparks = 40, selection = null)

    /** Accessories tab: Bun wears the glasses, the flower clip is bought, 90 sparks for the rest. */
    val Accessories = toCollectionUiState(
        owned = setOf(PetId.BUN, PetId.PIP),
        active = ActivePet(PetId.BUN, null, wearing = listOf(AccessoryId.ROUND_GLASSES)),
        sparks = 90,
        selection = null,
        ownedAccessories = setOf(AccessoryId.FLOWER_CLIP, AccessoryId.ROUND_GLASSES),
        outfits = mapOf(PetId.BUN to Outfit(setOf(AccessoryId.ROUND_GLASSES))),
        tab = CollectionTab.ACCESSORIES,
    )

    fun accessorySheet(state: CollectionUiState, item: AccessoryId, step: AccessoryStep = AccessoryStep.DETAILS): CollectionUiState =
        state.copy(accessorySheet = accessorySheet(state.accessories, state.activePet, state.sparks, AccessorySelection(item, step), busy = false))

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
private fun CollectionMockupPreview() {
    WigglebunTheme { CollectionScreen(CollectionPreviewStates.Mockup) }
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
