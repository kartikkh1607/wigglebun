package com.kartikkhandelwal.wigglebun.domain.rewards

import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import java.time.LocalDate

/**
 * How many days of breaks the next pet is away (Collection's "About 3 more days of breaks"; UI v2
 * decision D4): the sparks still missing ÷ the user's own pace, the average sparks a day over the
 * last [RECENT_DAYS] break days that had a break. Days without a break don't count, so a weekend
 * or a holiday doesn't slow the estimate. A new user, with no break day yet, gets [NEW_USER_PACE].
 *
 * The pace counts break rewards only (ledger keys `break:<day>:<type>`). Streak bonuses are
 * one-offs, and one landing in the window would promise the next pet far too soon.
 */
object SparksPace {
    const val RECENT_DAYS = 7

    /** 15 sparks: a day that just meets the streak goal with 5-spark breaks (the "Light" user in EconomyTest). */
    val NEW_USER_PACE: Int = RewardRules.DAILY_GOAL.count * BreakType.entries.minOf { it.defaultReward }

    /**
     * Sparks a day, as a fraction, over the [RECENT_DAYS] most recent days in [earnedByDay]
     * (break day → break rewards paid that day); [NEW_USER_PACE] when it is empty.
     */
    fun perDay(earnedByDay: Map<LocalDate, Int>): Double {
        val recent = earnedByDay.filterValues { it > 0 }.toSortedMap().values.toList().takeLast(RECENT_DAYS)
        return if (recent.isEmpty()) NEW_USER_PACE.toDouble() else recent.sum().toDouble() / recent.size
    }

    /** Days of breaks to earn [missing] more sparks at [perDay], rounded up; 0 when nothing is missing. */
    fun daysToEarn(missing: Int, perDay: Double): Int {
        if (missing <= 0) return 0
        return kotlin.math.ceil(missing / perDay.coerceAtLeast(1.0)).toInt()
    }

    /**
     * The break day of each `break:<day>:<type>` ledger key and what it paid, summed per day.
     * Keys that don't parse (none today) are skipped.
     */
    fun earnedByDay(breakRewards: List<Pair<String, Int>>): Map<LocalDate, Int> =
        breakRewards.mapNotNull { (key, amount) -> RewardRules.dayOfBreakKey(key)?.let { it to amount } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, amounts) -> amounts.sum() }
}
