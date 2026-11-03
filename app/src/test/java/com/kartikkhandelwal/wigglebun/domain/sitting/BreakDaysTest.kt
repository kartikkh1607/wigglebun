package com.kartikkhandelwal.wigglebun.domain.sitting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Break days over a history of boundaries: a change of hours never moves a break already done. */
class BreakDaysTest {

    private val kolkata = ZoneId.of("Asia/Kolkata")
    private val london = ZoneId.of("Europe/London")

    /** October 2026: Fri 23, Sat 24, Sun 25, Mon 26, Tue 27, Wed 28. */
    private fun at(text: String, zone: ZoneId = kolkata): Instant = LocalDateTime.parse(text).atZone(zone).toInstant()

    private fun day(dayOfMonth: Int): LocalDate = LocalDate.of(2026, 10, dayOfMonth)

    private val nights = DayBoundary(22 * 60)

    /** Nights since well before the dates these tests look at. */
    private val alwaysNights = DayBoundaryHistory().withChange(nights, at("2026-09-01T12:00"))

    @Test
    fun withoutAHistoryDaysTurnAtMidnight() {
        val days = BreakDays(DayBoundaryHistory(), kolkata)
        assertEquals(day(26), days.dayOf(at("2026-10-26T23:40")))
        assertEquals(day(27), days.dayOf(at("2026-10-27T00:10")))
        assertEquals(DaySpan(at("2026-10-26T00:00"), at("2026-10-27T00:00")), days.span(day(26)))
    }

    /** The case from the review: 23:40 and 00:10 on a 22:00–06:00 shift are the same day. */
    @Test
    fun aNightShiftIsOneDay() {
        val days = BreakDays(alwaysNights, kolkata)
        assertEquals(day(26), days.dayOf(at("2026-10-26T23:40")))
        assertEquals(day(26), days.dayOf(at("2026-10-27T00:10")))
        assertEquals(day(26), days.dayOf(at("2026-10-27T21:59")))
        assertEquals(day(27), days.dayOf(at("2026-10-27T22:00")))
        assertEquals(DaySpan(at("2026-10-26T22:00"), at("2026-10-27T22:00")), days.span(day(26)))
    }

    @Test
    fun nothingChangesForTheSameBoundary() {
        assertEquals(alwaysNights, alwaysNights.withChange(nights, at("2026-10-26T12:00")))
        assertEquals(DayBoundaryHistory(), DayBoundaryHistory().withChange(DayBoundary.MIDNIGHT, at("2026-10-26T12:00")))
    }

    /**
     * Switching to nights on Monday afternoon: Monday keeps its name until Tuesday 22:00, so
     * Monday night's shift is Monday's and the breaks from Monday morning stay Monday's.
     */
    @Test
    fun switchingToNightsMakesTodayLonger() {
        val days = BreakDays(DayBoundaryHistory().withChange(nights, at("2026-10-26T15:00")), kolkata)
        assertEquals(day(23), days.dayOf(at("2026-10-23T10:00"))) // last Friday's daytime break
        assertEquals(day(26), days.dayOf(at("2026-10-26T10:00")))
        assertEquals(day(26), days.dayOf(at("2026-10-26T15:00")))
        assertEquals(day(26), days.dayOf(at("2026-10-26T23:40")))
        assertEquals(day(26), days.dayOf(at("2026-10-27T00:10")))
        assertEquals(day(27), days.dayOf(at("2026-10-27T22:00")))
        assertEquals(DaySpan(at("2026-10-26T00:00"), at("2026-10-27T22:00")), days.span(day(26)))
        assertTrue(days.exists(day(27)))
    }

    /**
     * Back to daytime hours on Tuesday afternoon, during Monday's night-shift day: Monday runs on
     * to Wednesday 00:00, and Tuesday never starts. Breaks from Monday night stay Monday's.
     */
    @Test
    fun switchingBackToDaysCanSkipADate() {
        val history = alwaysNights.withChange(DayBoundary.MIDNIGHT, at("2026-10-27T15:00"))
        val days = BreakDays(history, kolkata)
        assertEquals(day(26), days.dayOf(at("2026-10-26T23:40")))
        assertEquals(day(26), days.dayOf(at("2026-10-27T02:00")))
        assertEquals(day(26), days.dayOf(at("2026-10-27T23:00")))
        assertEquals(day(28), days.dayOf(at("2026-10-28T00:00")))
        assertFalse(days.exists(day(27)))
        assertNull(days.span(day(27)))
        assertEquals(DaySpan(at("2026-10-26T22:00"), at("2026-10-28T00:00")), days.span(day(26)))
    }

