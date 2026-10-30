package com.kartikkhandelwal.wigglebun.ui.pet.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.domain.model.ActivePet
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.DoneBreak
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.domain.model.PetId
import com.kartikkhandelwal.wigglebun.domain.rewards.DayBar
import com.kartikkhandelwal.wigglebun.domain.rewards.RewardRules
import com.kartikkhandelwal.wigglebun.domain.rewards.WeeklyChart
import com.kartikkhandelwal.wigglebun.domain.sitting.WorkSchedule
import com.kartikkhandelwal.wigglebun.ui.chartColor
import com.kartikkhandelwal.wigglebun.ui.components.ChunkySurface
import com.kartikkhandelwal.wigglebun.ui.labelRes
import com.kartikkhandelwal.wigglebun.ui.pet.PetView
import com.kartikkhandelwal.wigglebun.ui.pet.displayName
import com.kartikkhandelwal.wigglebun.ui.pet.spec
import com.kartikkhandelwal.wigglebun.ui.theme.Card
import com.kartikkhandelwal.wigglebun.ui.theme.CardEdge
import com.kartikkhandelwal.wigglebun.ui.theme.Cream
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.DoneGreen
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.InkMuted
import com.kartikkhandelwal.wigglebun.ui.theme.PetShadow
import com.kartikkhandelwal.wigglebun.ui.theme.SessionAccent
import com.kartikkhandelwal.wigglebun.ui.theme.Track
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunShapes
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTextStyles
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.TextStyle as DayNameStyle

private val P = Dimens.Profile

@Composable
fun PetProfileScreenRoute(modifier: Modifier = Modifier, viewModel: PetProfileViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val loaded = state
    if (loaded == null) {
        Box(modifier.fillMaxSize().background(Cream))
        return
    }
    PetProfileScreen(loaded, modifier)
}

/**
 * Pet profile tab (CLAUDE.md §6.6; no mockup, so Home's card style): the active pet and its name,
 * the streak and total breaks side by side, and the last 7 days stacked by break category.
 */
@Composable
fun PetProfileScreen(state: PetProfileUiState, modifier: Modifier = Modifier) {
    val name = state.pet.displayName()
    Column(
        modifier
            .fillMaxSize()
            .background(Cream)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = Dimens.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.padding(top = P.TopPadding).size(P.PetSize)) {
            Canvas(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = P.PetShadowOffset)
                    .size(P.PetShadowWidth, P.PetShadowHeight),
            ) { drawOval(PetShadow) }
            PetView(
                energy = state.energy,
                mood = state.mood,
                spec = state.pet.spec,
                name = name,
                accessories = state.pet.wearing,
                modifier = Modifier.size(P.PetSize),
            )
        }
        // The name alone: one line, cut with "…" if it must be (TalkBack reads it whole).
        Text(
            name,
            style = WigglebunTextStyles.ScreenTitle,
            color = Ink,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = P.NameTop).semantics { heading() },
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = P.CardsTop).height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(P.CardGap),
        ) {
            StreakCard(state.streak, Modifier.weight(1f).fillMaxHeight())
            TotalCard(state.totalBreaks, Modifier.weight(1f).fillMaxHeight())
        }
        WeekCard(state, Modifier.padding(top = P.CardGap))
        Spacer(Modifier.height(P.ListBottomPadding))
    }
}

@Composable
private fun ProfileCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    ChunkySurface(shape = WigglebunShapes.Card, faceColor = Card, edgeColor = CardEdge, edgeHeight = P.CardEdge, modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(P.CardPadding), verticalArrangement = Arrangement.spacedBy(P.CardLabelGap), content = content)
    }
}

