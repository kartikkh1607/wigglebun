package com.kartikkhandelwal.wigglebun.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.domain.sitting.SittingSettings
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButton
import com.kartikkhandelwal.wigglebun.ui.components.StatusChip
import com.kartikkhandelwal.wigglebun.ui.pet.PetSpecs
import com.kartikkhandelwal.wigglebun.ui.pet.PetView
import com.kartikkhandelwal.wigglebun.ui.theme.Card
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.InkEdge
import com.kartikkhandelwal.wigglebun.ui.theme.InkMuted
import com.kartikkhandelwal.wigglebun.ui.theme.IntervalHill
import com.kartikkhandelwal.wigglebun.ui.theme.OnboardingCard
import com.kartikkhandelwal.wigglebun.ui.theme.OnboardingSceneInk
import com.kartikkhandelwal.wigglebun.ui.theme.SliderTrack
import com.kartikkhandelwal.wigglebun.ui.theme.TimerTired
import com.kartikkhandelwal.wigglebun.ui.theme.Track
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunShapes
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTextStyles
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme
import kotlin.math.roundToInt

private val S = Dimens.Onboarding
private val P = Dimens.OnboardingPanel

private const val INTERVAL_STEP = 5

/** Slider ticks between the ends: 20, 25, … 90 minutes. */
private val SliderSteps = (SittingSettings.MAX_INTERVAL_MINUTES - SittingSettings.MIN_INTERVAL_MINUTES) / INTERVAL_STEP - 1

/** The sitting pet's energy in the picture: the mockup's fill line sits a quarter of the way up. */
private const val SITTING_ENERGY = 0.25f

@Composable
fun IntervalScreenRoute(
    minutes: Int,
    petName: String,
    onPicked: (Int) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IntervalScreen(minutes, petName, onPicked, onBack, onContinue, onSkip, modifier)
}

/**
 * One question: how long before a break? (CLAUDE.md §5.1, §6.2, design/ui_v2/Interval.dc.html). The
 * answer is a slider on the screen, with the pet after a break and after sitting above it.
 */
@Composable
fun IntervalScreen(
    minutes: Int,
    petName: String,
    onPicked: (Int) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingPanelScreen(
        step = 3,
        title = stringResource(R.string.interval_title),
        body = stringResource(R.string.interval_body),
        panelHeight = P.IntervalPanelHeight,
        onBack = onBack,
        onSkip = onSkip,
        onContinue = onContinue,
        modifier = modifier,
        scene = { scale -> IntervalScene(minutes, petName, scale) },
    ) {
        IntervalCard(minutes, onPicked)
    }
}

/** The pet after a break and after sitting, with a dashed arrow between them. */
@Composable
private fun IntervalScene(minutes: Int, petName: String, scale: Float) {
    // The pets start below Back and Skip, which grow with the font and would otherwise sit on top of them.
    val fontScale = LocalDensity.current.fontScale
    val top = maxOf(P.IntervalPetsTop * scale, P.TopButtonsClear + P.TopButtonsClearPerFont * (fontScale - 1f).coerceAtLeast(0f))
    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = top)
                .drawBehind {
                    // Two hills under the pets, centred under each (the columns sit 94 dp either side of the middle).
                    val radiusX = P.IntervalHillRadiusX.toPx() * scale
                    val radiusY = P.IntervalHillRadiusY.toPx() * scale
                    val centreY = (P.IntervalPetSize * scale + P.IntervalHillOffset * scale).toPx()
                    listOf(-1, 1).forEach { side ->
                        val centreX = size.width / 2 + side * P.IntervalColumnCentre.toPx()
                        drawOval(IntervalHill, Offset(centreX - radiusX, centreY - radiusY), Size(radiusX * 2, radiusY * 2))
                    }
                },
            verticalAlignment = Alignment.Top,
        ) {
            ScenePet(R.string.interval_after_break, null, energy = 1f, mood = Mood.HAPPY, petName = petName, scale = scale)
            Arrow(scale)
            ScenePet(R.string.interval_after_sitting, minutes, energy = SITTING_ENERGY, mood = Mood.STIFF, petName = petName, scale = scale)
        }
    }
}

