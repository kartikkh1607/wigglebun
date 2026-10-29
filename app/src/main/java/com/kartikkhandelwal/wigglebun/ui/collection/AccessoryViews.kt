package com.kartikkhandelwal.wigglebun.ui.collection

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.domain.model.AccessoryId
import com.kartikkhandelwal.wigglebun.domain.model.ActivePet
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButton
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButtonStyle
import com.kartikkhandelwal.wigglebun.ui.components.ChunkySurface
import com.kartikkhandelwal.wigglebun.ui.components.SparkIcon
import com.kartikkhandelwal.wigglebun.ui.pet.PetNameSentence
import com.kartikkhandelwal.wigglebun.ui.pet.PetView
import com.kartikkhandelwal.wigglebun.ui.pet.displayName
import com.kartikkhandelwal.wigglebun.ui.pet.spec
import com.kartikkhandelwal.wigglebun.ui.theme.Card
import com.kartikkhandelwal.wigglebun.ui.theme.CardEdge
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.InkMuted
import com.kartikkhandelwal.wigglebun.ui.theme.MoodHappyChip
import com.kartikkhandelwal.wigglebun.ui.theme.MoodTiredChip
import com.kartikkhandelwal.wigglebun.ui.theme.MovementChipText
import com.kartikkhandelwal.wigglebun.ui.theme.Track
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunShapes
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTextStyles

private val C = Dimens.Collection

@get:StringRes
val AccessoryId.nameRes: Int
    get() = when (this) {
        AccessoryId.FLOWER_CLIP -> R.string.accessory_flower_clip
        AccessoryId.ROUND_GLASSES -> R.string.accessory_round_glasses
        AccessoryId.KNIT_SCARF -> R.string.accessory_knit_scarf
        AccessoryId.BEANIE -> R.string.accessory_beanie
    }

@get:StringRes
private val AccessoryId.blurbRes: Int
    get() = when (this) {
        AccessoryId.FLOWER_CLIP -> R.string.accessory_blurb_flower_clip
        AccessoryId.ROUND_GLASSES -> R.string.accessory_blurb_round_glasses
        AccessoryId.KNIT_SCARF -> R.string.accessory_blurb_knit_scarf
        AccessoryId.BEANIE -> R.string.accessory_blurb_beanie
    }

/** Pets | Accessories: a fully rounded segmented pill (CLAUDE.md §11). */
@Composable
internal fun CollectionTabs(selected: CollectionTab, onSelect: (CollectionTab) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(WigglebunShapes.Pill)
            .background(Track)
            .padding(C.TabsInnerPadding)
            .selectableGroup(),
    ) {
        CollectionTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(WigglebunShapes.Pill)
                    .background(if (isSelected) Card else Track)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(tab) })
                    .padding(vertical = C.TabPaddingVertical),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(if (tab == CollectionTab.PETS) R.string.collection_tab_pets else R.string.collection_tab_accessories),
                    style = WigglebunTextStyles.Chip,
                    color = if (isSelected) Ink else InkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Every accessory, tried on by the pet that is with you, two per row. */
