package com.kartikkhandelwal.wigglebun.ui.breaksession

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButton
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButtonStyle
import com.kartikkhandelwal.wigglebun.ui.components.ChunkySurface
import com.kartikkhandelwal.wigglebun.ui.components.TopIcon
import com.kartikkhandelwal.wigglebun.ui.components.TopIconButton
import com.kartikkhandelwal.wigglebun.ui.iconRes
import com.kartikkhandelwal.wigglebun.ui.labelRes
import com.kartikkhandelwal.wigglebun.ui.theme.Card
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.InkMuted
import com.kartikkhandelwal.wigglebun.ui.theme.SessionChip
import com.kartikkhandelwal.wigglebun.ui.theme.SessionDisc
import com.kartikkhandelwal.wigglebun.ui.theme.SessionNoteMark
import com.kartikkhandelwal.wigglebun.ui.theme.SessionNoteTile
import com.kartikkhandelwal.wigglebun.ui.theme.SessionProgress
import com.kartikkhandelwal.wigglebun.ui.theme.SessionStepLabel
import com.kartikkhandelwal.wigglebun.ui.theme.SessionTrack
import com.kartikkhandelwal.wigglebun.ui.theme.Track
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunShapes
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTextStyles
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val S = Dimens.BreakSession

private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60

/** Close (X) · the break's icon and name · an empty slot as wide as Close, like the mockup. */
@Composable
internal fun SessionHeader(type: BreakType, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(S.HeaderGap),
    ) {
        TopIconButton(TopIcon.Close, onClick = onClose)
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { BreakPill(type) }
        // Keeps the pill centred; with a big font the pill needs the room more than the centring.
        if (LocalDensity.current.fontScale <= S.LargeFontScale) Spacer(Modifier.width(Dimens.Shared.TopButtonSize))
    }
}

/** "Stand up · 1 min" with the break's full-colour icon on a white chunky pill. */
@Composable
private fun BreakPill(type: BreakType) {
    val title = stringResource(R.string.session_title_duration, stringResource(type.labelRes), type.durationMinutes)
    ChunkySurface(shape = WigglebunShapes.Pill, faceColor = Card, edgeHeight = S.PillEdge) {
        Row(
            Modifier.padding(start = S.PillPaddingStart, end = S.PillPaddingEnd, top = S.PillPaddingVertical, bottom = S.PillPaddingVertical),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(S.HeaderGap),
        ) {
            // Full-colour icon, drawn as it is (CLAUDE.md §11).
            Icon(painterResource(type.iconRes), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(S.PillIcon))
            Text(title, style = WigglebunTextStyles.Sparks.copy(fontSize = 15.sp), color = Ink, textAlign = TextAlign.Center)
        }
    }
}

/**
 * One bar per step: finished steps full, the current one filled to [currentProgress], the rest
 * empty. Silent for TalkBack (the step label says it).
 */
@Composable
internal fun StepBars(count: Int, currentIndex: Int, currentProgress: Float, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(S.SegmentGap),
    ) {
        repeat(count) { index ->
            val fill = when {
                index < currentIndex -> 1f
                index == currentIndex -> currentProgress.coerceIn(0f, 1f)
                else -> 0f
            }
            Box(Modifier.weight(1f).height(S.SegmentHeight).clip(WigglebunShapes.Pill).background(SessionTrack)) {
                if (fill > 0f) Box(Modifier.fillMaxHeight().fillMaxWidth(fill).background(SessionProgress))
            }
        }
    }
}

/**
 * The soft disc, the track and the step's progress arc from 12 o'clock, [content] (the pet) in the
 * middle. [ringAlpha] dims the ring alone (paused); the disc and the pet stay as they are.
 */
