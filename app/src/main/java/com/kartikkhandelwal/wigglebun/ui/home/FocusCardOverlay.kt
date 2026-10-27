package com.kartikkhandelwal.wigglebun.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.ui.categoryChipColors
import com.kartikkhandelwal.wigglebun.ui.components.ChunkySurface
import com.kartikkhandelwal.wigglebun.ui.labelRes
import com.kartikkhandelwal.wigglebun.ui.theme.ActionEdge
import com.kartikkhandelwal.wigglebun.ui.theme.Card
import com.kartikkhandelwal.wigglebun.ui.theme.CloseBorder
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.DoneGreen
import com.kartikkhandelwal.wigglebun.ui.theme.DoneGreenEdge
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.InkMuted
import com.kartikkhandelwal.wigglebun.ui.theme.Scrim
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunShapes
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTextStyles

private val H = Dimens.Home

/** Pop-in: scale 0.85 → 1 on a bouncy spring (slight overshoot, never linear). */
private const val POP_FROM_SCALE = 0.85f
private const val POP_DAMPING = 0.6f

/**
 * Full-screen dialog for [FocusCardOverlay]: covers the tab bar like the mockup and handles Back.
 * Edge-to-edge (the overlay pads for the nav bar itself) and no window dim (it draws its own scrim).
 */
@Composable
fun FocusCardDialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(0f) }
        content()
    }
}

/**
 * The focused break card (design/mockups_source/FocusCard.dc.html): card + Skip · Done · Snooze
 * over a dark scrim, with a close button. Stateless; Home shows it in a full-screen Dialog.
 */
@Composable
fun FocusCardOverlay(
    focus: FocusUi,
    intervalMinutes: Int,
    workStartMinute: Int,
    workEndMinute: Int,
    onSkip: () -> Unit,
    onDone: () -> Unit,
    onSnooze: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    animated: Boolean = !LocalInspectionMode.current,
) {
    val appear = remember { Animatable(if (animated) 0f else 1f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, spring(dampingRatio = POP_DAMPING, stiffness = Spring.StiffnessMediumLow))
    }
    Box(
        modifier
            .fillMaxSize()
            .graphicsLayer { alpha = appear.value.coerceIn(0f, 1f) }
            .background(Scrim)
            // Taps on the scrim do nothing (only Close/back dismiss), like the mockup.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        // The scrim runs under the system bars (edge-to-edge dialog); the content stays clear of them.
        BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .anchorToBottom(viewportHeight = maxHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FocusCard(
                    focus = focus,
                    intervalMinutes = intervalMinutes,
                    workStartMinute = workStartMinute,
                    workEndMinute = workEndMinute,
                    modifier = Modifier.graphicsLayer {
                        val scale = POP_FROM_SCALE + (1f - POP_FROM_SCALE) * appear.value
                        scaleX = scale
                        scaleY = scale
                    },
                )
                Spacer(Modifier.height(H.FocusToActionsGap))
                ActionsRow(focus, onSkip, onDone, onSnooze)
                Spacer(Modifier.height(H.ActionsToCloseGap))
                CloseButton(onClose)
            }
        }
    }
}

/**
 * Bottom-anchors the content like the mockup (close button [Dimens.Home.CloseBottomMargin] above
 * the bottom). On a short screen or with a large font the bottom margin shrinks first, keeping at
 * least [Dimens.Home.OverlayMinMargin] above and below; only after that does the content scroll.
 */
private fun Modifier.anchorToBottom(viewportHeight: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val viewport = viewportHeight.roundToPx()
    val minMargin = H.OverlayMinMargin.roundToPx()
    val bottom = (viewport - placeable.height - minMargin).coerceIn(minMargin, H.CloseBottomMargin.roundToPx())
    val height = maxOf(viewport, placeable.height + bottom + minMargin)
    layout(placeable.width, height) { placeable.place(0, height - bottom - placeable.height) }
}

