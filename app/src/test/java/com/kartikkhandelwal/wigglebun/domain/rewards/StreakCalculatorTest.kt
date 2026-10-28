package com.kartikkhandelwal.wigglebun.domain.rewards

import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.DoneBreak
import com.kartikkhandelwal.wigglebun.domain.sitting.WorkDayHistory
import com.kartikkhandelwal.wigglebun.domain.sitting.WorkSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class StreakCalculatorTest {

    private val zone = ZoneId.of("Europe/London")
    private val weekdays = WorkDayHistory(changes = emptyList(), current = WorkSchedule.Default.days)
    private val goal = RewardRules.DAILY_STREAK_GOAL

    // October 2026: Mon 19 … Fri 23, Sat 24, Sun 25, Mon 26, Tue 27.
    private fun day(dayOfMonth: Int): LocalDate = LocalDate.of(2026, 10, dayOfMonth)

    /** [types] distinct break types done on [date] at [time] local. */
    private fun done(date: LocalDate, types: Int, time: LocalTime = LocalTime.NOON): List<DoneBreak> =
        BreakType.entries.take(types).map { DoneBreak(it, date.atTime(time).atZone(zone).toInstant()) }

    private fun streak(
        done: List<DoneBreak>,
        today: LocalDate,
        history: WorkDayHistory = weekdays,
        goal: Int = this.goal,
    ) = StreakCalculator.compute(done, zone, today, history::isWorkDay, goal)

    @Test
    fun noBreaksNoStreak() {
        val s = streak(emptyList(), today = day(27))
        assertEquals(0, s.days)
        assertNull(s.startDate)
        assertTrue(s.todayIsWorkDay)
    }

    @Test
    fun weekendGapDoesNotBreakTheStreak() {
        val s = streak(done(day(22), 4) + done(day(23), 4) + done(day(26), 4), today = day(27))
        assertEquals(3, s.days) // Thu, Fri, Mon; Sat/Sun skipped; Tue still in progress
        assertEquals(day(22), s.startDate)
    }

    @Test
    fun aMissedWorkDayEndsTheStreak() {
        val s = streak(done(day(21), 4) + done(day(22), 1) + done(day(23), 4), today = day(26))
        assertEquals(1, s.days) // only Fri: Thu had 1 type
        assertEquals(day(23), s.startDate)
    }

    @Test
    fun todayInProgressDoesNotBreakItAndCountsOnceMet() {
        val earlier = done(day(23), 4) + done(day(26), 4)
        val partial = streak(earlier + done(day(27), 2), today = day(27))
        assertEquals(2, partial.days)
        assertEquals(2, partial.todayDistinct)
        assertFalse(partial.todayCounts)

        val met = streak(earlier + done(day(27), 4), today = day(27))
        assertEquals(3, met.days)
        assertTrue(met.todayCounts)
    }

    @Test
    fun repeatsOfOneTypeCountOnce() {
        val sameType = List(6) { DoneBreak(BreakType.NECK, day(27).atTime(9 + it, 0).atZone(zone).toInstant()) }
        val s = streak(sameType, today = day(27))
        assertEquals(1, s.todayDistinct)
        assertEquals(0, s.days)
    }

    @Test
    fun breaksOnANonWorkDayNeitherAddNorBreak() {
        val s = streak(done(day(23), 4) + done(day(24), 4), today = day(26))
        assertEquals(1, s.days) // Fri counts; Saturday's breaks don't add a day
        assertFalse(streak(emptyList(), today = day(24)).todayIsWorkDay)
    }

    /** Europe/London springs forward on Sun 29 Mar 2026 (a 23-hour day). */
    @Test
    fun dstChangeKeepsDaysIntact() {
        val friday = LocalDate.of(2026, 3, 27)
        val monday = LocalDate.of(2026, 3, 30)
        // Monday's 4th type at 00:30 local, i.e. 23:30 UTC on Sunday: must still count for Monday.
        val mondayBreaks = done(monday, 3) + DoneBreak(BreakType.entries[3], monday.atTime(0, 30).atZone(zone).toInstant())
        val s = streak(done(friday, 4) + mondayBreaks, today = monday)
        assertEquals(2, s.days)
        assertEquals(4, s.todayDistinct)
        assertEquals(friday, s.startDate)
    }

    /**
     * Saturdays became work days on Tue 27 Oct. The empty Saturday before that was not a work
     * day back then, so it must not break the streak.
     */
    @Test
    fun changedWorkDaysAreNotAppliedRetroactively() {
        val everyDay = DayOfWeek.entries.toSet()
        val history = weekdays.withChange(everyDay, today = day(27))
        val breaks = done(day(23), 4) + done(day(26), 4) + done(day(27), 4)

        assertEquals(3, streak(breaks, today = day(27), history = history).days)
        // Judging the past by today's days would have counted the empty Saturday as missed.
        val currentDaysOnly = WorkDayHistory(emptyList(), everyDay)
        assertEquals(2, streak(breaks, today = day(27), history = currentDaysOnly).days)
    }

    @Test
    fun repeatKeyIsStableWhileTheStreakLastsAndNewAfterABreak() {
        val first3 = done(day(21), 4) + done(day(22), 4) + done(day(23), 4)
        val onDay3 = streak(first3, today = day(23))
        val onDay4 = streak(first3 + done(day(26), 4), today = day(26))
        assertEquals(3, onDay3.days)
        assertEquals(4, onDay4.days)
        val firstStart = checkNotNull(onDay3.startDate)
        // Same streak, same start date: a repeat bonus key can't be earned twice by one streak.
        assertEquals(firstStart, onDay4.startDate)
        assertEquals(RewardRules.repeatKey(60, firstStart), RewardRules.repeatKey(60, checkNotNull(onDay4.startDate)))

        // A missed day, then a new streak: a new start date.
        val later = first3 + done(day(26), 1) + done(day(27), 4) + done(day(28), 4) + done(day(29), 4)
        val newStreak = streak(later, today = day(29))
        assertEquals(3, newStreak.days)
        assertNotEquals(RewardRules.repeatKey(60, firstStart), RewardRules.repeatKey(60, checkNotNull(newStreak.startDate)))
    }

    @Test
    fun goalIsCappedByTheEnabledBreakTypes() {
        assertEquals(4, RewardRules.dailyGoal(enabledTypeCount = BreakType.entries.size))
        assertEquals(3, RewardRules.dailyGoal(enabledTypeCount = 3))
        assertEquals(1, RewardRules.dailyGoal(enabledTypeCount = 0))

        // A user with only 3 types switched on can still build a streak.
        val threeTypes = RewardRules.dailyGoal(enabledTypeCount = 3)
        val s = streak(done(day(26), 3) + done(day(27), 3), today = day(27), goal = threeTypes)
        assertEquals(2, s.days)
        assertTrue(s.todayCounts)
    }
}
