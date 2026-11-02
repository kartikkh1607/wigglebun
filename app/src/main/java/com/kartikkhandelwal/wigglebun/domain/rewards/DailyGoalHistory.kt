package com.kartikkhandelwal.wigglebun.domain.rewards

import java.time.LocalDate

/** From [from] on, a day needed [goal] break types to count for the streak. */
data class DailyGoalChange(val from: LocalDate, val goal: Int)

/**
 * The streak's daily goal over time. It depends on how many break types are switched on, so each
 * past day is judged by the goal back then: switching a type on never breaks a streak retroactively
 * (like [com.kartikkhandelwal.wigglebun.domain.sitting.WorkDayHistory] for work days).
 */
data class DailyGoalHistory(
    /** Sorted by [DailyGoalChange.from]. Empty means every type has always been on. */
    val changes: List<DailyGoalChange> = emptyList(),
) {
    fun goalOn(date: LocalDate): Int =
        changes.lastOrNull { !it.from.isAfter(date) }?.goal ?: RewardRules.DAILY_STREAK_GOAL

    /** The history after the goal becomes [goal] on [today]; a same-day re-change replaces today's entry. */
    fun withChange(goal: Int, today: LocalDate): DailyGoalHistory {
        if (goal == goalOn(today)) return this
        return DailyGoalHistory(changes.filter { it.from.isBefore(today) } + DailyGoalChange(today, goal))
    }
}