/** "Streak · 3 days · Today 2 of 4" with a bar that turns green once today counts. */
@Composable
private fun StreakCard(streak: StreakCardUi, modifier: Modifier = Modifier) {
    ProfileCard(modifier.semantics(mergeDescendants = true) {}) {
        Text(stringResource(R.string.profile_streak), style = MaterialTheme.typography.labelMedium, color = InkMuted)
        Text(pluralStringResource(R.plurals.profile_streak_days, streak.days, streak.days), style = WigglebunTextStyles.StatNumber, color = Ink)
        if (streak.days == 0) {
            Text(pluralStringResource(R.plurals.profile_streak_hint, streak.goal, streak.goal), style = MaterialTheme.typography.bodySmall, color = InkMuted)
        }
        if (streak.todayIsWorkDay) {
            Text(stringResource(R.string.profile_today_progress, streak.todayDone, streak.goal), style = MaterialTheme.typography.bodySmall, color = Ink)
            Box(
                Modifier
                    .padding(top = P.ProgressTop - P.CardLabelGap)
                    .fillMaxWidth()
                    .height(P.ProgressHeight)
                    .clip(WigglebunShapes.Pill)
                    .background(Track),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(minOf(streak.todayDone, streak.goal) / streak.goal.toFloat())
                        .fillMaxHeight()
                        .clip(WigglebunShapes.Pill)
                        .background(if (streak.todayCounts) DoneGreen else SessionAccent),
                )
            }
        } else {
            Text(stringResource(R.string.profile_day_off), style = MaterialTheme.typography.bodySmall, color = Ink)
        }
    }
}

@Composable
private fun TotalCard(total: Int, modifier: Modifier = Modifier) {
    ProfileCard(modifier.semantics(mergeDescendants = true) {}) {
        Text(stringResource(R.string.profile_total), style = MaterialTheme.typography.labelMedium, color = InkMuted)
        Text(total.toString(), style = WigglebunTextStyles.StatNumber, color = Ink)
        Text(stringResource(R.string.profile_total_since), style = MaterialTheme.typography.bodySmall, color = InkMuted)
    }
}

/**
 * Seven stacked bars (dataviz mark specs: ≤ 24 dp wide, 4 dp rounded tip, square at the
 * baseline, 2 dp surface gaps between segments), the day's total above each bar, and a legend.
 * TalkBack reads one summary of all seven days instead of the bars.
 */