@Composable
internal fun CountdownRing(
    progress: Float,
    modifier: Modifier = Modifier,
    ringAlpha: Float = 1f,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize().clearAndSetSemantics {}) {
            drawCircle(SessionDisc, radius = size.minDimension * S.DiscRadiusFraction)
            drawSparkles()
        }
        // alpha() composites track + arc as one layer, so the arc doesn't show the track through it.
        Canvas(Modifier.matchParentSize().alpha(ringAlpha).clearAndSetSemantics {}) {
            // The stroke scales with the ring so a shrunken ring keeps the mockup's proportions.
            val stroke = S.RingStroke.toPx() * (size.minDimension / S.RingSize.toPx())
            val radius = size.minDimension * S.RingRadiusFraction
            drawCircle(SessionTrack, radius = radius, style = Stroke(stroke))
            if (progress > 0f) {
                drawArc(
                    color = SessionProgress,
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

/** Two white four-point sparkles in the disc, at the mockup's spots (58, 70) and (246, 98) of 300. */
private fun DrawScope.drawSparkles() {
    val unit = size.minDimension / 300f
    sparkle(Offset(58f * unit, 76f * unit), 8f * unit)
    sparkle(Offset(246f * unit, 102f * unit), 6f * unit)
}

private fun DrawScope.sparkle(centre: Offset, arm: Float) {
    val waist = arm * 0.3f
    val path = Path().apply {
        moveTo(centre.x, centre.y - arm)
        lineTo(centre.x + waist, centre.y - waist)
        lineTo(centre.x + arm, centre.y)
        lineTo(centre.x + waist, centre.y + waist)
        lineTo(centre.x, centre.y + arm)
        lineTo(centre.x - waist, centre.y + waist)
        lineTo(centre.x - arm, centre.y)
        lineTo(centre.x - waist, centre.y - waist)
        close()
    }
    drawPath(path, Card)
}

/**
 * "0:12" in a white pill, with a chip beside it: "Colour coming back!", then "All the colour is
 * back!", or "Paused". Wraps under the pill when a big font leaves no room beside it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TimerRow(
    remainingMillis: Long,
    isPaused: Boolean,
    colourIsBack: Boolean,
    modifier: Modifier = Modifier,
    timerAlpha: Float = 1f,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(S.HeaderGap, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(S.HeaderGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        ChunkySurface(shape = WigglebunShapes.Pill, faceColor = Card, edgeHeight = S.TimerEdge) {
            CountdownText(
                remainingMillis,
                Modifier.padding(horizontal = S.TimerPaddingHorizontal, vertical = S.TimerPaddingVertical).alpha(timerAlpha),
            )
        }
        val chip = when {
            isPaused -> stringResource(R.string.session_paused)
            colourIsBack -> stringResource(R.string.session_colour_back)
            else -> stringResource(R.string.session_colour_coming)
        }
        Text(
            text = chip,
            style = WigglebunTextStyles.Count.copy(fontWeight = FontWeight.ExtraBold),
            color = Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(WigglebunShapes.Pill)
                .background(if (isPaused) Track else SessionChip)
                .padding(horizontal = S.ChipPaddingHorizontal, vertical = S.ChipPaddingVertical)
                // Paused is announced politely ("Paused"); the colour chip just reads as text.
                .semantics { if (isPaused) liveRegion = LiveRegionMode.Polite },
        )
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
 * "STEP 2 OF 4", the step's title, its text and (on the first step of a moving break) the gentle
 * note. A polite live region, so TalkBack announces each new step ("Step 2 of 4. Reach up high.
 * Stretch…") without interrupting.
 */
@Composable
internal fun InstructionText(
    stepLabel: String,
    title: String,
    instruction: String,
    showSafetyNote: Boolean,
    modifier: Modifier = Modifier,
) {
    val announcement = stringResource(R.string.cd_step_instruction, stepLabel, title, instruction)
    Column(
        modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            liveRegion = LiveRegionMode.Polite
            contentDescription = announcement
        },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stepLabel.uppercase(), style = WigglebunTextStyles.SessionStepLabel, color = SessionStepLabel)
        Text(title, style = WigglebunTextStyles.SessionInstructionTitle, color = Ink)
        Text(instruction, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp), color = InkMuted)
        if (showSafetyNote) SafetyNote()
    }
}

