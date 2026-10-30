package com.kartikkhandelwal.wigglebun.ui.setup

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButton
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButtonStyle
import com.kartikkhandelwal.wigglebun.ui.components.PillCard
import com.kartikkhandelwal.wigglebun.ui.pet.PetSpecs
import com.kartikkhandelwal.wigglebun.ui.pet.PetView
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.InkMuted
import com.kartikkhandelwal.wigglebun.ui.theme.MoodHappyChip
import com.kartikkhandelwal.wigglebun.ui.theme.MovementChipText
import com.kartikkhandelwal.wigglebun.ui.theme.Track
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunShapes

private val S = Dimens.Setup

@get:StringRes
private val SetupCard.title: Int
    get() = when (this) {
        SetupCard.NOTIFICATIONS -> R.string.setup_notifications_title
        SetupCard.MOVEMENT -> R.string.setup_movement_title
        SetupCard.WIDGET -> R.string.setup_widget_title
    }

/** The "why" from CLAUDE.md §10, one line on the card. */
@get:StringRes
private val SetupCard.why: Int
    get() = when (this) {
        SetupCard.NOTIFICATIONS -> R.string.setup_notifications_why
        SetupCard.MOVEMENT -> R.string.setup_movement_why
        SetupCard.WIDGET -> R.string.setup_widget_why
    }

@get:DrawableRes
private val SetupCard.icon: Int
    get() = when (this) {
        SetupCard.NOTIFICATIONS -> R.drawable.ic_setup_notifications
        SetupCard.MOVEMENT -> R.drawable.ic_break_walk
        SetupCard.WIDGET -> R.drawable.ic_tab_collection
    }

@Composable
private fun IconTile(card: SetupCard) {
    Box(
        Modifier.size(S.IconTile).clip(WigglebunShapes.IconTile).background(Track),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(card.icon), contentDescription = null, tint = Ink, modifier = Modifier.size(S.Icon))
    }
}

/** One row in Settings. A card that's already on has nothing to do, so it can't be tapped. */
@Composable
fun SetupCardRow(ui: SetupCardUi, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    PillCard(modifier = modifier.fillMaxWidth(), onClick = if (ui.status == SetupStatus.ON) null else onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(S.RowGap)) {
            IconTile(ui.card)
            Column(Modifier.weight(1f)) {
                Text(stringResource(ui.card.title), style = MaterialTheme.typography.titleMedium, color = Ink)
                Text(
                    stringResource(if (ui.status == SetupStatus.BLOCKED) R.string.setup_blocked else ui.card.why),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                )
            }
            StatusChip(ui)
        }
    }
}

@Composable
private fun StatusChip(ui: SetupCardUi) {
    val (label, background, content) = when (ui.status) {
        SetupStatus.ON -> Triple(
            if (ui.card == SetupCard.WIDGET) R.string.setup_status_added else R.string.setup_status_on,
            MoodHappyChip,
            MovementChipText,
        )
        SetupStatus.OFF, SetupStatus.BLOCKED, SetupStatus.MANUAL -> Triple(R.string.setup_status_off, Track, InkMuted)
    }
    Text(
        stringResource(label),
        style = MaterialTheme.typography.labelMedium,
        color = content,
        modifier = Modifier
            .clip(WigglebunShapes.Pill)
            .background(background)
            .padding(horizontal = S.ChipHorizontal, vertical = S.ChipVertical),
    )
}

/** Home offers one card at a time. Tapping it explains first, the same as in Settings. */
@Composable
fun SetupHomePrompt(ui: SetupCardUi, onOpen: () -> Unit, onNotNow: () -> Unit, modifier: Modifier = Modifier) {
    PillCard(modifier = modifier.fillMaxWidth(), onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(S.RowGap)) {
            IconTile(ui.card)
            Column(Modifier.weight(1f)) {
                Text(stringResource(ui.card.title), style = MaterialTheme.typography.titleMedium, color = Ink)
                Text(stringResource(ui.card.why), style = MaterialTheme.typography.bodyMedium, color = InkMuted)
            }
            Box(
                Modifier
                    .defaultMinSize(minWidth = S.NotNowMinSize, minHeight = S.NotNowMinSize)
                    .clip(WigglebunShapes.Pill)
                    .clickable(role = Role.Button, onClick = onNotNow),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.setup_not_now), style = MaterialTheme.typography.labelMedium, color = InkMuted)
            }
        }
    }
}

/** Shown before any system dialog (CLAUDE.md §10): the pet says why, and the user decides. */
@Composable
fun SetupExplainContent(ui: SetupCardUi, onPrimary: () -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = S.SheetHorizontal, end = S.SheetHorizontal, bottom = S.SheetBottom),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(S.SheetGap),
    ) {
        PetView(energy = 1f, mood = Mood.HAPPY, spec = PetSpecs.Bun, modifier = Modifier.size(S.SheetPet))
        Text(stringResource(ui.card.title), style = MaterialTheme.typography.titleLarge, color = Ink, textAlign = TextAlign.Center)
        Text(explanation(ui), style = MaterialTheme.typography.bodyLarge, color = InkMuted, textAlign = TextAlign.Center)
        val primary = primaryLabel(ui)
        if (primary != null) {
            ChunkyButton(text = stringResource(primary), onClick = onPrimary, modifier = Modifier.fillMaxWidth())
        }
        ChunkyButton(
            text = stringResource(if (primary != null) R.string.setup_not_now else R.string.setup_got_it),
            onClick = onClose,
            style = if (primary != null) ChunkyButtonStyle.Secondary else ChunkyButtonStyle.Primary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun explanation(ui: SetupCardUi): String = stringResource(
    when {
        ui.status == SetupStatus.MANUAL -> R.string.setup_widget_manual
        ui.status == SetupStatus.BLOCKED -> when (ui.card) {
            SetupCard.MOVEMENT -> R.string.setup_movement_blocked
            else -> R.string.setup_notifications_blocked
        }
        ui.card == SetupCard.NOTIFICATIONS -> R.string.setup_notifications_explain
        ui.card == SetupCard.MOVEMENT -> R.string.setup_movement_explain
        else -> R.string.setup_widget_explain
    },
)

@StringRes
private fun primaryLabel(ui: SetupCardUi): Int? = when (ui.action) {
    is SetupAction.RequestPermission -> R.string.setup_allow
    SetupAction.EnableMovement -> R.string.setup_turn_on
    SetupAction.PinWidget -> R.string.setup_add_widget
    SetupAction.OpenNotificationSettings, SetupAction.OpenAppSettings -> R.string.setup_open_settings
    SetupAction.None -> null
}
