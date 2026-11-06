package com.kartikkhandelwal.wigglebun.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.domain.sitting.WorkSchedule
import com.kartikkhandelwal.wigglebun.ui.home.formatTime
import com.kartikkhandelwal.wigglebun.ui.home.minuteOfDay
import com.kartikkhandelwal.wigglebun.ui.pet.PetSpecs
import com.kartikkhandelwal.wigglebun.ui.pet.PetView
import com.kartikkhandelwal.wigglebun.ui.theme.ClockSun
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.NightRoomMoon
import com.kartikkhandelwal.wigglebun.ui.theme.NightRoomStar
import com.kartikkhandelwal.wigglebun.ui.theme.NightRoomWall
import com.kartikkhandelwal.wigglebun.ui.theme.OnboardingSceneInk
import com.kartikkhandelwal.wigglebun.ui.theme.SparkEdge
import com.kartikkhandelwal.wigglebun.ui.theme.Sun
import java.time.LocalTime

private val P = Dimens.OnboardingPanel

/** The stars in the bar as fractions of its width and height (Hours.dc.html: x 58..344 of 30..360). */
private val BarStars = listOf(0.085f to 0.29f, 0.285f to 0.82f, 0.795f to 0.24f, 0.95f to 0.82f)

/** The moon's bite: the mockup puts the second circle 4 right and 3 up of the first. */
private const val MOON_BITE_X = 4f
private const val MOON_BITE_Y = -3f

/**
 * The scene above the work hours question: the pet, and the day as a bar of 24 hours with the
 * hours the pet is awake in sun-yellow (design/ui_v2/Hours.dc.html). The bar follows the answer;
 * overnight hours light both ends. [scale] shrinks the scene (see [OnboardingPanelScreen]).
 */
@Composable
internal fun HoursScene(draft: WorkHoursDraft, petName: String, scale: Float) {
    val awake = draft.awakeRanges()
    val barHeight = P.ClockBarHeight * scale
    // A column, not fixed offsets: the labels grow with the font and must never be cut off.
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(P.HoursPetTop * scale))
        PetView(
            energy = 1f,
            mood = Mood.HAPPY,
            spec = PetSpecs.Bun,
            name = petName,
            modifier = Modifier.size(P.HoursPetSize * scale),
        )
        Spacer(Modifier.height((P.ClockBarTop - P.HoursPetTop - P.HoursPetSize) * scale))
        // The bar is a picture of the two rows below it, which say the same in words.
        Canvas(
            Modifier
                .padding(horizontal = P.ClockBarInset)
                .fillMaxWidth()
                .height(barHeight)
                .clearAndSetSemantics {},
        ) { drawDay(awake, edge = P.ClockBarEdge.toPx() * scale) }
        Spacer(Modifier.height((P.ClockLabelsTop - P.ClockBarTop - P.ClockBarHeight) * scale))
        ClockLabels(draft = draft, modifier = Modifier, barInset = P.ClockBarInset)
    }
}

/** The 24 hours as a pill: dark night, yellow where the pet is awake, a sun and a moon where there's room. */
private fun DrawScope.drawDay(awake: List<Pair<Int, Int>>, edge: Float) {
    val width = size.width
    val height = size.height
    fun x(minute: Int) = width * minute / WorkSchedule.MINUTES_PER_DAY
    val asleep = complement(awake)
    val shape = Path().apply { addRoundRect(RoundRect(0f, 0f, width, height, CornerRadius(height / 2))) }
    clipPath(shape) {
        drawRect(NightRoomWall, size = size)
        awake.forEach { (from, to) ->
            drawRect(Sun, Offset(x(from), 0f), Size(x(to) - x(from), height))
            drawRect(SparkEdge, Offset(x(from), height - edge), Size(x(to) - x(from), edge))
        }
    }
    val room = P.ClockSymbolRoom.toPx()
    val unit = height / P.ClockBarHeight.toPx()
    awake.forEach { (from, to) -> if (x(to) - x(from) >= room) drawSun(Offset((x(from) + x(to)) / 2, height / 2), unit) }
    asleep.forEach { (from, to) -> if (x(to) - x(from) >= room) drawMoon(Offset((x(from) + x(to)) / 2, height / 2), unit) }
    BarStars.forEach { (fx, fy) ->
        val minute = (fx * WorkSchedule.MINUTES_PER_DAY).toInt()
        if (asleep.any { (from, to) -> minute in from..to && x(to) - x(from) >= room }) {
            drawCircle(NightRoomStar, radius = STAR_RADIUS * unit * density, center = Offset(width * fx, height * fy))
        }
    }
}

private const val STAR_RADIUS = 1.5f