@Composable
private fun ScenePet(label: Int, minutes: Int?, energy: Float, mood: Mood, petName: String, scale: Float) {
    Column(Modifier.width(P.IntervalPetColumn), horizontalAlignment = Alignment.CenterHorizontally) {
        PetView(energy = energy, mood = mood, spec = PetSpecs.Bun, name = petName, modifier = Modifier.size(P.IntervalPetSize * scale))
        // One line, as drawn, at normal font sizes (it is wider than the pet's column and may stick out
        // of it); a big font would have the two labels run into each other, so there it wraps.
        val oneLine = LocalDensity.current.fontScale <= P.LargeFontScale
        Text(
            if (minutes == null) stringResource(label) else stringResource(label, minutes),
            style = WigglebunTextStyles.Bubble.copy(fontSize = P.IntervalLabelSize),
            color = Ink,
            textAlign = TextAlign.Center,
            softWrap = !oneLine,
            modifier = Modifier
                .then(if (oneLine) Modifier.wrapContentWidth(Alignment.CenterHorizontally, unbounded = true) else Modifier)
                .padding(top = P.IntervalPetLabelGap * scale)
                // A pill for one line; once it wraps, a pill would turn into a blob.
                .clip(if (oneLine) WigglebunShapes.Pill else WigglebunShapes.Bubble)
                .background(Card)
                .padding(horizontal = P.IntervalLabelPaddingHorizontal, vertical = P.IntervalLabelPaddingVertical),
        )
    }
}

/** The dashed arrow from "after a break" to "after sitting". */
@Composable
private fun Arrow(scale: Float) {
    Canvas(
        Modifier
            .padding(start = P.IntervalArrowGap, end = P.IntervalArrowGap, top = P.IntervalArrowDrop * scale)
            .size(P.IntervalArrowWidth, P.IntervalArrowHeight)
            .clearAndSetSemantics {},
    ) {
        val unit = size.width / ARROW_VIEW_WIDTH
        val path = Path().apply {
            moveTo(3f * unit, 12f * unit)
            lineTo(36f * unit, 12f * unit)
            moveTo(28f * unit, 4f * unit)
            lineTo(37f * unit, 12f * unit)
            lineTo(28f * unit, 20f * unit)
        }
        val dash = 5f * unit
        drawPath(
            path,
            OnboardingSceneInk,
            style = Stroke(
                width = 3.2f * unit,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
            ),
        )
    }
}

private const val ARROW_VIEW_WIDTH = 44f

