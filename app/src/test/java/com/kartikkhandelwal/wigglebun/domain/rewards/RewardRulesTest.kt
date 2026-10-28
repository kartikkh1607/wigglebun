package com.kartikkhandelwal.wigglebun.domain.rewards

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** Streak bonuses (CLAUDE.md §5.4): milestones once per lifetime, then +250 every 30 days. */
class RewardRulesTest {

    private val start = LocalDate.of(2026, 11, 2)

    private fun days(bonuses: List<Milestone>) = bonuses.map { it.days }

    @Test
    fun firstStreakPaysEachMilestoneOnItsDay() {
        val sim = StreakSimulation()
        val paidOn = (1..30).associateWith { sim.keepStreak(1) }.filterValues { it.isNotEmpty() }
        assertEquals(
            mapOf(3 to listOf(Milestone(3, 20)), 7 to listOf(Milestone(7, 50)), 14 to listOf(Milestone(14, 100)), 30 to listOf(Milestone(30, 250))),
            paidOn,
        )
    }

    @Test
    fun restartingAStreakAfterDay30PaysNoMilestonesAgain() {
        val sim = StreakSimulation()
        assertEquals(listOf(3, 7, 14, 30), days(sim.keepStreak(30)))
        sim.miss()
        assertEquals(emptyList<Int>(), days(sim.keepStreak(30)))
    }

    @Test
    fun day60And90OfOneStreakEachPay250() {
        val sim = StreakSimulation()
        sim.keepStreak(30)
        assertEquals(emptyList<Milestone>(), sim.keepStreak(29)) // days 31–59
        assertEquals(listOf(Milestone(60, 250)), sim.keepStreak(1))
        assertEquals(emptyList<Milestone>(), sim.keepStreak(29)) // days 61–89
        assertEquals(listOf(Milestone(90, 250)), sim.keepStreak(1))
    }

    @Test
    fun aStreakBrokenBeforeDay3AndRebuiltStillPaysThe3DayBonusOnce() {
        val sim = StreakSimulation()
        assertEquals(emptyList<Milestone>(), sim.keepStreak(2))
        sim.miss()
        assertEquals(listOf(3), days(sim.keepStreak(3)))
        sim.miss()
        assertEquals(emptyList<Int>(), days(sim.keepStreak(3)))
    }

    /** A second streak still earns its own 60-day bonus: that one is per streak, not per lifetime. */
    @Test
    fun aNewStreakCanEarnTheRepeatBonusButNotTheMilestones() {
        val sim = StreakSimulation()
        sim.keepStreak(30)
        sim.miss()
        assertEquals(listOf(60), days(sim.keepStreak(60)))
    }

    @Test
    fun bonusKeys() {
        assertEquals(emptyList<StreakBonus>(), RewardRules.bonusesFor(2, start))
        assertEquals(
            listOf("milestone:3", "milestone:7", "milestone:14", "milestone:30", "streak:60:2026-11-02", "streak:90:2026-11-02"),
            RewardRules.bonusesFor(119, start).map { it.key },
        )
    }

    @Test
    fun milestonesReachedAreCumulative() {
        assertEquals(emptyList<Milestone>(), RewardRules.milestonesReached(2))
        assertEquals(listOf(3, 7), RewardRules.milestonesReached(13).map { it.days })
        assertEquals(listOf(3, 7, 14, 30), RewardRules.milestonesReached(45).map { it.days })
    }
}
