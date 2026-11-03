package com.kartikkhandelwal.wigglebun.domain.sitting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** When a day starts for breaks: midnight, or the shift start when work hours cross midnight. */
class DayBoundaryTest {

    private val kolkata = ZoneId.of("Asia/Kolkata")
    private val london = ZoneId.of("Europe/London")

    private fun at(text: String, zone: ZoneId = kolkata): Instant = LocalDateTime.parse(text).atZone(zone).toInstant()

    private val weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
    private val nights = WorkSchedule(weekdays, startMinute = 22 * 60, endMinute = 6 * 60)
    private val nightDay = DayBoundary.of(nights)

    private val monday = LocalDate.of(2026, 10, 26)
    private val tuesday = monday.plusDays(1)

    @Test
    fun aNightShiftIsOneDay() {
        assertEquals(monday, nightDay.dayOf(at("2026-10-26T23:40"), kolkata))
        assertEquals(monday, nightDay.dayOf(at("2026-10-27T00:10"), kolkata))
    }

    @Test
    fun theNextDayStartsWhenTheNextShiftDoes() {
        assertEquals(monday, nightDay.dayOf(at("2026-10-27T21:59"), kolkata))
        assertEquals(tuesday, nightDay.dayOf(at("2026-10-27T22:00"), kolkata))
        assertEquals(at("2026-10-27T22:00"), nightDay.start(tuesday, kolkata))
        assertEquals(at("2026-10-27T22:00"), nightDay.end(monday, kolkata))
    }

    @Test
    fun daytimeHoursTurnAtMidnight() {
        val daytime = DayBoundary.of(WorkSchedule.Default)
        assertEquals(DayBoundary.MIDNIGHT, daytime)
        assertEquals(monday, daytime.dayOf(at("2026-10-26T23:40"), kolkata))
        assertEquals(tuesday, daytime.dayOf(at("2026-10-27T00:10"), kolkata))
    }

    /** 22:00 to midnight stays within one calendar day, so midnight stays the boundary. */
    @Test
    fun endingAtMidnightIsNotOvernight() {
        assertTrue(nights.crossesMidnight)
        listOf(
            WorkSchedule(weekdays, startMinute = 22 * 60, endMinute = 0),
            WorkSchedule(weekdays, startMinute = 22 * 60, endMinute = WorkSchedule.MINUTES_PER_DAY),
            WorkSchedule.AlwaysOn,
        ).forEach { schedule ->
            assertFalse(schedule.crossesMidnight)
            assertEquals(DayBoundary.MIDNIGHT, DayBoundary.of(schedule))
        }
    }

    /** Clocks go forward at 01:00 on Sun 29 Mar 2026: Saturday's shift day is 23 hours long. */
    @Test
    fun springForwardMakesA23HourDay() {
        val saturday = LocalDate.of(2026, 3, 28)
        val start = nightDay.start(saturday, london)
        val end = nightDay.end(saturday, london)
        assertEquals(Instant.parse("2026-03-28T22:00:00Z"), start) // 22:00 GMT
        assertEquals(Instant.parse("2026-03-29T21:00:00Z"), end) // 22:00 BST
        assertEquals(Duration.ofHours(23), Duration.between(start, end))

        assertEquals(saturday, nightDay.dayOf(at("2026-03-29T00:30", london), london))
        assertEquals(saturday, nightDay.dayOf(at("2026-03-29T03:00", london), london)) // after the jump
        assertEquals(saturday, nightDay.dayOf(end.minusMillis(1), london))
        assertEquals(saturday.plusDays(1), nightDay.dayOf(end, london))
    }

    /** Clocks go back at 02:00 on Sun 25 Oct 2026: the day is 25 hours, and 01:30 happens twice. */
    @Test
    fun fallBackMakesA25HourDayAndBothOneThirtiesCount() {
        val saturday = LocalDate.of(2026, 10, 24)
        val start = nightDay.start(saturday, london)
        val end = nightDay.end(saturday, london)
        assertEquals(Instant.parse("2026-10-24T21:00:00Z"), start) // 22:00 BST
        assertEquals(Instant.parse("2026-10-25T22:00:00Z"), end) // 22:00 GMT
        assertEquals(Duration.ofHours(25), Duration.between(start, end))

        val oneThirty = ZonedDateTime.of(LocalDateTime.parse("2026-10-25T01:30"), london)
        val first = oneThirty.withEarlierOffsetAtOverlap().toInstant()
        val second = oneThirty.withLaterOffsetAtOverlap().toInstant()
        assertEquals(Duration.ofHours(1), Duration.between(first, second))
        assertEquals(saturday, nightDay.dayOf(first, london))
        assertEquals(saturday, nightDay.dayOf(second, london))
    }

    /** Every day's start belongs to it, and the moment before belongs to the day before. */
    @Test
    fun startAndDayOfAgree() {
        val dates = listOf(LocalDate.of(2026, 3, 27), LocalDate.of(2026, 3, 28), LocalDate.of(2026, 3, 29), LocalDate.of(2026, 10, 24), LocalDate.of(2026, 10, 25))
        listOf(nightDay, DayBoundary.MIDNIGHT).forEach { boundary ->
            dates.forEach { date ->
                val start = boundary.start(date, london)
                assertEquals(date, boundary.dayOf(start, london))
                assertEquals(date.minusDays(1), boundary.dayOf(start.minusMillis(1), london))
            }
        }
    }

    /**
     * A shift from 01:30 to 01:00 has its boundary inside the hour that repeats on a fall-back
     * night. The day starts at the first 01:30 and doesn't step back when 01:00 comes round again.
     */
    @Test
    fun aBoundaryInTheRepeatedHourNeverGoesBackwards() {
        val boundary = DayBoundary.of(WorkSchedule(weekdays, startMinute = 90, endMinute = 60))
        assertEquals(DayBoundary(90), boundary)
        val sunday = LocalDate.of(2026, 10, 25)
        val firstOneThirty = boundary.start(sunday, london)
        assertEquals(Instant.parse("2026-10-25T00:30:00Z"), firstOneThirty) // 01:30 BST

        var instant = firstOneThirty.minus(Duration.ofHours(2))
        var previous = boundary.dayOf(instant, london)
        while (instant.isBefore(firstOneThirty.plus(Duration.ofHours(3)))) {
            val day = boundary.dayOf(instant, london)
            assertFalse("went back at $instant", day.isBefore(previous))
            previous = day
            instant = instant.plus(Duration.ofMinutes(5))
        }
        // The second 01:15 (GMT) comes after the first 01:30, so it is Sunday's too.
        assertEquals(sunday, boundary.dayOf(Instant.parse("2026-10-25T01:15:00Z"), london))
    }
}
