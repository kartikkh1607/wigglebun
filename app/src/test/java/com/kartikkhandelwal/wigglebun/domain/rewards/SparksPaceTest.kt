package com.kartikkhandelwal.wigglebun.domain.rewards

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** Collection's days estimate (UI v2 decision D4). */
class SparksPaceTest {

    private val monday: LocalDate = LocalDate.of(2026, 10, 26)

    private fun days(vararg earned: Int): Map<LocalDate, Int> =
        earned.withIndex().associate { (i, sparks) -> monday.plusDays(i.toLong()) to sparks }

    @Test
    fun aNewUserGetsFifteenADay() {
        assertEquals(15, SparksPace.NEW_USER_PACE)
        assertEquals(15.0, SparksPace.perDay(emptyMap()), 0.0)
        // The mockup: 30 / 75, about 3 more days.
        assertEquals(3, SparksPace.daysToEarn(75 - 30, SparksPace.perDay(emptyMap())))
    }

    @Test
    fun theUsersOwnPaceAveragesTheirBreakDays() {
        // Heavy days: 40 a day, so the 45 Pip still needs is 2 days, not the fallback's 3.
        assertEquals(40.0, SparksPace.perDay(days(40, 40, 40)), 0.0)
        assertEquals(2, SparksPace.daysToEarn(45, SparksPace.perDay(days(40, 40, 40))))
        // Light days: 10 a day, Pip's 45 takes 5.
        assertEquals(5, SparksPace.daysToEarn(45, SparksPace.perDay(days(10, 10))))
    }

    @Test
    fun onlyTheLastSevenBreakDaysCount() {
        // Three old 40-spark days, then seven recent 10-spark days: the old ones are left out.
        val old = (0 until 3).associate { monday.minusDays(30L - it) to 40 }
        assertEquals(10.0, SparksPace.perDay(old + days(10, 10, 10, 10, 10, 10, 10)), 0.0)
    }

    @Test
    fun daysWithoutABreakDontSlowThePace() {
        // A weekend between two 20-spark days: still 20 a day.
        val gap = mapOf(monday to 20, monday.plusDays(7) to 20)
        assertEquals(20.0, SparksPace.perDay(gap), 0.0)
    }

    @Test
    fun roundsUpAndStopsAtZero() {
        assertEquals(1, SparksPace.daysToEarn(1, 40.0))
        assertEquals(1, SparksPace.daysToEarn(40, 40.0))
        assertEquals(2, SparksPace.daysToEarn(41, 40.0))
        // 22.5 a day: 45 is exactly 2 days.
        assertEquals(2, SparksPace.daysToEarn(45, SparksPace.perDay(days(20, 25))))
        assertEquals(0, SparksPace.daysToEarn(0, 15.0))
        assertEquals(0, SparksPace.daysToEarn(-10, 15.0))
    }

    @Test
    fun breakKeysAreSummedPerDay() {
        val earned = SparksPace.earnedByDay(
            listOf(
                "break:2026-10-26:STAND_UP" to 5,
                "break:2026-10-26:WALK" to 10,
                "break:2026-10-27:NECK" to 5,
            ),
        )
        assertEquals(mapOf(monday to 15, monday.plusDays(1) to 5), earned)
    }
}