@Composable
internal fun AccessoryGrid(items: List<AccessoryUi>, pet: ActivePet, onOpen: (AccessoryId) -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(C.GridGap),
    ) {
        items.chunked(2).forEach { row ->
            Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(C.GridGap)) {
                row.forEach { item ->
                    AccessoryCard(item, pet, onClick = { onOpen(item.id) }, modifier = Modifier.weight(1f).fillMaxHeight())
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AccessoryCard(item: AccessoryUi, pet: ActivePet, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val name = stringResource(item.id.nameRes)
    val petName = pet.displayName()
    val description = when (item.status) {
        AccessoryStatus.WORN -> stringResource(R.string.accessory_cd_worn, name, petName)
        AccessoryStatus.OWNED -> stringResource(R.string.accessory_cd_owned, name)
        AccessoryStatus.LOCKED -> pluralStringResource(R.plurals.accessory_cd_locked, item.price, name, item.price)
    }
    val wearer = pet.tryingOn(item.id)
    ChunkySurface(
        shape = WigglebunShapes.Card,
        faceColor = Card,
        edgeColor = CardEdge,
        edgeHeight = C.CardEdge,
        onClick = onClick,
        onClickLabel = stringResource(R.string.collection_open_pet),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = C.CardPaddingHorizontal, end = C.CardPaddingHorizontal, top = C.CardPaddingTop, bottom = C.CardPaddingBottom)
                .clearAndSetSemantics { contentDescription = description },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(C.CardGap),
        ) {
            PetView(
                energy = 1f,
                mood = Mood.HAPPY,
                spec = wearer.spec,
                name = petName,
                animated = false,
                accessories = wearer.wearing,
                modifier = Modifier.size(C.PetSize),
            )
            // Up to two lines at large font sizes; the row's cards keep one height (IntrinsicSize.Max).
            Text(name, style = WigglebunTextStyles.PetCardName, color = Ink, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            AccessoryPill(item)
        }
    }
}

@Composable
private fun AccessoryPill(item: AccessoryUi) {
    val (background, textColor) = when (item.status) {
        AccessoryStatus.WORN -> MoodHappyChip to MovementChipText
        AccessoryStatus.OWNED -> Track to Ink
        AccessoryStatus.LOCKED -> MoodTiredChip to Ink
    }
    Row(
        modifier = Modifier
            .clip(WigglebunShapes.Pill)
            .background(background)
            .padding(horizontal = C.PillPaddingHorizontal, vertical = C.PillPaddingVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(C.PillIconGap),
    ) {
        if (item.status == AccessoryStatus.LOCKED) SparkIcon(C.PillIcon)
        val text = when (item.status) {
            AccessoryStatus.WORN -> stringResource(R.string.collection_wearing)
            AccessoryStatus.OWNED -> stringResource(R.string.collection_owned)
            AccessoryStatus.LOCKED -> item.price.toString()
        }
        Text(text, style = WigglebunTextStyles.Chip, color = textColor)
    }
}

/** What the accessory sheet's buttons do; each gets the accessory the sheet is about. */
class AccessorySheetActions(
    val onAskToBuy: (AccessoryId) -> Unit = {},
    val onConfirmBuy: (AccessoryId) -> Unit = {},
    val onCancelBuy: (AccessoryId) -> Unit = {},
    val onWear: (AccessoryId) -> Unit = {},
    val onTakeOff: (AccessoryId) -> Unit = {},
)

/**
 * One accessory up close: the pet with you trying it on, its name and line, and one button
 * (buy, how many sparks are missing, put on or take off). Buying asks first, then puts it on.
 */
@Composable
fun AccessorySheetContent(sheet: AccessorySheetUi, sparks: Int, actions: AccessorySheetActions, modifier: Modifier = Modifier) {
    val id = sheet.item.id
    val name = stringResource(id.nameRes)
    val pet = sheet.preview
    val petName = pet.displayName()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = C.SheetPaddingHorizontal, end = C.SheetPaddingHorizontal, bottom = C.SheetPaddingBottom),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(C.SheetGap),
    ) {
        when (sheet.step) {
            AccessoryStep.DETAILS -> {
                PetView(energy = 1f, mood = Mood.HAPPY, spec = pet.spec, name = petName, accessories = pet.wearing, modifier = Modifier.size(C.SheetPetSize))
                Text(name, style = MaterialTheme.typography.headlineMedium, color = Ink, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(id.blurbRes), style = MaterialTheme.typography.bodyLarge, color = InkMuted, textAlign = TextAlign.Center)
                AccessoryMainAction(sheet.action, petName, enabled = !sheet.busy, id = id, actions = actions)
            }
            AccessoryStep.CONFIRM -> {
                PetView(energy = 1f, mood = Mood.HAPPY, spec = pet.spec, name = petName, accessories = pet.wearing, modifier = Modifier.size(C.SheetSmallPetSize))
                Text(
                    stringResource(R.string.buy_confirm_title, name),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Ink,
                    textAlign = TextAlign.Center,
                )
                Text(
                    pluralStringResource(R.plurals.adopt_confirm_body, sheet.item.price, sheet.item.price, sparks),
                    style = MaterialTheme.typography.bodyLarge,
                    color = InkMuted,
                    textAlign = TextAlign.Center,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = C.SheetButtonTop),
                    horizontalArrangement = Arrangement.spacedBy(C.GridGap),
                ) {
                    ChunkyButton(
                        text = stringResource(R.string.adopt_confirm_no),
                        onClick = { actions.onCancelBuy(id) },
                        style = ChunkyButtonStyle.Secondary,
                        enabled = !sheet.busy,
                        modifier = Modifier.weight(1f),
                    )
                    ChunkyButton(
                        text = stringResource(R.string.buy_confirm_yes),
                        onClick = { actions.onConfirmBuy(id) },
                        style = ChunkyButtonStyle.Done,
                        enabled = !sheet.busy,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AccessoryMainAction(
    action: AccessoryAction,
    petName: String,
    enabled: Boolean,
    id: AccessoryId,
    actions: AccessorySheetActions,
) {
    val modifier = Modifier.fillMaxWidth().padding(top = C.SheetButtonTop)
    when (action) {
        is AccessoryAction.Buy -> {
            val description = pluralStringResource(R.plurals.sheet_buy_cd, action.price, action.price)
            ChunkyButton(
                onClick = { actions.onAskToBuy(id) },
                style = ChunkyButtonStyle.Highlight,
                enabled = enabled,
                modifier = modifier.semantics { contentDescription = description },
            ) {
                Text(stringResource(R.string.sheet_buy, action.price), maxLines = 1, modifier = Modifier.clearAndSetSemantics {})
                // In Ink like the label: the yellow star would vanish on the Sun button.
                Box(Modifier.padding(start = C.SheetButtonIconGap)) { SparkIcon(C.SheetButtonIcon, tint = Ink) }
            }
        }
        is AccessoryAction.NeedMore -> ChunkyButton(
            text = pluralStringResource(R.plurals.sheet_more_sparks, action.missing, action.missing),
            onClick = {},
            style = ChunkyButtonStyle.Inactive,
            enabled = false,
            modifier = modifier,
        )
        // One line in the button: a long name is shortened ("Put on Wigglebo…").
        AccessoryAction.Wear -> ChunkyButton(onClick = { actions.onWear(id) }, enabled = enabled, modifier = modifier) {
            PetNameSentence(R.string.sheet_wear, petName, maxLines = 1)
        }
        AccessoryAction.TakeOff -> ChunkyButton(
            text = stringResource(R.string.sheet_take_off),
            onClick = { actions.onTakeOff(id) },
            style = ChunkyButtonStyle.Secondary,
            enabled = enabled,
            modifier = modifier,
        )
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun AccessoriesTabPreview() =
    CollectionWithSheetPreview(CollectionPreviewStates.Accessories)

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun AccessorySheetPreview() =
    CollectionWithSheetPreview(CollectionPreviewStates.accessorySheet(CollectionPreviewStates.Accessories, AccessoryId.BEANIE))
