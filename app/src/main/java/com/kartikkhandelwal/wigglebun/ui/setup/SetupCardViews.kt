package com.kartikkhandelwal.wigglebun.ui.setup

import com.kartikkhandelwal.wigglebun.feedback.tickClickable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import com.kartikkhandelwal.wigglebun.R
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.kartikkhandelwal.wigglebun.ui.components.ChunkySurface
import com.kartikkhandelwal.wigglebun.ui.components.SettingsRow
import com.kartikkhandelwal.wigglebun.ui.components.StatusChip
import com.kartikkhandelwal.wigglebun.ui.pet.PetNameSentence
import com.kartikkhandelwal.wigglebun.ui.theme.Card
import com.kartikkhandelwal.wigglebun.ui.theme.InkEdge
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTextStyles
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButton
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButtonStyle
import com.kartikkhandelwal.wigglebun.ui.components.PillCard
import com.kartikkhandelwal.wigglebun.ui.pet.PetSpecs
import com.kartikkhandelwal.wigglebun.ui.pet.PetView
import com.kartikkhandelwal.wigglebun.ui.theme.BreakNeckTint
import com.kartikkhandelwal.wigglebun.ui.theme.BreakStandTint
import com.kartikkhandelwal.wigglebun.ui.theme.BreakWalkTint
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.InkMuted
import com.kartikkhandelwal.wigglebun.ui.theme.MoodHappyChip
import com.kartikkhandelwal.wigglebun.ui.theme.MovementChipText
import com.kartikkhandelwal.wigglebun.ui.theme.Track
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunShapes

private val S = Dimens.Setup
private val D = Dimens.Settings

@get:StringRes
private val SetupCard.title: Int
    get() = when (this) {
        SetupCard.NOTIFICATIONS -> R.string.setup_notifications_title
        SetupCard.MOVEMENT -> R.string.setup_movement_title
        SetupCard.WIDGET -> R.string.setup_widget_title
    }

/** A row's name in Settings: short, whatever the state (the button or chip on the right says it). */
@get:StringRes
private val SetupCard.shortTitle: Int
    get() = when (this) {
        SetupCard.NOTIFICATIONS -> R.string.setup_notifications_title_on
        SetupCard.MOVEMENT -> R.string.setup_movement_title_on
        SetupCard.WIDGET -> R.string.setup_widget_title_on
    }

/** Under the name in Settings (design/ui_v2/Settings.dc.html); %1$s is the pet's name where there is one. */
@get:StringRes
private val SetupCard.settingsWhy: Int
    get() = when (this) {
        SetupCard.NOTIFICATIONS -> R.string.settings_notifications_why
        SetupCard.MOVEMENT -> R.string.settings_movement_why
        SetupCard.WIDGET -> R.string.settings_widget_why
    }

/** The "why" from CLAUDE.md §10, one line on the card. */
@get:StringRes
private val SetupCard.why: Int
    get() = when (this) {
        SetupCard.NOTIFICATIONS -> R.string.setup_notifications_why
        SetupCard.MOVEMENT -> R.string.setup_movement_why
        SetupCard.WIDGET -> R.string.setup_widget_why
    }

/** Full-colour icons (design/ui_v2/icons), drawn untinted. */
@get:DrawableRes
private val SetupCard.icon: Int
    get() = when (this) {
        SetupCard.NOTIFICATIONS -> R.drawable.ic_settings_bell
        SetupCard.MOVEMENT -> R.drawable.ic_break_walk
        SetupCard.WIDGET -> R.drawable.ic_settings_widget
    }

/** The tile behind each icon, as in design/ui_v2/Settings.dc.html. */
private val SetupCard.tile: Color
    get() = when (this) {
        SetupCard.NOTIFICATIONS -> BreakStandTint
        SetupCard.MOVEMENT -> BreakWalkTint
        SetupCard.WIDGET -> BreakNeckTint
    }

@Composable
private fun IconTile(card: SetupCard) {
    Box(
        Modifier.size(S.IconTile).clip(WigglebunShapes.IconTile).background(card.tile),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(card.icon), contentDescription = null, modifier = Modifier.size(S.Icon))
    }
}

/**
 * One row of Settings' "Your setup" card: the name, why under it, and on the right a small
 * "Turn on" ("Add" for the widget) while it's off, or an "On" / "Added" chip. Refused in the
 * phone's settings, the line under the name says so and "Turn on" leads there (through the
 * explanation sheet, as every row does). Once on there's nothing to do, so it can't be tapped.
 */
@Composable
fun SetupSettingsRow(ui: SetupCardUi, petName: String, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val isOn = ui.status == SetupStatus.ON
    val isWidget = ui.card == SetupCard.WIDGET
    val action = stringResource(if (isWidget) R.string.setup_add else R.string.setup_turn_on)
    SettingsRow(
        icon = ui.card.icon,
        tile = ui.card.tile,
        title = stringResource(ui.card.shortTitle),
        modifier = modifier,
        onClick = if (isOn) null else onOpen,
        onClickLabel = action,
        subtitleContent = {
            if (ui.status == SetupStatus.BLOCKED) {
                Text(stringResource(R.string.setup_blocked))
            } else {
                // Plain text: the row grows to fit, so the name is never shortened here ("So Bun can
                // remind you" must not read "So B… can remind you").
                Text(stringResource(ui.card.settingsWhy, petName))
            }
        },
    ) {
        if (isOn) {
            StatusChip(stringResource(if (isWidget) R.string.setup_status_added else R.string.setup_status_on))
        } else {
            RowActionButton(action)
        }
    }
}

/**
 * The small Ink button on a row ("Turn on"). The whole row is the touch target and TalkBack's
 * action, so this only shows what a tap does.
 */
@Composable
private fun RowActionButton(text: String) {
    ChunkySurface(
        shape = RoundedCornerShape(D.ActionRadius),
        faceColor = Ink,
        edgeColor = InkEdge,
        edgeHeight = D.ActionEdge,
        modifier = Modifier.clearAndSetSemantics {},
    ) {
        Text(
            text,
            style = WigglebunTextStyles.Status,
            color = Card,
            modifier = Modifier.padding(horizontal = D.ActionPaddingHorizontal, vertical = D.ActionPaddingVertical),
        )
    }
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
                    .tickClickable(onClick = onNotNow),
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