private fun DrawScope.drawSun(centre: Offset, unit: Float) {
    val radius = P.ClockSunRadius.toPx() * unit
    drawCircle(ClockSun, radius, centre)
    val inner = radius + 3.5f * density * unit
    val outer = radius + 6.5f * density * unit
    val ray = Stroke(width = P.ClockSunRay.toPx() * unit, cap = StrokeCap.Round)
    listOf(0f to -1f, 0f to 1f, -1f to 0f, 1f to 0f).forEach { (dx, dy) ->
        drawLine(
            ClockSun,
            Offset(centre.x + dx * inner, centre.y + dy * inner),
            Offset(centre.x + dx * outer, centre.y + dy * outer),
            strokeWidth = ray.width,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawMoon(centre: Offset, unit: Float) {
    val radius = P.ClockMoonRadius.toPx() * unit
    drawCircle(NightRoomMoon, radius, centre)
    drawCircle(NightRoomWall, radius - 1f * density * unit, Offset(centre.x + MOON_BITE_X * density * unit, centre.y + MOON_BITE_Y * density * unit))
}

/** The stretches of the day not in [ranges], which are sorted and don't touch. */
internal fun complement(ranges: List<Pair<Int, Int>>): List<Pair<Int, Int>> {
    val day = WorkSchedule.MINUTES_PER_DAY
    val gaps = mutableListOf<Pair<Int, Int>>()
    var at = 0
    ranges.sortedBy { it.first }.forEach { (from, to) ->
        if (from > at) gaps += at to from
        at = to
    }
    if (at < day) gaps += at to day
    return gaps
}

/**
 * The hours the pet is awake, as minute ranges within one day: one for 9–18, two for 22–06 (the
 * start of the day up to the end, and the start up to midnight).
 */
internal fun WorkHoursDraft.awakeRanges(): List<Pair<Int, Int>> {
    val day = WorkSchedule.MINUTES_PER_DAY
    val end = if (endMinute == 0) day else endMinute
    return when {
        startMinute < end -> listOf(startMinute to end)
        startMinute > end -> listOf(0 to end, startMinute to day)
        else -> emptyList()
    }
}

/** Where a label sits against the spot it points at. */
private enum class LabelAlign { START, CENTRE, END }

private class ClockLabel(val minute: Int, val text: String, val align: LabelAlign)

/**
 * "12 AM", the start, the end and "12 AM" under the bar (Hours.dc.html). Each start and end label is
 * centred under its edge of the awake stretch; the two midnights sit at the ends. A label that would
 * touch one already placed is left out (9–10 puts start and end next to each other), start first.
 */
@Composable
private fun ClockLabels(draft: WorkHoursDraft, modifier: Modifier, barInset: Dp) {
    val midnight = formatTime(LocalTime.MIDNIGHT)
    val labels = remember(draft.startMinute, draft.endMinute) {
        listOf(
            draft.startMinute to LabelAlign.CENTRE,
            draft.endMinute to LabelAlign.CENTRE,
            0 to LabelAlign.START,
            WorkSchedule.MINUTES_PER_DAY to LabelAlign.END,
        )
    }
    val gap = with(LocalDensity.current) { P.ClockLabelGap.roundToPx() }
    val barInsetPx = with(LocalDensity.current) { barInset.roundToPx() }
    val edgeInsetPx = with(LocalDensity.current) { P.ClockLabelsInset.roundToPx() }
    val entries = labels.map { (minute, align) ->
        ClockLabel(minute, if (minute % WorkSchedule.MINUTES_PER_DAY == 0) midnight else formatTime(minuteOfDay(minute)), align)
    }
    Layout(
        content = {
            entries.forEach {
                Text(it.text, style = MaterialTheme.typography.labelMedium, color = OnboardingSceneInk, maxLines = 1, softWrap = false)
            }
        },
        modifier = modifier.fillMaxWidth().clearAndSetSemantics {},
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        val barWidth = constraints.maxWidth - 2 * barInsetPx
        val placed = mutableListOf<IntRange>()
        val spots = mutableListOf<Pair<Placeable, Int>>()
        placeables.forEachIndexed { index, placeable ->
            val entry = entries[index]
            val target = barInsetPx + barWidth * entry.minute / WorkSchedule.MINUTES_PER_DAY
            val left = when (entry.align) {
                LabelAlign.START -> edgeInsetPx
                LabelAlign.END -> constraints.maxWidth - edgeInsetPx - placeable.width
                LabelAlign.CENTRE -> (target - placeable.width / 2).coerceIn(edgeInsetPx, constraints.maxWidth - edgeInsetPx - placeable.width)
            }
            val span = (left - gap)..(left + placeable.width + gap)
            if (placed.none { it.first <= span.last && span.first <= it.last }) {
                placed += left..(left + placeable.width)
                spots += placeable to left
            }
        }
        layout(constraints.maxWidth, placeables.maxOf { it.height }) {
            spots.forEach { (placeable, left) -> placeable.place(left, 0) }
        }
    }
}
