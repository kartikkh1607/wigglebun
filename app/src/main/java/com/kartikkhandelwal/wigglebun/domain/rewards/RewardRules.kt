package com.kartikkhandelwal.wigglebun.domain.rewards

import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import java.time.LocalDate

/** A streak length that pays a one-off bonus. */
data class Milestone(val days: Int, val bonus: Int)

/** A bonus a streak has earned and the ledger key that pays it at most once. */
data class StreakBonus(val milestone: Milestone, val key: String)

/**
 * Reward numbers and ledger keys (CLAUDE.md §5.4). The milestone amounts are placeholders:
 * they are an open decision (CLAUDE.md §15).
 */
object RewardRules {
    /** Distinct break types a work day needs to count for the streak. */
    const val DAILY_STREAK_GOAL = 4

    /**
     * Streak length in work days → bonus sparks. Each pays once per lifetime, so breaking a streak
     * on purpose and rebuilding it never earns them again.
     */
    val MILESTONES: List<Milestone> = listOf(
        Milestone(days = 3, bonus = 20),
        Milestone(days = 7, bonus = 50),
        Milestone(days = 14, bonus = 100),
        Milestone(days = 30, bonus = 250),
    )

    /** After the last milestone, every further [REPEAT_EVERY_DAYS] of the same streak pays [REPEAT_BONUS]. */
    const val REPEAT_EVERY_DAYS = 30
    const val REPEAT_BONUS = 250

    /** Never more than the break types the user has switched on (toggles arrive in M9). */
    fun dailyGoal(enabledTypeCount: Int): Int = minOf(DAILY_STREAK_GOAL, enabledTypeCount).coerceAtLeast(1)

    fun milestonesReached(streakDays: Int): List<Milestone> = MILESTONES.filter { it.days <= streakDays }

    /**
     * Every bonus a streak of [streakDays] that began on [streakStart] has earned so far: the
     * lifetime milestones (keyed by length only) and, from day 60, one repeat bonus per 30 days
     * (keyed by this streak's start, so only an unbroken streak reaches them).
     */
    fun bonusesFor(streakDays: Int, streakStart: LocalDate): List<StreakBonus> {
        val lifetime = milestonesReached(streakDays).map { StreakBonus(it, milestoneKey(it.days)) }
        val lastMilestone = MILESTONES.last().days
        val repeats = (lastMilestone + REPEAT_EVERY_DAYS..streakDays step REPEAT_EVERY_DAYS).map { days ->
            StreakBonus(Milestone(days, REPEAT_BONUS), repeatKey(days, streakStart))
        }
        return lifetime + repeats
    }

    /** Only the first completion of a type each day pays. */
    fun breakKey(date: LocalDate, type: BreakType): String = "break:$date:${type.name}"

    /** A milestone pays once per lifetime. */
    fun milestoneKey(days: Int): String = "milestone:$days"

    /** A repeat bonus pays once per streak (a new streak has a new start date). */
    fun repeatKey(days: Int, streakStart: LocalDate): String = "$LEGACY_STREAK_PREFIX$days:$streakStart"

    /**
     * Up to DB v3, milestones were keyed `streak:<days>:<start>` (once per streak); the v4 migration
     * moves each milestone's first payment to [milestoneKey]. Repeat bonuses (60, 90, … days) use
     * the same shape, which never collides: no old key has more than 30 days.
     */
    const val LEGACY_STREAK_PREFIX = "streak:"
}
