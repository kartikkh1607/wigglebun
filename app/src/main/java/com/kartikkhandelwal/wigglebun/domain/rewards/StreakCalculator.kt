package com.kartikkhandelwal.wigglebun.domain.rewards

import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.DoneBreak
import java.time.LocalDate
import java.time.ZoneId

data class Streak(
    /** Work days in a row that met the goal, ending today (if met) or the last finished work day. */
    val days: Int,
    /** First day of the current streak; null when [days] is 0. */
    val startDate: LocalDate?,
    val goal: Int,
    /** Distinct break types done today. */
    val todayDistinct: Int,
    val todayIsWorkDay: Boolean,
) {
    val todayCounts: Boolean get() = todayIsWorkDay && todayDistinct >= goal
}

/**
 * The work-day streak (CLAUDE.md §5.4). Pure: days are local calendar dates in [zone].
 * - A work day counts when it has at least [goal] distinct break types done.
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
        goal: Int,
    ): Streak {
        val typesByDate: Map<LocalDate, Set<BreakType>> = done
            .groupBy(keySelector = { it.at.atZone(zone).toLocalDate() }, valueTransform = { it.type })
            .mapValues { it.value.toSet() }
        val todayIsWorkDay = isWorkDay(today)
        val todayDistinct = typesByDate[today]?.size ?: 0
        val firstDay = typesByDate.keys.minOrNull()
            ?: return Streak(days = 0, startDate = null, goal = goal, todayDistinct = 0, todayIsWorkDay = todayIsWorkDay)

        var days = 0
        var start: LocalDate? = null
        var date = today
        while (!date.isBefore(firstDay)) {
            if (isWorkDay(date)) {
                val met = (typesByDate[date]?.size ?: 0) >= goal
                if (met) {
                    days++
                    start = date
                } else if (date != today) {
                    break
                }
            }
            date = date.minusDays(1)
        }
        return Streak(days, start, goal, todayDistinct, todayIsWorkDay)
    }
}
