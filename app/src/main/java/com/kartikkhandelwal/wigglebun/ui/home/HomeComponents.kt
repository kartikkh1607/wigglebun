package com.kartikkhandelwal.wigglebun.ui.home

import android.text.format.DateFormat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.layout.Layout
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
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.domain.model.AccessoryId
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButton
import com.kartikkhandelwal.wigglebun.ui.components.ChunkySurface
import com.kartikkhandelwal.wigglebun.ui.components.PillCard
import com.kartikkhandelwal.wigglebun.ui.components.SparkIcon
import com.kartikkhandelwal.wigglebun.ui.components.SparksPill
import com.kartikkhandelwal.wigglebun.ui.components.SpeechBubble
import com.kartikkhandelwal.wigglebun.ui.iconRes
import com.kartikkhandelwal.wigglebun.ui.labelRes
import com.kartikkhandelwal.wigglebun.ui.pet.PetNameSentence
import com.kartikkhandelwal.wigglebun.ui.pet.PetSpec
import com.kartikkhandelwal.wigglebun.ui.pet.PetView
import com.kartikkhandelwal.wigglebun.ui.theme.ButtonBorder
import com.kartikkhandelwal.wigglebun.ui.theme.ButtonEdge
import com.kartikkhandelwal.wigglebun.ui.theme.Card
import com.kartikkhandelwal.wigglebun.ui.theme.CheckIdle
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.DoneGreen
import com.kartikkhandelwal.wigglebun.ui.theme.DoneGreenEdge
import com.kartikkhandelwal.wigglebun.ui.theme.FlameChip
import com.kartikkhandelwal.wigglebun.ui.theme.FlameChipText
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.InkMuted
import com.kartikkhandelwal.wigglebun.ui.theme.OnSurface
import com.kartikkhandelwal.wigglebun.ui.theme.RewardChip
import com.kartikkhandelwal.wigglebun.ui.theme.StreakSegmentIdle
import com.kartikkhandelwal.wigglebun.ui.theme.Surfaces
import com.kartikkhandelwal.wigglebun.ui.theme.TimerHappy
import com.kartikkhandelwal.wigglebun.ui.theme.TimerStiff
import com.kartikkhandelwal.wigglebun.ui.theme.TimerTired
import com.kartikkhandelwal.wigglebun.ui.theme.Track
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunShapes
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTextStyles
import com.kartikkhandelwal.wigglebun.ui.tint
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

// ---------- Room scene ----------

/**
 * Top of Home (design/ui_v2/Main.dc.html): the room, the menu button and sparks pill on the wall,
 * and the pet on the rug with what it says above its head ([line]; none while it sleeps). The
 * scene is 340 dp below the status bar; at large font sizes it grows at the top, so the bubble
 * never covers the buttons or the pet.
 */
@Composable
fun HomeScene(
    energy: Float,
    mood: Mood,
    pet: PetSpec,
    petName: String,
    line: PetLine?,
    sparks: Int,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier,
    accessories: List<AccessoryId> = emptyList(),
) {
    // Edge-to-edge: the wall runs under the status bar; the mockup layout starts below it.
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    OnSurface(Surfaces.HomeWall) {
        Layout(
            modifier = modifier.fillMaxWidth(),
            content = {
                RoomScene(topInset = topInset)
                Row(Modifier.padding(horizontal = H.TopBarPadding), verticalAlignment = Alignment.CenterVertically) {
                    MenuButton(onOpenMenu)
                    Spacer(Modifier.weight(1f))
                    SparksPill(sparks)
                }
                PetWithLine(energy, mood, pet, petName, line, accessories)
            },
        ) { measurables, constraints ->
            val width = constraints.maxWidth
            val top = topInset.roundToPx()
            val bar = measurables[1].measure(Constraints.fixedWidth(width))
            val petUnit = measurables[2].measure(Constraints(maxWidth = width))
            val barBottom = H.TopBarPadding.roundToPx() + bar.height
            val belowPet = H.PetAboveSceneBottom.roundToPx()
            val scene = maxOf(H.SceneHeight.roundToPx(), barBottom + H.BubbleBelowTopBar.roundToPx() + petUnit.height + belowPet)
            val height = top + scene
            val room = measurables[0].measure(Constraints.fixed(width, height))
            layout(width, height) {
                room.place(0, 0)
                petUnit.place((width - petUnit.width) / 2, height - belowPet - petUnit.height)
                bar.place(0, top + H.TopBarPadding.roundToPx())
            }
        }
    }
}

/**
 * The pet with its speech bubble above its head, the tail pointing at it. TalkBack reads the two
 * as one item ("Bun, energy draining from sitting. Getting sleepy… stretch with me?"): the bubble
 * says what the old mood chip said.
 */
