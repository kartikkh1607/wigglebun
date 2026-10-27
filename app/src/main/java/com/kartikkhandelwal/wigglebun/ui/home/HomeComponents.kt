package com.kartikkhandelwal.wigglebun.ui.home

import android.text.format.DateFormat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButton
import com.kartikkhandelwal.wigglebun.ui.components.ChunkySurface
import com.kartikkhandelwal.wigglebun.ui.components.PillCard
import com.kartikkhandelwal.wigglebun.ui.iconRes
import com.kartikkhandelwal.wigglebun.ui.labelRes
import com.kartikkhandelwal.wigglebun.ui.tint
import com.kartikkhandelwal.wigglebun.ui.pet.PetView
import com.kartikkhandelwal.wigglebun.ui.pet.SleepZSide
import com.kartikkhandelwal.wigglebun.ui.theme.ButtonBorder
import com.kartikkhandelwal.wigglebun.ui.theme.ButtonEdge
import com.kartikkhandelwal.wigglebun.ui.theme.Card
import com.kartikkhandelwal.wigglebun.ui.theme.CardEdge
import com.kartikkhandelwal.wigglebun.ui.theme.CheckIdle
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.DoneGreen
import com.kartikkhandelwal.wigglebun.ui.theme.DoneGreenEdge
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.InkMuted
import com.kartikkhandelwal.wigglebun.ui.theme.MoodHappyChip
import com.kartikkhandelwal.wigglebun.ui.theme.MoodStiffChip
import com.kartikkhandelwal.wigglebun.ui.theme.MoodTiredChip
import com.kartikkhandelwal.wigglebun.ui.theme.PetShadow
import com.kartikkhandelwal.wigglebun.ui.theme.TimerHappy
import com.kartikkhandelwal.wigglebun.ui.theme.TimerStiff
import com.kartikkhandelwal.wigglebun.ui.theme.TimerTired
import com.kartikkhandelwal.wigglebun.ui.theme.TopButtonEdge
import com.kartikkhandelwal.wigglebun.ui.theme.Track
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunShapes
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTextStyles
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** List items stagger in this far apart (CLAUDE.md §11). */
internal const val STAGGER_MILLIS = 40L
private const val STAGGER_DURATION_MILLIS = 320
private const val PROGRESS_TWEEN_MILLIS = 600

private val H = Dimens.Home

// ---------- Desk scene ----------

/** Top of Home: desk scene with Bun on the floor, menu button and sparks pill. */
@Composable
fun DeskScene(
    energy: Float,
    mood: Mood,
    petName: String,
    sparks: Int,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Edge-to-edge: the picture runs under the status bar; the mockup layout starts below it.
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(modifier.fillMaxWidth().height(H.SceneHeight + topInset)) {
        Image(
            painter = painterResource(R.drawable.desk_scene),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        Box(Modifier.fillMaxWidth().padding(top = topInset).height(H.SceneHeight)) {
            Canvas(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = H.PetShadowTop)
                    .size(H.PetShadowWidth, H.PetShadowHeight),
            ) { drawOval(PetShadow) }
            PetView(
                energy = energy,
                mood = mood,
                name = petName,
                // The scene's plant pot sits just right of Bun's head; float the z's left over plain wall.
                sleepZSide = SleepZSide.LEFT,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = H.PetTop)
                    .size(H.PetSize),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(H.TopBarPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MenuButton(onOpenMenu)
                Spacer(Modifier.weight(1f))
                SparksPill(sparks)
            }
        }
    }
}

@Composable
private fun MenuButton(onClick: () -> Unit) {
    val description = stringResource(R.string.cd_open_menu)
    ChunkySurface(
        shape = RoundedCornerShape(H.MenuButtonRadius),
        faceColor = Card,
        edgeColor = TopButtonEdge,
        edgeHeight = H.TopButtonEdge,
        onClick = onClick,
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(H.MenuButtonSize, H.MenuButtonSize + H.TopButtonEdge)
            .semantics { contentDescription = description },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_menu), contentDescription = null, tint = Ink, modifier = Modifier.size(H.MenuIcon))
        }
    }
}

@Composable
private fun SparksPill(sparks: Int) {
    val description = pluralStringResource(R.plurals.cd_sparks, sparks, sparks)
    ChunkySurface(
        shape = WigglebunShapes.Pill,
        faceColor = Card,
        edgeColor = TopButtonEdge,
        edgeHeight = H.TopButtonEdge,
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        Row(
            modifier = Modifier
                .height(H.SparksHeight)
                .padding(start = H.SparksPaddingStart, end = H.SparksPaddingEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(H.SparksGap),
        ) {
            SparkIcon(H.SparksIcon)
            Text(sparks.toString(), style = WigglebunTextStyles.Sparks, color = Ink)
        }
    }
}

@Composable
internal fun SparkIcon(size: Dp) {
    Icon(
        painter = painterResource(R.drawable.ic_spark),
        contentDescription = null,
        tint = Color.Unspecified,
        modifier = Modifier.size(size),
    )
}

// ---------- Timer pill ----------

@Composable
fun TimerPill(
    pill: PillState,
    petName: String,
    today: LocalDate,
    showBreakCta: Boolean,
    onTakeBreak: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PillCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = H.PillPaddingHorizontal, vertical = H.PillPaddingVertical),
    ) {
        when (pill) {
            is PillState.Sitting -> SittingContent(pill, petName, showBreakCta, onTakeBreak)
            is PillState.Asleep -> AsleepContent(pill, petName, today)
        }
    }
}

