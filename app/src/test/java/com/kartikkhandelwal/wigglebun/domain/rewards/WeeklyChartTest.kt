package com.kartikkhandelwal.wigglebun.domain.rewards

import com.kartikkhandelwal.wigglebun.domain.model.BreakCategory
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.DoneBreak
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class WeeklyChartTest {

    private val london = ZoneId.of("Europe/London")
    private val newYork = ZoneId.of("America/New_York")

    private fun done(type: BreakType, local: String, zone: ZoneId) =
        DoneBreak(type, LocalDateTime.parse(local).atZone(zone).toInstant())

    @Test
    fun sevenDaysOldestFirstEndingToday() {
        val today = LocalDate.of(2026, 10, 28)
        val bars = WeeklyChart.lastSevenDays(emptyList(), today, london)
        assertEquals((6 downTo 0).map { today.minusDays(it.toLong()) }, bars.map { it.date })
        assertTrue("empty days are still there, with nothing in them", bars.all { it.total == 0 && it.segments.isEmpty() })
    }

    @Test
    fun breaksStackByCategoryAndTheTotalCountsRepeats() {
        val today = LocalDate.of(2026, 10, 28)
        val bars = WeeklyChart.lastSevenDays(
            listOf(
                done(BreakType.STAND_UP, "2026-10-28T09:00", london),
                done(BreakType.WALK, "2026-10-28T10:00", london),
                done(BreakType.WALK, "2026-10-28T11:00", london), // a repeat still counts
                done(BreakType.NECK, "2026-10-28T12:00", london),
                done(BreakType.SHOULDERS, "2026-10-28T13:00", london),
                done(BreakType.WRIST, "2026-10-28T14:00", london),
                done(BreakType.WATER, "2026-10-28T15:00", london),
            ),
            today,
            london,
        )
        val todayBar = bars.last()
        assertEquals(7, todayBar.total)
        assertEquals(
            mapOf(BreakCategory.MOVEMENT to 3, BreakCategory.STRETCH to 3, BreakCategory.WATER to 1),
            todayBar.counts,
        )
        // Bottom to top in the stack order; Eyes (none today) is left out.
        assertEquals(
            listOf(BreakCategory.STRETCH to 3, BreakCategory.MOVEMENT to 3, BreakCategory.WATER to 1),
            todayBar.segments,
        )
    }

    /** Stretch blue and Eyes lilac are never neighbours in a stack (colour-blind safe order). */
    @Test
    fun stretchAndEyesAreNeverStackedTogether() {
        val order = WeeklyChart.STACK_ORDER
        assertEquals(BreakCategory.entries.toSet(), order.toSet())
        val stretch = order.indexOf(BreakCategory.STRETCH)
        val eyes = order.indexOf(BreakCategory.EYES)
        assertTrue(kotlin.math.abs(stretch - eyes) > 1)
    }

    @Test
    fun breaksOutsideTheWeekAreLeftOut() {
        val today = LocalDate.of(2026, 10, 28)
        val bars = WeeklyChart.lastSevenDays(
            listOf(
                done(BreakType.WALK, "2026-10-21T23:59", london), // 7 days ago: just outside
                done(BreakType.WALK, "2026-10-22T00:00", london), // first day of the week
                done(BreakType.WALK, "2026-10-29T08:00", london), // "tomorrow": the phone's clock was moved back
            ),
            today,
            london,
        )
        assertEquals(1, bars.sumOf { it.total })
        assertEquals(1, bars.first().total)
    }

    /** UK clocks go back on 25 Oct 2026: a 25-hour day stays one day, nothing shifts. */
    @Test
    fun aWeekWithTheClocksGoingBack() {
        val today = LocalDate.of(2026, 10, 27)
        val bars = WeeklyChart.lastSevenDays(
            listOf(
                done(BreakType.EYES, "2026-10-25T00:30", london),
                done(BreakType.EYES, "2026-10-25T01:30", london), // the hour that happens twice
                done(BreakType.WATER, "2026-10-25T23:30", london),
                done(BreakType.NECK, "2026-10-26T00:30", london),
            ),
            today,
            london,
        )
        assertEquals(7, bars.size)
        assertEquals(bars.map { it.date }.distinct(), bars.map { it.date })
        assertEquals(3, bars.single { it.date == LocalDate.of(2026, 10, 25) }.total)
        assertEquals(1, bars.single { it.date == LocalDate.of(2026, 10, 26) }.total)
    }

    /** US clocks go forward on 8 Mar 2026: a 23-hour day loses no breaks to its neighbours. */
    @Test
    fun aWeekWithTheClocksGoingForward() {
        val today = LocalDate.of(2026, 3, 10)
        val bars = WeeklyChart.lastSevenDays(
            listOf(
                done(BreakType.STAND_UP, "2026-03-08T00:30", newYork),
                done(BreakType.WALK, "2026-03-08T23:30", newYork),
                done(BreakType.WATER, "2026-03-07T23:30", newYork),
                done(BreakType.NECK, "2026-03-09T00:30", newYork),
            ),
            today,
            newYork,
        )
        assertEquals(7, bars.size)
        assertEquals(2, bars.single { it.date == LocalDate.of(2026, 3, 8) }.total)
        assertEquals(1, bars.single { it.date == LocalDate.of(2026, 3, 7) }.total)
        assertEquals(1, bars.single { it.date == LocalDate.of(2026, 3, 9) }.total)
    }
}