@Composable
private fun PetWithLine(
    energy: Float,
    mood: Mood,
    pet: PetSpec,
    petName: String,
    line: PetLine?,
    accessories: List<AccessoryId>,
) {
    val fontScale = LocalDensity.current.fontScale
    Layout(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        content = {
            PetView(
                energy = energy,
                mood = mood,
                spec = pet,
                name = petName,
                accessories = accessories,
                modifier = Modifier.size(H.PetWidth, H.PetHeight),
            )
            if (line != null) SpeechBubble(stringResource(line.textRes))
        },
    ) { measurables, constraints ->
        val petBox = measurables[0].measure(Constraints())
        // The mockup's width at 100%; larger fonts may use more of the screen before wrapping.
        val maxBubble = minOf(
            (H.BubbleMaxWidth * fontScale.coerceAtLeast(1f)).roundToPx(),
            constraints.maxWidth - 2 * H.BubbleSideMargin.roundToPx(),
        )
        val bubble = measurables.getOrNull(1)?.measure(Constraints(maxWidth = maxBubble.coerceAtLeast(0)))
        val above = bubble?.let { (it.height - H.BubbleIntoPet.roundToPx()).coerceAtLeast(0) } ?: 0
        val width = maxOf(petBox.width, bubble?.width ?: 0)
        layout(width, above + petBox.height) {
            petBox.place((width - petBox.width) / 2, above)
            bubble?.place((width - bubble.width) / 2, 0)
        }
    }
}

private val PetLine.textRes: Int
    get() = when (this) {
        PetLine.HAPPY -> R.string.home_line_happy
        PetLine.TIRED -> R.string.home_line_tired
        PetLine.STIFF -> R.string.home_line_stiff
        PetLine.WALK -> R.string.home_line_walk
        PetLine.CELEBRATE -> R.string.home_line_celebrate
    }

/** The timer bar keeps the mood's colour (D1). */
private val PetLine?.barColor: Color
    get() = when (this) {
        PetLine.TIRED -> TimerTired
        PetLine.STIFF -> TimerStiff
        else -> TimerHappy
    }

@Composable
private fun MenuButton(onClick: () -> Unit) {
    val description = stringResource(R.string.cd_open_menu)
    ChunkySurface(
        shape = RoundedCornerShape(H.MenuButtonRadius),
        faceColor = Card,
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

// ---------- Timer card ----------

/**
 * The card under the room (design/ui_v2/Main.dc.html): minutes since the last break and the
 * timer bar in the mood's colour, or when work starts while asleep; then today's streak goal.
 */
@Composable
fun TimerCard(
    pill: PillState,
    line: PetLine?,
    petName: String,
    today: LocalDate,
    showBreakCta: Boolean,
    streakGoal: StreakGoalUi?,
    onTakeBreak: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PillCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = H.PillPaddingHorizontal,
            end = H.PillPaddingHorizontal,
            top = H.PillPaddingTop,
            bottom = H.PillPaddingBottom,
        ),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(H.PillGap)) {
            when (pill) {
                is PillState.Sitting -> SittingContent(pill, line, showBreakCta, onTakeBreak)
                is PillState.Asleep -> AsleepContent(pill, petName, today)
            }
            if (streakGoal != null) {
                HorizontalDivider(thickness = H.DividerThickness, color = Track)
                StreakGoalRow(streakGoal)
            }
        }
    }
}

@Composable
private fun SittingContent(pill: PillState.Sitting, line: PetLine?, showBreakCta: Boolean, onTakeBreak: () -> Unit) {
    // TalkBack reads "32 min, since your last break" as one item.
    Column(Modifier.semantics(mergeDescendants = true) {}) {
        Text(
            pluralStringResource(R.plurals.home_minutes, pill.minutes, pill.minutes),
            style = WigglebunTextStyles.TimerMinutes,
            color = Ink,
        )
        Text(stringResource(R.string.home_since_last_break), style = MaterialTheme.typography.labelMedium, color = InkMuted)
    }
    ProgressBar(pill.progress, line.barColor)
    if (showBreakCta) {
        ChunkyButton(
            text = stringResource(R.string.home_take_break),
            onClick = onTakeBreak,
            modifier = Modifier.fillMaxWidth().padding(top = H.CtaTopGap),
            wrap = true,
        )
    }
}

/**
 * "Streak goal ▬▬▭ 2 of 3" and the streak's flame chip (hidden at 0). One TalkBack item. At large
 * font sizes the goal's parts wrap; the chip stays on the right.
 */