@Composable
private fun SittingContent(pill: PillState.Sitting, petName: String, showBreakCta: Boolean, onTakeBreak: () -> Unit) {
    val (chipBackground, barColor) = pill.chip.colors()
    Column(verticalArrangement = Arrangement.spacedBy(H.PillGap)) {
        // The label gets its own line so the chip never squeezes it (large fonts).
        // TalkBack reads label, minutes and chip as one sentence.
        Column(
            modifier = Modifier.semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(H.PillLabelGap),
        ) {
            Text(stringResource(R.string.home_time_since_break), style = MaterialTheme.typography.labelMedium, color = InkMuted)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(H.CardGap),
            ) {
                Text(
                    pluralStringResource(R.plurals.home_minutes, pill.minutes, pill.minutes),
                    style = MaterialTheme.typography.displayMedium,
                    color = Ink,
                    modifier = Modifier.weight(1f),
                )
            // Both sides may shrink, so long pet names and large fonts wrap instead of clipping.
            Text(
                text = stringResource(pill.chip.textRes(), petName),
                style = WigglebunTextStyles.Chip,
                color = Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(WigglebunShapes.Pill)
                    .background(chipBackground)
                    .padding(horizontal = H.ChipPaddingHorizontal, vertical = H.ChipPaddingVertical),
            )
            }
        }
        ProgressBar(pill.progress, barColor)
        if (showBreakCta) {
            ChunkyButton(
                text = stringResource(R.string.home_take_break),
                onClick = onTakeBreak,
                modifier = Modifier.fillMaxWidth().padding(top = H.CtaTopGap),
            )
        }
    }
}

@Composable
private fun ProgressBar(progress: Float, color: Color) {
    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(PROGRESS_TWEEN_MILLIS, easing = FastOutSlowInEasing),
        label = "sitProgress",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(H.ProgressHeight)
            .clip(WigglebunShapes.Pill)
            .background(Track)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f) },
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .fillMaxHeight()
                .clip(WigglebunShapes.Pill)
                .background(color),
        )
    }
}

@Composable
private fun AsleepContent(pill: PillState.Asleep, petName: String, today: LocalDate) {
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(H.PillLabelGap),
    ) {
        Text(stringResource(R.string.home_asleep_title, petName), style = MaterialTheme.typography.titleLarge, color = Ink)
        Text(workStartsText(pill.nextStart, today), style = MaterialTheme.typography.bodyMedium, color = InkMuted)
    }
}

@Composable
private fun workStartsText(next: LocalDateTime?, today: LocalDate): String {
    if (next == null) return stringResource(R.string.home_no_work_hours)
    val time = formatTime(next.toLocalTime())
    return when (next.toLocalDate()) {
        today -> stringResource(R.string.home_work_starts_today, time)
        today.plusDays(1) -> stringResource(R.string.home_work_starts_tomorrow, time)
        else -> stringResource(
            R.string.home_work_starts_on,
            next.dayOfWeek.getDisplayName(TextStyle.FULL, currentLocale()),
            time,
        )
    }
}

/** A time in the user's 12/24-hour preference, see [formatClockTime]. */
@Composable
internal fun formatTime(time: LocalTime): String =
    formatClockTime(time, DateFormat.is24HourFormat(LocalContext.current), currentLocale())

/** The app's locale, read so a language change recomposes (Locale.getDefault() would not). */
@Composable
private fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

/**
 * "9 AM", "9:30 AM" (12-hour) or "9:00", "18:00" (24-hour). Whole hours drop ":00" in 12-hour
 * form so "9 AM–6 PM" fits the focused card on one line; 24-hour keeps it ("9–18" reads like a
 * number range). The space before AM/PM is non-breaking so a time never splits across lines.
 */
internal fun formatClockTime(time: LocalTime, is24Hour: Boolean, locale: Locale): String {
    val pattern = when {
        is24Hour -> "H:mm"
        time.minute == 0 -> "h a"
        else -> "h:mm a"
    }
    return DateTimeFormatter.ofPattern(pattern, locale).format(time).replace(' ', NO_BREAK_SPACE)
}

private const val NO_BREAK_SPACE = '\u00A0'

/** Minutes after midnight (1440 = midnight) as a time. */
internal fun minuteOfDay(minute: Int): LocalTime = LocalTime.of((minute / 60) % 24, minute % 60)