@Composable
private fun SafetyNote() {
    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(S.NoteGap)) {
        Canvas(Modifier.size(S.NoteIcon).clearAndSetSemantics {}) {
            val unit = size.minDimension / 24f
            drawCircle(SessionNoteTile, radius = 10f * unit)
            drawLine(SessionNoteMark, Offset(12f * unit, 7f * unit), Offset(12f * unit, 13f * unit), strokeWidth = 2.6f * unit, cap = StrokeCap.Round)
            drawCircle(SessionNoteMark, radius = 1.5f * unit, center = Offset(12f * unit, 16.8f * unit))
        }
        Text(stringResource(R.string.session_safety), style = MaterialTheme.typography.labelMedium, color = InkMuted)
    }
}

/** Pause/Resume (a fixed 112 dp) + Next step; on the last step Next becomes a green Done. */
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
            style = ChunkyButtonStyle.Secondary,
            // At 0:00 on the last step there is nothing left to pause.
            enabled = !isFinished,
            modifier = Modifier.width(S.PauseWidth),
        )
        SessionButton(
            text = stringResource(if (isLastStep) R.string.session_done else R.string.session_next),
            onClick = if (isLastStep) onDone else onNext,
            style = if (isLastStep) ChunkyButtonStyle.Done else ChunkyButtonStyle.Primary,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * The Pause slot is narrow, so the label shrinks (never grows, never below
 * [WigglebunTextStyles.SessionButtonMinFontSize]) instead of being cut off on small phones or
 * with large fonts.
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
        shape = WigglebunShapes.SessionButton,
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

/** Slots of [SessionLayout]. */
internal enum class SessionSlot { Top, Ring, Timer, Panel }

/**
 * Stretch.dc.html's stack: the header and step bars on top, the ring with the timer pill resting on
 * its foot, the white panel at the bottom. The panel gets the room it asks for, but the ring never
 * drops below [Dimens.BreakSession.RingMinScale] of its size, so on a short phone or with a big font the
 * ring shrinks first and then the panel's text scrolls. Free room (a tall phone) centres the ring
 * between the bars and the panel.
 */
@Composable
internal fun SessionLayout(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val loose = constraints.copy(minWidth = width, minHeight = 0)
        fun slot(slot: SessionSlot) = measurables.first { it.layoutId == slot }

        val top = slot(SessionSlot.Top).measure(loose)
        val timer = slot(SessionSlot.Timer).measure(loose)
        val ringFull = S.RingSize.roundToPx()
        val ringMin = (ringFull * S.RingMinScale).roundToInt()
        val ringTop = S.RingTop.roundToPx()
        // The timer overlaps the ring's box by this much (scaled with the ring below).
        fun overlap(ring: Int) = (S.TimerOverlap.toPx() * ring / ringFull).roundToInt()
        val timerGap = S.TimerGap.roundToPx()
        val fixed = top.height + ringTop + timer.height + timerGap
        val panelMax = max(0, height - fixed - ringMin + overlap(ringMin))
        val panel = slot(SessionSlot.Panel).measure(loose.copy(maxHeight = panelMax))

        // Room for the ring: what is left above the panel, less the timer that hangs below it.
        val room = height - panel.height - top.height - ringTop - timer.height - timerGap
        var ringSize = min(ringFull, max(ringMin, room + overlap(ringFull)))
        // Solve for the overlap at the final size (it shrinks with the ring).
        ringSize = min(ringFull, max(ringMin, room + overlap(ringSize)))
        val ring = slot(SessionSlot.Ring).measure(Constraints.fixed(ringSize, ringSize))

        val block = ringSize + timer.height - overlap(ringSize)
        val band = height - panel.height - top.height - timerGap
        val ringY = top.height + max(ringTop, (band - block) / 2)
        layout(width, height) {
            top.placeRelative(0, 0)
            ring.placeRelative((width - ringSize) / 2, ringY)
            timer.placeRelative(0, ringY + ringSize - overlap(ringSize))
            panel.placeRelative(0, height - panel.height)
        }
    }
}

/** Marks a child of [SessionLayout]. */
internal fun Modifier.sessionSlot(slot: SessionSlot): Modifier = this.layoutId(slot)