@Composable
private fun StreakGoalRow(goal: StreakGoalUi) {
    val description = buildString {
        append(stringResource(R.string.cd_home_streak_goal, goal.done, goal.goal))
        if (goal.days > 0) append(' ').append(pluralStringResource(R.plurals.cd_home_streak_days, goal.days, goal.days))
    }
    Row(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(H.PillGap),
    ) {
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(H.StreakGap),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.home_streak_goal), style = WigglebunTextStyles.StrongLabel, color = Ink)
            Row(horizontalArrangement = Arrangement.spacedBy(H.SegmentGap)) {
                repeat(goal.goal) { index ->
                    Box(
                        Modifier
                            .size(H.SegmentWidth, H.SegmentHeight)
                            .clip(WigglebunShapes.Pill)
                            .background(if (index < goal.done) DoneGreen else StreakSegmentIdle),
                    )
                }
            }
            Text(stringResource(R.string.home_streak_progress, goal.done, goal.goal), style = MaterialTheme.typography.labelMedium, color = InkMuted)
        }
        if (goal.days > 0) {
            Row(
                modifier = Modifier
                    .clip(WigglebunShapes.Pill)
                    .background(FlameChip)
                    .padding(start = H.FlamePaddingStart, end = H.FlamePaddingEnd, top = H.FlamePaddingVertical, bottom = H.FlamePaddingVertical),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(H.FlameGap),
            ) {
                Image(painterResource(R.drawable.ic_flame), contentDescription = null, modifier = Modifier.size(H.FlameIcon))
                Text(pluralStringResource(R.plurals.home_streak_days, goal.days, goal.days), style = WigglebunTextStyles.Chip, color = FlameChipText)
            }
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
        PetNameSentence(R.string.home_asleep_title, petName, style = MaterialTheme.typography.titleLarge, color = Ink)
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

// ---------- Breaks list ----------

@Composable
fun BreaksHeader(doneCount: Int, totalCount: Int, modifier: Modifier = Modifier, lastShift: Boolean = false) {
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
            stringResource(if (lastShift) R.string.home_breaks_title_last_shift else R.string.home_breaks_title),
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
        shape = WigglebunShapes.BreakCard,
        faceColor = Card,
        edgeHeight = H.CardEdge,
        onClick = onOpen,
        onClickLabel = stringResource(R.string.cd_open_break),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = H.CardPaddingHorizontal, vertical = H.CardPaddingVertical),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(H.CardGap),
        ) {
            BreakIconTile(card.type, H.IconTileSize, WigglebunShapes.IconTile, H.BreakIcon)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(H.CardTextGap)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Ink)
                when {
                    // What it paid today, in green; the reward chip goes (design/ui_v2/Main.dc.html).
                    card.doneToday -> Text(
                        text = if (card.sparksEarned > 0) {
                            pluralStringResource(R.plurals.home_break_done_sparks, card.sparksEarned, card.sparksEarned)
                        } else {
                            stringResource(R.string.home_break_done)
                        },
                        style = WigglebunTextStyles.Count,
                        color = DoneGreenEdge,
                    )
                    card.skippedToday -> Text(stringResource(R.string.home_break_skipped), style = MaterialTheme.typography.bodySmall, color = InkMuted)
                    else -> Text(
                        pluralStringResource(R.plurals.home_minutes, card.type.durationMinutes, card.type.durationMinutes),
                        style = MaterialTheme.typography.bodySmall,
                        color = InkMuted,
                    )
                }
            }
            if (!card.doneToday) SparkReward(card.type.defaultReward)
            CheckButton(done = card.doneToday, title = title, onClick = onCheck)
        }
    }
}

/** A card's spark reward: star and number on a pale yellow pill, read as "5 sparks". */
@Composable
private fun SparkReward(sparks: Int) {
    val description = pluralStringResource(R.plurals.cd_sparks, sparks, sparks)
    Row(
        modifier = Modifier
            .clearAndSetSemantics { contentDescription = description }
            .clip(WigglebunShapes.Pill)
            .background(RewardChip)
            .padding(start = H.RewardChipPaddingStart, end = H.RewardChipPaddingEnd, top = H.RewardChipPaddingVertical, bottom = H.RewardChipPaddingVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(H.RewardGap),
    ) {
        SparkIcon(H.RewardChipIcon)
        Text(sparks.toString(), style = WigglebunTextStyles.StrongLabel, color = Ink)
    }
}

/** The break's full-colour icon (design/ui_v2/icons) on its light tint. */
@Composable
internal fun BreakIconTile(type: BreakType, size: Dp, shape: Shape, iconSize: Dp) {
    Box(
        modifier = Modifier.size(size).clip(shape).background(type.tint),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(type.iconRes), contentDescription = null, modifier = Modifier.size(iconSize))
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
