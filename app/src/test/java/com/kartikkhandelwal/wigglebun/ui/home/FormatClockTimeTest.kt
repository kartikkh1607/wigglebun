package com.kartikkhandelwal.wigglebun.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime
import java.util.Locale

/** Focused card and asleep pill times: "9 AM–6 PM" / "9:00–18:00". */
class FormatClockTimeTest {

    private fun format(hour: Int, minute: Int, is24Hour: Boolean) =
        formatClockTime(LocalTime.of(hour, minute), is24Hour, Locale.US)

    @Test
    fun twelveHourDropsZeroMinutesOnWholeHours() {
        assertEquals("9 AM", format(9, 0, is24Hour = false))
        assertEquals("6 PM", format(18, 0, is24Hour = false))
        assertEquals("12 AM", format(0, 0, is24Hour = false))
    }

    @Test
    fun twelveHourKeepsMinutesOtherwise() {
        assertEquals("9:30 AM", format(9, 30, is24Hour = false))
        assertEquals("5:45 PM", format(17, 45, is24Hour = false))
    }

    @Test
    fun twentyFourHourAlwaysShowsMinutes() {
        assertEquals("9:00", format(9, 0, is24Hour = true))
        assertEquals("18:00", format(18, 0, is24Hour = true))
        assertEquals("9:30", format(9, 30, is24Hour = true))
    }

    @Test
    fun midnightEndOfDayWrapsToZero() {
        assertEquals(LocalTime.MIDNIGHT, minuteOfDay(24 * 60))
        assertEquals(LocalTime.of(18, 0), minuteOfDay(18 * 60))
    }
}
