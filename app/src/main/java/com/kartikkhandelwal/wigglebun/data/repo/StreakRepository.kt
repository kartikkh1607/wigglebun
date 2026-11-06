package com.kartikkhandelwal.wigglebun.data.repo

import com.kartikkhandelwal.wigglebun.data.prefs.SettingsRepository
import com.kartikkhandelwal.wigglebun.data.time.AppClock
import com.kartikkhandelwal.wigglebun.domain.model.DoneBreak
import com.kartikkhandelwal.wigglebun.domain.rewards.DailyGoalHistory
import com.kartikkhandelwal.wigglebun.domain.rewards.Streak
import com.kartikkhandelwal.wigglebun.domain.rewards.StreakCalculator
import com.kartikkhandelwal.wigglebun.domain.sitting.BreakDays
import com.kartikkhandelwal.wigglebun.domain.sitting.WorkDayHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** What Home, Settings, the profile and the celebration show about the streak. */
data class StreakSummary(
    /** Work days in a row that met the daily goal (see [Streak.days]). */
    val days: Int,
    /** Today's progress toward [goal]. */
    val todayDone: Int,
    val goal: Int,
    val todayIsWorkDay: Boolean,
    /** Every completed break so far, repeats included. */
    val totalBreaks: Int,
)

/** Everything the streak and the weekly chart are worked out from, as of today's break day. */
class BreakHistory(
    val done: List<DoneBreak>,
    /** Today's break day (see [BreakDays]). */
    val today: LocalDate,
    val zone: ZoneId,
    val days: BreakDays,
    private val workDays: WorkDayHistory,
    val goals: DailyGoalHistory,
) {
    /** A date a change of hours skipped has no time, so it can't be a missed work day. */
    fun isWorkDay(date: LocalDate): Boolean = days.exists(date) && workDays.isWorkDay(date)

    val streak: Streak by lazy { StreakCalculator.compute(done, zone, today, ::isWorkDay, goals::goalOn, days::dayOf) }

    val summary: StreakSummary
        get() = StreakSummary(streak.days, streak.todayDone, streak.goal, streak.todayIsWorkDay, done.size)
}

/**
 * The streak, kept in one place (it used to be worked out by the profile alone). Recomputed when a
 * break is logged, the break day changes, or the work days or the goal rule change; the sit status
 * ticks every second, but only its break day is used.
 */
@Singleton
class StreakRepository @Inject constructor(
    sitRepository: SitRepository,
    breakRepository: BreakRepository,
    settingsRepository: SettingsRepository,
    private val clock: AppClock,
) {
    /** Today's break day; compared with the (data class) boundary history, so a tick doesn't count. */
    private val breakDay = combine(sitRepository.status.filterNotNull(), settingsRepository.dayBoundaryHistory) { status, boundaries ->
        BreakDays(boundaries, clock.zone).dayOf(status.now) to boundaries
    }
        .distinctUntilChanged()
        .map { (today, boundaries) -> today to BreakDays(boundaries, clock.zone) }

    val history: Flow<BreakHistory> = combine(
        breakRepository.observeDoneBreaks(),
        breakDay,
        settingsRepository.workDayHistory,
        settingsRepository.dailyGoalHistory,
    ) { done, (today, days), workDays, goals ->
        BreakHistory(done, today, clock.zone, days, workDays, goals)
    }

    val summary: Flow<StreakSummary> = history.map { it.summary }.distinctUntilChanged()
}
