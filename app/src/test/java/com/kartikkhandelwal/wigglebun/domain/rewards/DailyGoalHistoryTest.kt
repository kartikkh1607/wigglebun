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
        types.mapIndexed { i, type -> DoneBreak(type, date.atTime(9 + i, 0).atZone(zone).toInstant()) }

    @Test
    fun theCurrentRuleIsThreeCompletedBreaks() {
        assertEquals(DailyGoal.Breaks(3), RewardRules.DAILY_GOAL)
    }

    /** Installs from before the history existed had 4 different types. */
    @Test
    fun beforeAnyChangeTheOriginalRuleApplies() {
        assertEquals(DailyGoal.DifferentTypes(4), DailyGoalHistory().goalOn(monday))
    }

    @Test
    fun aChangeAppliesFromThatDayOn() {
        val history = DailyGoalHistory().withChange(DailyGoal.Breaks(3), monday)
        assertEquals(DailyGoalHistory.BEFORE_HISTORY, history.goalOn(monday.minusDays(1)))
        assertEquals(DailyGoal.Breaks(3), history.goalOn(monday))
        assertEquals(DailyGoal.Breaks(3), history.goalOn(monday.plusDays(5)))
    }

    @Test
    fun aSecondChangeTheSameDayReplacesTheFirst() {
        val history = DailyGoalHistory().withChange(DailyGoal.Breaks(3), monday).withChange(DailyGoal.Breaks(2), monday)
        assertEquals(listOf(DailyGoalChange(monday, DailyGoal.Breaks(2))), history.changes)
    }

    @Test
    fun noChangeWritesNothing() {
        assertEquals(DailyGoalHistory(), DailyGoalHistory().withChange(DailyGoalHistory.BEFORE_HISTORY, monday))
        val adopted = DailyGoalHistory().withChange(RewardRules.DAILY_GOAL, monday)
        assertEquals(adopted, adopted.withChange(RewardRules.DAILY_GOAL, monday.plusDays(3)))
    }

    /** Completed breaks count repeats; the old rule counted each type once. */
    @Test
    fun theTwoRulesCountDifferently() {
        val day = listOf(BreakType.WATER, BreakType.WATER, BreakType.NECK)
        assertEquals(3, DailyGoal.Breaks(3).progress(day))
        assertEquals(2, DailyGoal.DifferentTypes(4).progress(day))
    }

    /**
     * Mon and Tue had 4 different types under the old rule; the new rule arrives on Wednesday,
     * where 3 waters are enough. The streak runs through all three days.
     */
    @Test
    fun theStreakCarriesOverTheRuleChange() {
        val wednesday = monday.plusDays(2)
        val goals = DailyGoalHistory().withChange(RewardRules.DAILY_GOAL, wednesday)
        val fourTypes = arrayOf(BreakType.WALK, BreakType.NECK, BreakType.WATER, BreakType.EYES)
        val breaks = done(monday, *fourTypes) + done(monday.plusDays(1), *fourTypes) +
            done(wednesday, BreakType.WATER, BreakType.WATER, BreakType.WATER)

        val streak = StreakCalculator.compute(breaks, zone, wednesday, { true }, goals::goalOn)
        assertEquals(3, streak.days)
        assertEquals(3, streak.goal)
        assertEquals(3, streak.todayDone)
    }
}