@Composable
private fun FocusCard(
    focus: FocusUi,
    intervalMinutes: Int,
    workStartMinute: Int,
    workEndMinute: Int,
    modifier: Modifier = Modifier,
) {
    val type = focus.type
    val (chipBackground, chipText) = type.categoryChipColors()
    Box(
        modifier
            .padding(horizontal = H.FocusMargin)
            .fillMaxWidth()
            .clip(WigglebunShapes.FocusedCard)
            .background(Card),
    ) {
        // A break type earns sparks only the first time each day (CLAUDE.md §5.4), so a repeat
        // doesn't promise any.
        if (!focus.doneToday) {
            Reward(
                sparks = type.defaultReward,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = H.FocusRewardInset, end = H.FocusRewardInset),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = H.FocusPaddingHorizontal,
                    end = H.FocusPaddingHorizontal,
                    top = H.FocusPaddingTop,
                    bottom = H.FocusPaddingBottom,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(H.FocusGap),
        ) {
            BreakIconTile(type, H.FocusTileSize, RoundedCornerShape(H.FocusTileRadius), H.FocusIcon)
            Text(
                text = stringResource(type.category.labelRes),
                style = WigglebunTextStyles.Chip,
                color = chipText,
                modifier = Modifier
                    .clip(WigglebunShapes.Pill)
                    .background(chipBackground)
                    .padding(horizontal = H.FocusChipPaddingHorizontal, vertical = H.FocusChipPaddingVertical),
            )
            Text(
                text = stringResource(type.labelRes),
                style = MaterialTheme.typography.headlineMedium,
                color = Ink,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(
                    R.string.focus_reminds,
                    intervalMinutes,
                    formatTime(minuteOfDay(workStartMinute)),
                    formatTime(minuteOfDay(workEndMinute)),
                ),
                style = WigglebunTextStyles.FocusSub,
                color = InkMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ActionsRow(focus: FocusUi, onSkip: () -> Unit, onDone: () -> Unit, onSnooze: () -> Unit) {
    // Three fixed slots like the mockup, so Done is always in the middle and Skip on the left;
    // the Snooze slot stays empty until the reminder is due.
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = H.ActionsPaddingHorizontal),
        verticalAlignment = Alignment.Top,
    ) {
        ActionButton(
            icon = R.drawable.ic_skip,
            label = stringResource(R.string.focus_skip),
            description = stringResource(R.string.focus_skip),
            size = H.ActionSize,
            radius = H.ActionRadius,
            edge = H.ActionEdge,
            iconSize = H.ActionIcon,
            face = Card,
            edgeColor = ActionEdge,
            iconTint = Ink,
            onClick = onSkip,
            modifier = Modifier.weight(1f),
        )
        // Always enabled: doing a break again (even one already done today) still resets the timer.
        ActionButton(
            icon = R.drawable.ic_check,
            label = stringResource(R.string.focus_done),
            description = stringResource(R.string.focus_done),
            size = H.DoneActionSize,
            radius = H.DoneActionRadius,
            edge = H.DoneActionEdge,
            iconSize = H.DoneActionIcon,
            face = DoneGreen,
            edgeColor = DoneGreenEdge,
            iconTint = Card,
            onClick = onDone,
            modifier = Modifier.weight(1f),
        )
        if (focus.canSnooze) {
            ActionButton(
                icon = R.drawable.ic_snooze,
                label = stringResource(R.string.focus_snooze),
                description = stringResource(R.string.cd_snooze),
                size = H.ActionSize,
                radius = H.ActionRadius,
                edge = H.ActionEdge,
                iconSize = H.ActionIcon,
                face = Card,
                edgeColor = ActionEdge,
                iconTint = Ink,
                onClick = onSnooze,
                modifier = Modifier.weight(1f),
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun ActionButton(
    @DrawableRes icon: Int,
    label: String,
    description: String,
    size: Dp,
    radius: Dp,
    edge: Dp,
    iconSize: Dp,
    face: Color,
    edgeColor: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        // All faces end on one line (the mockup's align-items: end) and all labels start on one
        // line below, even when a label wraps at a large font.
        Box(Modifier.height(H.ActionSlotHeight), contentAlignment = Alignment.BottomCenter) {
            ChunkySurface(
                shape = RoundedCornerShape(radius),
                faceColor = face,
                edgeColor = edgeColor,
                edgeHeight = edge,
                onClick = onClick,
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    // Thinner edges than Done's: lift the button so the faces' bottoms line up.
                    .padding(bottom = H.DoneActionEdge - edge)
                    .size(size, size + edge)
                    .semantics { contentDescription = description },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painterResource(icon), contentDescription = null, tint = iconTint, modifier = Modifier.size(iconSize))
                }
            }
        }
        // The mockup's label gap is measured from the face, with the edge drawn inside it.
        Spacer(Modifier.height(H.ActionLabelGap - H.DoneActionEdge))
        Text(label, style = MaterialTheme.typography.titleSmall, color = Card, textAlign = TextAlign.Center)
    }
}

@Composable
private fun CloseButton(onClose: () -> Unit) {
    val description = stringResource(R.string.cd_close)
    Box(
        modifier = Modifier
            .size(H.CloseSize)
            .clip(CircleShape)
            .border(BorderStroke(H.CloseBorder, CloseBorder), CircleShape)
            .clickable(role = Role.Button, onClick = onClose)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(R.drawable.ic_close), contentDescription = null, tint = Card, modifier = Modifier.size(H.CloseIcon))
    }
}
