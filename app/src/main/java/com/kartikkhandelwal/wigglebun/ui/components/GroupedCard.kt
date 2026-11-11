package com.kartikkhandelwal.wigglebun.ui.components

import com.kartikkhandelwal.wigglebun.feedback.tickClickable
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.ui.theme.BreakEyesTint
import com.kartikkhandelwal.wigglebun.ui.theme.BreakNeckTint
import com.kartikkhandelwal.wigglebun.ui.theme.BreakStandTint
import com.kartikkhandelwal.wigglebun.ui.theme.BreakWalkTint
import com.kartikkhandelwal.wigglebun.ui.theme.BreakWristTint
import com.kartikkhandelwal.wigglebun.ui.theme.Card
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.GroupDivider
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.InkMuted
import com.kartikkhandelwal.wigglebun.ui.theme.LocalSurface
import com.kartikkhandelwal.wigglebun.ui.theme.OnSurface
import com.kartikkhandelwal.wigglebun.ui.theme.StatusOnChip
import com.kartikkhandelwal.wigglebun.ui.theme.StatusOnText
import com.kartikkhandelwal.wigglebun.ui.theme.Surfaces
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunShapes
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTextStyles
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme

private val S = Dimens.Shared

/** The rows of a [GroupedCard], in order; a divider goes between each two. */
class GroupedCardScope internal constructor() {
    internal val rows = mutableListOf<@Composable () -> Unit>()

    fun row(content: @Composable () -> Unit) {
        rows += content
    }
}

/**
 * One white card holding a section's rows, with a thin divider between them (Settings.dc.html:
 * "Your setup", "Work hours", "Breaks"). The edge is the surface's.
 */
@Composable
fun GroupedCard(
    modifier: Modifier = Modifier,
    dividerColor: Color = GroupDivider,
    content: GroupedCardScope.() -> Unit,
) {
    val rows = GroupedCardScope().apply(content).rows
    ChunkySurface(shape = WigglebunShapes.Card, faceColor = Card, edgeHeight = Dimens.CardEdge, modifier = modifier.fillMaxWidth()) {
        Column {
            rows.forEachIndexed { index, row ->
                if (index > 0) HorizontalDivider(thickness = S.DividerThickness, color = dividerColor)
                row()
            }
        }
    }
}

/**
 * A row in a [GroupedCard]: a full-colour icon on its tile, a title, an optional reason under it
 * and whatever goes on the right ([trailing]: a status chip, a button, a value). Read by TalkBack
 * as one item; tappable when [onClick] is set.
 */
@Composable
fun SettingsRow(
    @DrawableRes icon: Int,
    tile: Color,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    /** In place of [subtitle], for a line that needs more than plain text (one with the pet's name: `PetNameSentence`). */
    subtitleContent: (@Composable () -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val hasSubtitle = subtitle != null || subtitleContent != null
    val action = if (onClick != null) {
        Modifier.tickClickable(onClickLabel = onClickLabel, onClick = onClick)
    } else {
        Modifier.semantics(mergeDescendants = true) {}
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(action)
            .heightIn(min = if (hasSubtitle) S.RowMinHeight else S.RowMinHeightSingle)
            .padding(
                horizontal = S.RowPaddingHorizontal,
                vertical = if (hasSubtitle) S.RowPaddingVertical else S.RowPaddingVerticalSingle,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(S.RowGap),
    ) {
        Box(Modifier.size(S.RowTile).clip(WigglebunShapes.RowTile).background(tile), contentAlignment = Alignment.Center) {
            Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(S.RowIcon))
        }
        // At very large font sizes a button beside the text would squeeze the title until words
        // break mid-word, so whatever goes on the right moves under the text instead.
        val stacked = LocalDensity.current.fontScale >= STACK_TRAILING_FONT_SCALE
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(S.RowTextGap)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Ink)
            if (subtitleContent != null) {
                CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodySmall, LocalContentColor provides InkMuted) {
                    subtitleContent()
                }
            } else if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = InkMuted)
            }
            if (stacked) Row(Modifier.padding(top = S.RowStackedGap), content = trailing)
        }
        if (!stacked) trailing()
    }
}

/** From this font scale (the widget's large-font threshold too) a row's trailing part goes under its text. */
private const val STACK_TRAILING_FONT_SCALE = 1.6f

/** A small state pill, green for "On" / "Added" (Settings.dc.html). */
@Composable
fun StatusChip(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = StatusOnChip,
    content: Color = StatusOnText,
) {
    Text(
        text,
        style = WigglebunTextStyles.Status,
        color = content,
        modifier = modifier
            .clip(WigglebunShapes.Pill)
            .background(background)
            .padding(horizontal = S.StatusPaddingHorizontal, vertical = S.StatusPaddingVertical),
    )
}

// Preview copy is sample data only; real screens read strings from resources.
@Preview(widthDp = 390)
@Composable
internal fun GroupedCardPreview() {
    WigglebunTheme {
        OnSurface(Surfaces.Settings) {
            Column(
                Modifier.background(LocalSurface.current.background).padding(Dimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.ListGap),
            ) {
                GroupedCard {
                    row {
                        SettingsRow(R.drawable.ic_settings_bell, BreakStandTint, "Notifications", subtitle = "So Bun can remind you") {
                            StatusChip("On")
                        }
                    }
                    row {
                        SettingsRow(R.drawable.ic_break_walk, BreakWalkTint, "Movement detection", subtitle = "Walking resets the timer", onClick = {})
                    }
                    row {
                        SettingsRow(R.drawable.ic_settings_widget, BreakNeckTint, "Widget", subtitle = "Bun on your home screen") {
                            StatusChip("Added")
                        }
                    }
                }
                GroupedCard {
                    row { SettingsRow(R.drawable.ic_settings_sunrise, BreakStandTint, "Starts") { Text("9 AM", style = MaterialTheme.typography.titleLarge) } }
                    row { SettingsRow(R.drawable.ic_settings_moon, BreakEyesTint, "Ends") { Text("6 PM", style = MaterialTheme.typography.titleLarge) } }
                }
                GroupedCard {
                    row { SettingsRow(R.drawable.ic_settings_timer, BreakWristTint, "Remind me every") { Text("45 min", style = MaterialTheme.typography.titleLarge) } }
                    row { SettingsRow(R.drawable.ic_settings_shield, BreakWalkTint, "Privacy policy", onClick = {}) }
                }
            }
        }
    }
}