@Composable
private fun WeekCard(state: PetProfileUiState, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val maxTotal = state.week.maxOf { it.total }.coerceAtLeast(1)
    val summary = state.week.map { day ->
        pluralStringResource(R.plurals.profile_chart_day, day.total, day.date.dayOfWeek.getDisplayName(DayNameStyle.FULL, locale), day.total)
    }.joinToString(", ")
    val chartDescription = stringResource(R.string.profile_chart_cd, summary)
    ProfileCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.profile_week_title),
                style = MaterialTheme.typography.titleMedium,
                color = Ink,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            Text(pluralStringResource(R.plurals.profile_week_total, state.weekTotal, state.weekTotal), style = WigglebunTextStyles.Count, color = InkMuted)
        }
        Column(Modifier.padding(top = P.ChartTop).clearAndSetSemantics { contentDescription = chartDescription }) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                state.week.forEach { day ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (day.total > 0) {
                            Text(day.total.toString(), style = MaterialTheme.typography.labelSmall, color = Ink)
                            Spacer(Modifier.height(P.BarLabelGap))
                        }
                        DayBarView(day, maxTotal)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = P.BarLabelGap)) {
                state.week.forEachIndexed { index, day ->
                    val today = index == state.week.lastIndex
                    Text(
                        day.date.dayOfWeek.getDisplayName(DayNameStyle.NARROW, locale),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (today) FontWeight.ExtraBold else FontWeight.Bold),
                        color = if (today) Ink else InkMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        val present = WeeklyChart.STACK_ORDER.filter { category -> state.week.any { (it.counts[category] ?: 0) > 0 } }
        if (present.isEmpty()) {
            Text(
                stringResource(R.string.profile_week_empty),
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted,
                modifier = Modifier.padding(top = P.LegendTop),
            )
        } else {
            FlowRow(
                modifier = Modifier.padding(top = P.LegendTop),
                horizontalArrangement = Arrangement.spacedBy(P.LegendGap),
                verticalArrangement = Arrangement.spacedBy(P.CardLabelGap),
            ) {
                // Reversed, so the legend reads top-down like the stack.
                present.asReversed().forEach { category ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(P.LegendDot).clip(CircleShape).background(category.chartColor))
                        Text(
                            stringResource(category.labelRes),
                            style = MaterialTheme.typography.labelMedium,
                            color = InkMuted,
                            modifier = Modifier.padding(start = P.LegendDotGap),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayBarView(day: DayBar, maxTotal: Int) {
    if (day.total == 0) {
        // A stub on the baseline: "nothing that day", not missing data.
        Box(Modifier.width(P.BarWidth).height(P.EmptyBarHeight).clip(WigglebunShapes.Pill).background(Track))
        return
    }
    val segments = day.segments.asReversed() // top to bottom
    Column(
        Modifier
            .width(P.BarWidth)
            .height(P.BarMaxHeight * (day.total / maxTotal.toFloat()))
            .clip(RoundedCornerShape(topStart = P.BarRadius, topEnd = P.BarRadius)),
    ) {
        segments.forEachIndexed { index, (category, count) ->
            if (index > 0) Spacer(Modifier.height(P.BarGapInStack))
            Box(Modifier.weight(count.toFloat()).fillMaxWidth().background(category.chartColor))
        }
    }
}

/** Sample states for previews and screenshot tests, built through the real mapper. */
internal object PetProfilePreviewStates {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 10, 28) // Wednesday
    private val workDays = WorkSchedule.Default.days
    private val goal = RewardRules.dailyGoal(BreakType.entries.size)

    private fun on(date: LocalDate, vararg types: BreakType): List<DoneBreak> = types.mapIndexed { i, type ->
        DoneBreak(type, Instant.from(date.atTime(10 + i, 0).atZone(zone)))
    }

    /** First day: Bun, nothing done yet. */
    val EmptyWeek = toPetProfileUiState(
        pet = ActivePet.Default,
        mood = Mood.HAPPY,
        energy = 0.9f,
        done = emptyList(),
        today = today,
        zone = zone,
        isWorkDay = { it.dayOfWeek in workDays },
        goal = goal,
    )

    /** Pip, a 4-day streak, 2 of 4 today, 128 breaks in all (105 of them in September). */
    val BusyWeek = run {
        val older = (0 until 105).map { i ->
            DoneBreak(BreakType.entries[i % BreakType.entries.size], Instant.from(LocalDate.of(2026, 9, 1 + i / 5).atTime(9 + i % 5, 0).atZone(zone)))
        }
        val week = on(today.minusDays(6), BreakType.STAND_UP, BreakType.WALK, BreakType.NECK, BreakType.EYES, BreakType.WATER) +
            on(today.minusDays(5), BreakType.STAND_UP, BreakType.NECK, BreakType.SHOULDERS, BreakType.WRIST, BreakType.WATER, BreakType.WALK) +
            on(today.minusDays(3), BreakType.WALK) +
            on(today.minusDays(2), BreakType.STAND_UP, BreakType.WALK, BreakType.EYES, BreakType.WATER, BreakType.NECK) +
            on(today.minusDays(1), BreakType.STAND_UP, BreakType.NECK, BreakType.WRIST, BreakType.EYES) +
            on(today, BreakType.WALK, BreakType.WATER)
        toPetProfileUiState(
            pet = ActivePet(PetId.PIP, chosenName = null),
            mood = Mood.TIRED,
            energy = 0.55f,
            done = older + week,
            today = today,
            zone = zone,
            isWorkDay = { it.dayOfWeek in workDays },
            goal = goal,
        )
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun PetProfileEmptyPreview() {
    WigglebunTheme { PetProfileScreen(PetProfilePreviewStates.EmptyWeek) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun PetProfileBusyPreview() {
    WigglebunTheme { PetProfileScreen(PetProfilePreviewStates.BusyWeek) }
}