/** "45 min", whether that's the recommended one, and the slider (20 to 90 minutes in steps of 5). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IntervalCard(minutes: Int, onPicked: (Int) -> Unit) {
    val minutesText = stringResource(R.string.interval_minutes, minutes)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(P.IntervalCardRadius))
            .background(OnboardingCard)
            .padding(
                start = P.IntervalCardPaddingHorizontal,
                end = P.IntervalCardPaddingHorizontal,
                top = P.IntervalCardPaddingTop,
                bottom = P.IntervalCardPaddingBottom,
            ),
        verticalArrangement = Arrangement.spacedBy(P.IntervalCardGap),
    ) {
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(P.IntervalChipGap),
        ) {
            Text(minutesText, style = MaterialTheme.typography.displayMedium.copy(fontSize = P.IntervalValueSize), color = Ink)
            if (minutes == SittingSettings.DEFAULT_INTERVAL_MINUTES) StatusChip(stringResource(R.string.interval_recommended))
        }
        IntervalSlider(minutes, minutesText, onPicked)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.interval_minutes, SittingSettings.MIN_INTERVAL_MINUTES), style = MaterialTheme.typography.labelMedium, color = InkMuted)
            Text(stringResource(R.string.interval_minutes, SittingSettings.MAX_INTERVAL_MINUTES), style = MaterialTheme.typography.labelMedium, color = InkMuted)
        }
    }
}

/**
 * The slider of Interval.dc.html: a thin track filled in the tired colour up to a white thumb with an
 * Ink ring. The rounded track and thumb are drawn here; stepping, dragging and TalkBack come from
 * [Slider], which says the minutes ("45 min") instead of a percentage.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntervalSlider(minutes: Int, minutesText: String, onPicked: (Int) -> Unit) {
    val range = SittingSettings.MIN_INTERVAL_MINUTES.toFloat()..SittingSettings.MAX_INTERVAL_MINUTES.toFloat()
    val thumbSize = P.SliderThumb + P.SliderThumbRing * 2
    Slider(
        value = minutes.toFloat(),
        onValueChange = { onPicked(it.roundToInt()) },
        valueRange = range,
        steps = SliderSteps,
        modifier = Modifier.fillMaxWidth().height(P.SliderHeight).semantics { stateDescription = minutesText },
        thumb = {
            Box(
                Modifier
                    .size(thumbSize)
                    .drawBehind { drawCircle(Ink, radius = size.minDimension / 2, center = center + Offset(0f, P.SliderThumbDrop.toPx())) }
                    .background(Card, CircleShape)
                    .border(P.SliderThumbRing, Ink, CircleShape),
            )
        },
        track = { state ->
            val fraction = (state.value - range.start) / (range.endInclusive - range.start)
            // Slider keeps its track half a thumb in from each end; the mockup's runs the full width,
            // so the track is widened back by that much on both sides.
            val reachPx = with(LocalDensity.current) { thumbSize.toPx() / 2 }
            Canvas(
                Modifier
                    .height(P.SliderTrackHeight)
                    .layout { measurable, constraints ->
                        val widened = measurable.measure(constraints.copy(minWidth = constraints.maxWidth + 2 * reachPx.roundToInt(), maxWidth = constraints.maxWidth + 2 * reachPx.roundToInt()))
                        layout(constraints.maxWidth, widened.height) { widened.place(-reachPx.roundToInt(), 0) }
                    },
            ) {
                val radius = CornerRadius(size.height / 2)
                // The thumb's centre travels between half a thumb in from each end.
                val reach = reachPx
                drawRoundRect(SliderTrack, size = size, cornerRadius = radius)
                drawRoundRect(TimerTired, size = Size(reach + fraction * (size.width - 2 * reach), size.height), cornerRadius = radius)
            }
        },
    )
}

/** The picker Settings still opens for "Remind me every": a slider in a bottom sheet. */
@Composable
internal fun IntervalSheetContent(initial: Int, onDone: (Int) -> Unit, modifier: Modifier = Modifier) {
    var minutes by rememberSaveable { mutableIntStateOf(initial) }
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = S.SheetHorizontal, end = S.SheetHorizontal, bottom = S.SheetBottom),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(S.SheetGap),
    ) {
        Text(stringResource(R.string.interval_sheet), style = MaterialTheme.typography.titleLarge, color = Ink, textAlign = TextAlign.Center)
        Text(stringResource(R.string.interval_minutes, minutes), style = MaterialTheme.typography.displayMedium, color = Ink)
        Slider(
            value = minutes.toFloat(),
            onValueChange = { minutes = it.roundToInt() },
            valueRange = SittingSettings.MIN_INTERVAL_MINUTES.toFloat()..SittingSettings.MAX_INTERVAL_MINUTES.toFloat(),
            steps = SliderSteps,
            colors = SliderDefaults.colors(
                thumbColor = Ink,
                activeTrackColor = Ink,
                activeTickColor = Track,
                inactiveTrackColor = Track,
                inactiveTickColor = InkEdge,
            ),
        )
        ChunkyButton(text = stringResource(R.string.sheet_done), onClick = { onDone(minutes) }, modifier = Modifier.fillMaxWidth())
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun IntervalPreview() {
    WigglebunTheme { IntervalScreen(SittingSettings.DEFAULT_INTERVAL_MINUTES, "Bun", {}, {}, {}, {}) }
}

/** The shortest and longest answers: the slider's ends, and no "Recommended" chip. */
@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun IntervalShortestPreview() {
    WigglebunTheme { IntervalScreen(SittingSettings.MIN_INTERVAL_MINUTES, "Bun", {}, {}, {}, {}) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun IntervalLongestPreview() {
    WigglebunTheme { IntervalScreen(SittingSettings.MAX_INTERVAL_MINUTES, "Bun", {}, {}, {}, {}) }
}

@Preview(widthDp = 360, heightDp = 640)
@Composable
internal fun IntervalSmallPhonePreview() {
    WigglebunTheme { IntervalScreen(SittingSettings.DEFAULT_INTERVAL_MINUTES, "WWWWWWWWWWWW", {}, {}, {}, {}) }
}

@Preview(widthDp = 360, heightDp = 640, fontScale = 1.3f)
@Composable
internal fun IntervalSmallLargeFontPreview() {
    WigglebunTheme { IntervalScreen(SittingSettings.DEFAULT_INTERVAL_MINUTES, "Bun", {}, {}, {}, {}) }
}

@Preview(widthDp = 360, heightDp = 640, fontScale = 2f)
@Composable
internal fun IntervalSmallHugeFontPreview() {
    WigglebunTheme { IntervalScreen(SittingSettings.DEFAULT_INTERVAL_MINUTES, "Bun", {}, {}, {}, {}) }
}