private fun MoodChip.textRes(): Int = when (this) {
    MoodChip.GREAT -> R.string.home_chip_great
    MoodChip.TIRED -> R.string.home_chip_tired
    MoodChip.STIFF -> R.string.home_chip_stiff
    MoodChip.WALK -> R.string.home_chip_walk
}

/** Chip background and timer bar colour (Main.dc.html). */
private fun MoodChip.colors(): Pair<Color, Color> = when (this) {
    MoodChip.GREAT, MoodChip.WALK -> MoodHappyChip to TimerHappy
    MoodChip.TIRED -> MoodTiredChip to TimerTired
    MoodChip.STIFF -> MoodStiffChip to TimerStiff
}

// ---------- Breaks list ----------

@Composable
fun BreaksHeader(doneCount: Int, totalCount: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = H.HeaderPaddingHorizontal,
                end = H.HeaderPaddingHorizontal,
                top = H.HeaderPaddingTop,
                bottom = H.HeaderPaddingBottom,
            ),
        horizontalArrangement = Arrangement.spacedBy(H.CardGap),
    ) {
        Text(
            stringResource(R.string.home_breaks_title),
            style = MaterialTheme.typography.titleLarge,
            color = Ink,
            modifier = Modifier.weight(1f).alignByBaseline(),
        )
        Text(
            stringResource(R.string.home_breaks_done, doneCount, totalCount),
            style = WigglebunTextStyles.Count,
            color = InkMuted,
            modifier = Modifier.alignByBaseline(),
        )
    }
}

@Composable
fun BreakCard(
    card: BreakCardUi,
    onOpen: () -> Unit,
    onCheck: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(card.type.labelRes)
    ChunkySurface(
        shape = WigglebunShapes.Card,
        faceColor = Card,
        edgeColor = CardEdge,
        edgeHeight = H.CardEdge,
        onClick = onOpen,
        onClickLabel = stringResource(R.string.cd_open_break),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = H.CardPaddingStart, top = H.CardPadding, end = H.CardPadding, bottom = H.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(H.CardGap),
        ) {
            BreakIconTile(card.type, H.IconTileSize, WigglebunShapes.IconTile, H.BreakIcon)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(H.CardTextGap)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Ink)
                Text(
                    text = if (card.skippedToday) {
                        stringResource(R.string.home_break_skipped)
                    } else {
                        pluralStringResource(R.plurals.home_minutes, card.type.durationMinutes, card.type.durationMinutes)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                )
            }
            Reward(card.type.defaultReward)
            CheckButton(done = card.doneToday, title = title, onClick = onCheck)
        }
    }
}

@Composable
internal fun BreakIconTile(type: BreakType, size: Dp, shape: Shape, iconSize: Dp) {
    Box(
        modifier = Modifier.size(size).clip(shape).background(type.tint),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(type.iconRes), contentDescription = null, tint = Ink, modifier = Modifier.size(iconSize))
    }
}

@Composable
internal fun Reward(sparks: Int, modifier: Modifier = Modifier) {
    val description = pluralStringResource(R.plurals.cd_sparks, sparks, sparks)
    Row(
        // "5 sparks" instead of "plus 5" and an unlabelled icon.
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(H.RewardGap),
    ) {
        Text(stringResource(R.string.home_reward, sparks), style = MaterialTheme.typography.titleSmall, color = Ink)
        SparkIcon(H.RewardIcon)
    }
}

/**
 * Chunky check: grey outline until done, then green with a white tick (Main.dc.html). Once done
 * it is a status indicator, not a button: repeat breaks go through the focused card's Done.
 */
@Composable
private fun CheckButton(done: Boolean, title: String, onClick: () -> Unit) {
    val description = if (done) {
        stringResource(R.string.cd_break_done, title)
    } else {
        stringResource(R.string.cd_mark_break_done, title)
    }
    ChunkySurface(
        shape = WigglebunShapes.IconTile,
        faceColor = if (done) DoneGreen else Card,
        edgeColor = if (done) DoneGreenEdge else ButtonEdge,
        edgeHeight = H.CheckEdge,
        border = BorderStroke(Dimens.ButtonBorder, if (done) DoneGreen else ButtonBorder),
        onClick = if (done) null else onClick,
        contentAlignment = Alignment.Center,
        // 52×48 face: at least the 48 dp touch target. Merged so "Neck stretch, done" stays its
        // own TalkBack stop instead of folding into the card's description.
        modifier = Modifier
            .width(H.CheckWidth)
            .height(H.CheckHeight + H.CheckEdge)
            .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = if (done) Card else CheckIdle,
                modifier = Modifier.size(H.CheckIcon),
            )
        }
    }
}

/** Fade + short slide in, [STAGGER_MILLIS] apart (only on first show). */
@Composable
internal fun Modifier.staggerIn(index: Int, enabled: Boolean): Modifier {
    if (!enabled) return this
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * STAGGER_MILLIS)
        progress.animateTo(1f, tween(STAGGER_DURATION_MILLIS, easing = FastOutSlowInEasing))
    }
    val slide = with(LocalDensity.current) { H.StaggerSlide.toPx() }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * slide
    }
}
