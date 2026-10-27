package com.kartikkhandelwal.wigglebun.domain.rewards

import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import java.time.LocalDate

/** A streak length that pays a one-off bonus. */
data class Milestone(val days: Int, val bonus: Int)

/**
 * Reward numbers and ledger keys (CLAUDE.md §5.4). The milestone amounts are placeholders:
 * prices are an open decision (CLAUDE.md §15).
 */
object RewardRules {
    /** Distinct break types a work day needs to count for the streak. */
    const val DAILY_STREAK_GOAL = 4

    /** Streak length in work days → bonus sparks. Each pays once per streak. */
    val MILESTONES: List<Milestone> = listOf(
        Milestone(days = 3, bonus = 20),
        Milestone(days = 7, bonus = 50),
        Milestone(days = 14, bonus = 100),
        Milestone(days = 30, bonus = 250),
    )

    /** Never more than the break types the user has switched on (toggles arrive in M9). */
    fun dailyGoal(enabledTypeCount: Int): Int = minOf(DAILY_STREAK_GOAL, enabledTypeCount).coerceAtLeast(1)

    fun milestonesReached(streakDays: Int): List<Milestone> = MILESTONES.filter { it.days <= streakDays }

    /** Only the first completion of a type each day pays. */
    fun breakKey(date: LocalDate, type: BreakType): String = "break:$date:${type.name}"

    /** A milestone pays once per streak; a new streak (new start date) can earn it again. */
    fun streakKey(days: Int, streakStart: LocalDate): String = "streak:$days:$streakStart"
}