    /** Switching back before midnight skips nothing: Monday ends at midnight. */
    @Test
    fun switchingBackBeforeMidnightSkipsNothing() {
        val history = alwaysNights.withChange(DayBoundary.MIDNIGHT, at("2026-10-26T23:00"))
        val days = BreakDays(history, kolkata)
        assertEquals(day(26), days.dayOf(at("2026-10-26T23:30")))
        assertEquals(day(27), days.dayOf(at("2026-10-27T00:00")))
        assertTrue(days.exists(day(27)))
        assertEquals(DaySpan(at("2026-10-26T22:00"), at("2026-10-27T00:00")), days.span(day(26)))
    }

    /** A second change while a day is still carried on keeps carrying the same day. */
    @Test
    fun changesInARowKeepTheSameToday() {
        val history = DayBoundaryHistory()
            .withChange(nights, at("2026-10-26T15:00"))
            .withChange(DayBoundary(23 * 60), at("2026-10-27T21:00"))
        val days = BreakDays(history, kolkata)
        assertEquals(day(26), days.dayOf(at("2026-10-27T22:30")))
        assertEquals(day(27), days.dayOf(at("2026-10-27T23:00")))
        assertTrue(days.exists(day(27)))
    }

    /** Clocks go forward at 01:00 on Sun 29 Mar 2026: Saturday's night-shift day is 23 hours. */
    @Test
    fun springForwardMakesA23HourDay() {
        val history = DayBoundaryHistory().withChange(nights, at("2026-03-01T12:00", london))
        val days = BreakDays(history, london)
        val saturday = LocalDate.of(2026, 3, 28)
        val span = days.span(saturday)!!
        assertEquals(Duration.ofHours(23), Duration.between(span.start, span.end))
        assertEquals(saturday, days.dayOf(at("2026-03-29T00:30", london)))
        assertEquals(saturday, days.dayOf(at("2026-03-29T03:00", london)))
    }

    /** Clocks go back at 02:00 on Sun 25 Oct 2026: 25 hours, and both 01:30s are Saturday's. */
    @Test
    fun fallBackMakesA25HourDay() {
        val history = DayBoundaryHistory().withChange(nights, at("2026-10-01T12:00", london))
        val days = BreakDays(history, london)
        val saturday = day(24)
        val span = days.span(saturday)!!
        assertEquals(Duration.ofHours(25), Duration.between(span.start, span.end))
        assertEquals(saturday, days.dayOf(Instant.parse("2026-10-25T00:30:00Z"))) // 01:30 BST
        assertEquals(saturday, days.dayOf(Instant.parse("2026-10-25T01:30:00Z"))) // 01:30 GMT
    }

    /**
     * Over a month of changes and both DST switches: the day never goes backwards, and every
     * day's span agrees with [BreakDays.dayOf] at its edges.
     */
    @Test
    fun daysNeverGoBackwardsAndSpansAgree() {
        val history = DayBoundaryHistory()
            .withChange(nights, at("2026-10-20T15:00", london))
            .withChange(DayBoundary.MIDNIGHT, at("2026-10-24T03:00", london)) // mid-shift, DST weekend
            .withChange(DayBoundary(21 * 60), at("2026-10-26T10:00", london))
            .withChange(nights, at("2026-10-28T21:30", london))
        val days = BreakDays(history, london)

        var instant = at("2026-10-18T00:00", london)
        var previous = days.dayOf(instant)
        while (instant.isBefore(at("2026-11-02T00:00", london))) {
            val day = days.dayOf(instant)
            assertFalse("went back at $instant", day.isBefore(previous))
            previous = day
            instant = instant.plus(Duration.ofMinutes(7))
        }

        var date = LocalDate.of(2026, 10, 18)
        while (date.isBefore(LocalDate.of(2026, 11, 1))) {
            val span = days.span(date)
            if (span == null) {
                assertFalse(days.exists(date))
            } else {
                assertEquals(date, days.dayOf(span.start))
                assertEquals(date, days.dayOf(span.end.minusMillis(1)))
                assertTrue(days.dayOf(span.start.minusMillis(1)).isBefore(date))
                assertTrue(days.dayOf(span.end).isAfter(date))
            }
            date = date.plusDays(1)
        }
    }
}
