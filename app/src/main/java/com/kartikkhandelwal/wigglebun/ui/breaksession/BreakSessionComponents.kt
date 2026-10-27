package com.kartikkhandelwal.wigglebun.ui.breaksession

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButton
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButtonStyle
import com.kartikkhandelwal.wigglebun.ui.components.ChunkySurface
import com.kartikkhandelwal.wigglebun.ui.theme.Card
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.SessionAccent
import com.kartikkhandelwal.wigglebun.ui.theme.SessionEdge
import com.kartikkhandelwal.wigglebun.ui.theme.SessionMuted
import com.kartikkhandelwal.wigglebun.ui.theme.SessionSegmentIdle
import com.kartikkhandelwal.wigglebun.ui.theme.SessionTrack
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunShapes
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTextStyles

private val S = Dimens.BreakSession

/** Mockup ring: radius 130 inside a 280 box (stroke 12). */
private const val RING_RADIUS_FRACTION = 130f / 280f
private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60

/** Close (X) · title + "Step X of Y" · empty slot, like the mockup's 48 / 1fr / 48 grid. */
@Composable
internal fun SessionHeader(title: String, stepLabel: String?, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CloseButton(onClose)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = WigglebunTextStyles.SessionTitle, color = Ink, textAlign = TextAlign.Center)
            if (stepLabel != null) {
                Text(stepLabel, style = WigglebunTextStyles.Count, color = SessionMuted, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.width(S.CloseSize))
    }
}

@Composable
private fun CloseButton(onClose: () -> Unit) {
    val description = stringResource(R.string.cd_close_break)
    ChunkySurface(
        shape = WigglebunShapes.IconTile,
        faceColor = Card,
        edgeColor = SessionEdge,
        edgeHeight = S.CloseEdge,
        onClick = onClose,
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(S.CloseSize, S.CloseSize + S.CloseEdge)
            .semantics { contentDescription = description },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_close), contentDescription = null, tint = Ink, modifier = Modifier.size(S.CloseIcon))
        }
    }
}

/** One bar per step (or per break needed for the streak); the first [filled] are coloured. */
@Composable
internal fun Segments(count: Int, filled: Int, modifier: Modifier = Modifier, filledColor: Color = SessionAccent) {
    Row(
        modifier = modifier.fillMaxWidth().clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(S.SegmentGap),
    ) {
        repeat(count) { index ->
            Box(
                Modifier
                    .weight(1f)
                    .height(S.SegmentHeight)
                    .clip(WigglebunShapes.Pill)
                    .background(if (index < filled) filledColor else SessionSegmentIdle),
            )
        }
    }
}

/** Track circle with the step's progress arc from 12 o'clock, [content] (the pet) in the middle. */
@Composable
internal fun CountdownRing(progress: Float, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize().clearAndSetSemantics {}) {
            val stroke = S.RingStroke.toPx()
            val radius = size.minDimension * RING_RADIUS_FRACTION
            drawCircle(SessionTrack, radius = radius, style = Stroke(stroke))
            if (progress > 0f) {
                drawArc(
                    color = SessionAccent,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceAtMost(1f),
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

/** "0:11"; TalkBack reads "11 seconds left" when focused (not a live region: no per-second chatter). */
@Composable
internal fun CountdownText(remainingMillis: Long, modifier: Modifier = Modifier) {
    // Round up, so a step shows its full length at the start and 0:00 only when it is over.
    val totalSeconds = ((remainingMillis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).toInt()
    val minutes = totalSeconds / SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    val description = timeLeftDescription(minutes, seconds)
    Text(
        text = stringResource(R.string.session_countdown, minutes, seconds),
        style = WigglebunTextStyles.Countdown,
        color = Ink,
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
    )
}

@Composable
private fun timeLeftDescription(minutes: Int, seconds: Int): String {
    val minutesText = pluralStringResource(R.plurals.cd_minutes, minutes, minutes)
    val secondsText = pluralStringResource(R.plurals.cd_seconds, seconds, seconds)
    val amount = when {
        minutes > 0 && seconds > 0 -> stringResource(R.string.cd_minutes_seconds, minutesText, secondsText)
        minutes > 0 -> minutesText
        else -> secondsText
    }
    return stringResource(R.string.cd_time_left, amount)
}

/**
 * The step's instruction on a white chunky card. A polite live region, so TalkBack announces
 * each new step ("Step 2 of 4. Slowly tilt…") without interrupting.
 */
@Composable
internal fun InstructionCard(stepLabel: String, instruction: String, showSafetyNote: Boolean, modifier: Modifier = Modifier) {
    val announcement = stringResource(R.string.cd_step_instruction, stepLabel, instruction)
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        ChunkySurface(
            shape = WigglebunShapes.Card,
            faceColor = Card,
            edgeColor = SessionEdge,
            edgeHeight = S.CardEdge,
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = announcement
                },
        ) {
            Text(
                text = instruction,
                style = WigglebunTextStyles.Instruction,
                color = Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = S.CardPaddingHorizontal, vertical = S.CardPaddingVertical),
            )
        }
        if (showSafetyNote) {
            Text(
                text = stringResource(R.string.session_safety),
                style = MaterialTheme.typography.bodyMedium,
                color = SessionMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = S.SafetyTop),
            )
        }
    }
}

/** Pause/Resume (1) + Next step (2); on the last step Next becomes a green Done. */
@Composable
internal fun SessionButtons(
    isPaused: Boolean,
    isLastStep: Boolean,
    isFinished: Boolean,
    onTogglePause: () -> Unit,
    onNext: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(S.ButtonsGap)) {
        SessionButton(
            text = stringResource(if (isPaused) R.string.session_resume else R.string.session_pause),
            onClick = onTogglePause,
            style = ChunkyButtonStyle.SessionSecondary,
            // At 0:00 on the last step there is nothing left to pause.
            enabled = !isFinished,
            modifier = Modifier.weight(1f),
        )
        SessionButton(
            text = stringResource(if (isLastStep) R.string.session_done else R.string.session_next),
            onClick = if (isLastStep) onDone else onNext,
            style = if (isLastStep) ChunkyButtonStyle.Done else ChunkyButtonStyle.Primary,
            modifier = Modifier.weight(2f),
        )
    }
}

/**
 * The mockup's 1 : 2 split leaves the Pause/Resume slot narrow, so the label shrinks (never
 * grows, never below [WigglebunTextStyles.SessionButtonMinFontSize]) instead of being cut off on
 * small phones or with large fonts.
 */
@Composable
private fun SessionButton(
    text: String,
    onClick: () -> Unit,
    style: ChunkyButtonStyle,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val labelStyle = MaterialTheme.typography.labelLarge
    ChunkyButton(
        onClick = onClick,
        style = style,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = S.ButtonPaddingHorizontal),
        modifier = modifier.heightIn(min = S.ButtonHeight + style.edgeHeight),
    ) {
        Text(
            text = text,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(
                minFontSize = WigglebunTextStyles.SessionButtonMinFontSize,
                maxFontSize = labelStyle.fontSize,
            ),
        )
    }
}
