package com.kartikkhandelwal.wigglebun.domain.rewards

import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.DoneBreak
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DailyGoalHistoryTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val monday = LocalDate.of(2026, 10, 26)

    private fun done(date: LocalDate, vararg types: BreakType) =
        types.map { DoneBreak(it, date.atTime(12, 0).atZone(zone).toInstant()) }

    @Test
    fun theDefaultGoalAppliesUntilAChange() {
        assertEquals(RewardRules.DAILY_STREAK_GOAL, DailyGoalHistory().goalOn(monday))
    }

    @Test
    fun aChangeAppliesFromThatDayOn() {
        val history = DailyGoalHistory().withChange(3, monday)
        assertEquals(RewardRules.DAILY_STREAK_GOAL, history.goalOn(monday.minusDays(1)))
        assertEquals(3, history.goalOn(monday))
        assertEquals(3, history.goalOn(monday.plusDays(5)))
    }

    @Test
    fun aSecondChangeTheSameDayReplacesTheFirst() {
        val history = DailyGoalHistory().withChange(3, monday).withChange(2, monday)
        assertEquals(listOf(DailyGoalChange(monday, 2)), history.changes)
    }

    @Test
    fun noChangeWritesNothing() {
        assertEquals(DailyGoalHistory(), DailyGoalHistory().withChange(RewardRules.DAILY_STREAK_GOAL, monday))
    }

    /**
     * Three types on for three days, then a fourth switched on. Judging the past by today's goal
     * of 4 would wipe out the streak. It keeps the three days, and today counts once it meets 4.
     */
    @Test
    fun switchingATypeOnDoesNotBreakTheStreak() {
        val today = monday.plusDays(3)
        val threeTypes = arrayOf(BreakType.WALK, BreakType.NECK, BreakType.WATER)
        val goals = DailyGoalHistory().withChange(3, monday).withChange(4, today)
        val past = done(monday, *threeTypes) + done(monday.plusDays(1), *threeTypes) + done(monday.plusDays(2), *threeTypes)

        val before = StreakCalculator.compute(past, zone, today, { true }, goals.goalOn(today), goals::goalOn)
        assertEquals(3, before.days)
        assertEquals(4, before.goal)

        val after = StreakCalculator.compute(
            past + done(today, *threeTypes, BreakType.EYES),
            zone,
            today,
            { true },
            goals.goalOn(today),
            goals::goalOn,
        )
        assertEquals(4, after.days)
    }
}
