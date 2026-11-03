package com.kartikkhandelwal.wigglebun.domain.rewards

import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import java.time.LocalDate

/** What a work day needs to count for the streak (CLAUDE.md §5.4). */
sealed interface DailyGoal {
    /** How many are needed. */
    val count: Int

    /** How far a day's completed breaks (repeats included) get toward [count]. */
    fun progress(done: List<BreakType>): Int

    /** This many completed breaks of any types, whichever types are switched on. A repeat counts too. */
    data class Breaks(override val count: Int) : DailyGoal {
        override fun progress(done: List<BreakType>): Int = done.size
    }

    /** The first rule: this many different break types. Days from before the switch keep it. */
    data class DifferentTypes(override val count: Int) : DailyGoal {
        override fun progress(done: List<BreakType>): Int = done.toSet().size
    }
}

/** From [from] on, a work day needed [goal] to count for the streak. */
data class DailyGoalChange(val from: LocalDate, val goal: DailyGoal)

/**
 * The streak's daily goal over time, so a change to the rule applies from the day of the change
 * and never re-judges a past day (like [com.kartikkhandelwal.wigglebun.domain.sitting.WorkDayHistory]
 * for work days).
 */
data class DailyGoalHistory(
    /** Sorted by [DailyGoalChange.from]. Days before the first change had [BEFORE_HISTORY]. */
    val changes: List<DailyGoalChange> = emptyList(),
) {
    fun goalOn(date: LocalDate): DailyGoal = changes.lastOrNull { !it.from.isAfter(date) }?.goal ?: BEFORE_HISTORY

    /** The history after the goal becomes [goal] on [today]; a same-day re-change replaces today's entry. */
    fun withChange(goal: DailyGoal, today: LocalDate): DailyGoalHistory {
        if (goal == goalOn(today)) return this
        return DailyGoalHistory(changes.filter { it.from.isBefore(today) } + DailyGoalChange(today, goal))
    }

    companion object {
        /** The rule installs had before the history existed: 4 different break types. */
        val BEFORE_HISTORY: DailyGoal = DailyGoal.DifferentTypes(4)
    }
}
