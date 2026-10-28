package com.kartikkhandelwal.wigglebun.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.ui.theme.ButtonBorder
import com.kartikkhandelwal.wigglebun.ui.theme.ButtonEdge
import com.kartikkhandelwal.wigglebun.ui.theme.Card
import com.kartikkhandelwal.wigglebun.ui.theme.CardEdge
import com.kartikkhandelwal.wigglebun.ui.theme.CheckIdle
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.DoneGreen
import com.kartikkhandelwal.wigglebun.ui.theme.DoneGreenEdge
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.InkEdge
import com.kartikkhandelwal.wigglebun.ui.theme.InkMuted
import com.kartikkhandelwal.wigglebun.ui.theme.SessionEdge
import com.kartikkhandelwal.wigglebun.ui.theme.Sun
import com.kartikkhandelwal.wigglebun.ui.theme.SunEdge
import com.kartikkhandelwal.wigglebun.ui.theme.Track
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunShapes
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme

/** Colour sets from design/mockups_source/Tokens.dc.html ("Buttons and shape"). */
enum class ChunkyButtonStyle(
    val face: Color,
    val edge: Color,
    val content: Color,
    val edgeHeight: Dp,
    val borderColor: Color? = null,
) {
    Primary(face = Ink, edge = InkEdge, content = Card, edgeHeight = Dimens.ButtonEdge),
    Secondary(
        face = Card,
        edge = ButtonEdge,
        content = Ink,
        edgeHeight = Dimens.SecondaryButtonEdge,
        borderColor = ButtonBorder,
    ),
    Done(face = DoneGreen, edge = DoneGreenEdge, content = Card, edgeHeight = Dimens.ButtonEdge),
    Highlight(face = Sun, edge = SunEdge, content = Ink, edgeHeight = Dimens.ButtonEdge),

    /**
     * A button that can't be tapped yet but still says something ("With you", "30 more sparks"):
     * flat Track face and full-strength InkMuted text instead of the 50% fade, so it stays legible.
     */
    Inactive(face = Track, edge = CardEdge, content = InkMuted, edgeHeight = Dimens.SecondaryButtonEdge),

    /** Secondary on the blue break-session background (BreakSession.dc.html "Pause"). */
    SessionSecondary(
        face = Card,
        edge = SessionEdge,
        content = Ink,
        edgeHeight = Dimens.SecondaryButtonEdge,
        borderColor = SessionEdge,
    ),
}

@Composable
fun ChunkyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ChunkyButtonStyle = ChunkyButtonStyle.Primary,
    shape: Shape = WigglebunShapes.Pill,
    enabled: Boolean = true,
    contentColor: Color = style.content,
    contentPadding: PaddingValues = PaddingValues(horizontal = Dimens.ButtonHorizontalPadding),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    ChunkySurface(
        shape = shape,
        faceColor = style.face,
        edgeColor = style.edge,
        edgeHeight = style.edgeHeight,
        modifier = modifier
            .alpha(if (enabled || style == ChunkyButtonStyle.Inactive) 1f else 0.5f)
            .defaultMinSize(minHeight = Dimens.ButtonHeight + style.edgeHeight),
        border = style.borderColor?.let { BorderStroke(Dimens.ButtonBorder, it) },
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                Row(
                    modifier = Modifier.padding(contentPadding),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content,
                )
            }
        }
    }
}

@Composable
fun ChunkyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ChunkyButtonStyle = ChunkyButtonStyle.Primary,
    enabled: Boolean = true,
    /** Long labels in full-width buttons: wrap to more lines instead of clipping at large font sizes. */
    wrap: Boolean = false,
) {
    ChunkyButton(onClick = onClick, modifier = modifier, style = style, enabled = enabled) {
        if (wrap) {
            // Wraps rather than clips at large font sizes (200% on a small phone): the button grows taller.
            Text(text = text, textAlign = TextAlign.Center)
        } else {
            Text(text = text, maxLines = 1)
        }
    }
}

/** Square chunky button, e.g. the green Done check. */
@Composable
fun ChunkyIconButton(
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    style: ChunkyButtonStyle = ChunkyButtonStyle.Done,
    @DrawableRes iconRes: Int = R.drawable.ic_check,
    iconTint: Color = if (style == ChunkyButtonStyle.Secondary) CheckIdle else style.content,
    enabled: Boolean = true,
) {
    ChunkyButton(
        onClick = onClick,
        modifier = modifier.size(
            width = Dimens.IconButtonSize,
            height = Dimens.IconButtonSize + style.edgeHeight,
        ),
        style = style,
        shape = WigglebunShapes.SmallButton,
        enabled = enabled,
        contentColor = iconTint,
        contentPadding = PaddingValues(),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(Dimens.IconButtonIcon),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
internal fun ChunkyButtonPreview() {
    WigglebunTheme {
        Column(
            modifier = Modifier.padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.ListGap),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ListGap)) {
                ChunkyButton(text = "Primary", onClick = {})
                ChunkyButton(text = "Secondary", onClick = {}, style = ChunkyButtonStyle.Secondary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ListGap)) {
                ChunkyButton(text = "Highlight", onClick = {}, style = ChunkyButtonStyle.Highlight)
                ChunkyButton(text = "Disabled", onClick = {}, enabled = false)
            }
            ChunkyButton(
                text = "Take a 2-min break",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ListGap)) {
                ChunkyIconButton(onClick = {}, contentDescription = stringResource(R.string.cd_done))
                ChunkyIconButton(
                    onClick = {},
                    contentDescription = stringResource(R.string.cd_mark_done),
                    style = ChunkyButtonStyle.Secondary,
                )
            }
        }
    }
}
