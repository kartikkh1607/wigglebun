package com.kartikkhandelwal.wigglebun.domain.rewards

import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.DoneBreak
import com.kartikkhandelwal.wigglebun.domain.sitting.BreakDays
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class Streak(
    /** Work days in a row that met the goal, ending today (if met) or the last finished work day. */
    val days: Int,
    /** First day of the current streak; null when [days] is 0. */
    val startDate: LocalDate?,
    /** How many today's goal needs (see [DailyGoal.count]). */
    val goal: Int,
    /** Today's progress toward [goal] (see [DailyGoal.progress]). */
    val todayDone: Int,
    val todayIsWorkDay: Boolean,
) {
    val todayCounts: Boolean get() = todayIsWorkDay && todayDone >= goal
}

/**
 * The work-day streak (CLAUDE.md §5.4). Pure: days are break days ([dayOf], see [BreakDays]).
 * - A work day counts when its breaks meet the goal it had that day ([goalOn]).
 * - Non-work days are skipped: they never break the streak and never add to it.
 * - Today counts as soon as it meets the goal; until then it is in progress, not missed.
 * - A finished work day below the goal ends the streak. Days before the first logged break
 *   don't count as missed (nothing to judge before the app was used).
 */
object StreakCalculator {

    fun compute(
        done: List<DoneBreak>,
        zone: ZoneId,
        today: LocalDate,
        isWorkDay: (LocalDate) -> Boolean,
        /** The goal each day had, so a change to the rule never re-judges a past day (see [DailyGoalHistory]). */
        goalOn: (LocalDate) -> DailyGoal,
        /** The break day of each break; calendar dates in [zone] unless a night shift says otherwise. */
        dayOf: (Instant) -> LocalDate = { it.atZone(zone).toLocalDate() },
    ): Streak {
        val breaksByDate: Map<LocalDate, List<BreakType>> = done
            .groupBy(keySelector = { dayOf(it.at) }, valueTransform = { it.type })
        fun progress(date: LocalDate) = goalOn(date).progress(breaksByDate[date].orEmpty())

        val goal = goalOn(today).count
        val todayIsWorkDay = isWorkDay(today)
        val todayDone = progress(today)
        val firstDay = breaksByDate.keys.minOrNull()
            ?: return Streak(days = 0, startDate = null, goal = goal, todayDone = 0, todayIsWorkDay = todayIsWorkDay)

        var days = 0
        var start: LocalDate? = null
        var date = today
        while (!date.isBefore(firstDay)) {
            if (isWorkDay(date)) {
                val met = progress(date) >= goalOn(date).count
                if (met) {
                    days++
                    start = date
                } else if (date != today) {
                    break
                }
            }
            date = date.minusDays(1)
        }
        return Streak(days, start, goal, todayDone, todayIsWorkDay)
    }
}
