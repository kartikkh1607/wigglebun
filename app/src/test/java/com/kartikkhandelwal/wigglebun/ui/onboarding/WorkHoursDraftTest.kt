package com.kartikkhandelwal.wigglebun.ui.onboarding

import com.kartikkhandelwal.wigglebun.domain.sitting.WorkSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class WorkHoursDraftTest {

    private val nineToSix = WorkHoursDraft.of(WorkSchedule.Default)

    @Test
    fun theDefaultsAreFine() {
        assertNull(nineToSix.problem)
        assertEquals(WorkSchedule.Default, nineToSix.toSchedule())
    }

    @Test
    fun aDayCanBeTurnedOffAndBackOn() {
        val noMonday = nineToSix.toggled(DayOfWeek.MONDAY)
        assertFalse(DayOfWeek.MONDAY in noMonday.days)
        assertEquals(nineToSix, noMonday.toggled(DayOfWeek.MONDAY))
    }

    @Test
    fun noDaysIsAProblem() {
        assertEquals(WorkHoursProblem.NO_DAYS, nineToSix.copy(days = emptySet()).problem)
    }

    @Test
    fun theSameStartAndEndIsAProblem() {
        assertEquals(WorkHoursProblem.SAME_TIME, nineToSix.copy(startMinute = 600, endMinute = 600).problem)
        // Midnight is 0 and 1440 in the schedule: both are the same time as a midnight start.
        assertEquals(WorkHoursProblem.SAME_TIME, nineToSix.copy(startMinute = 0, endMinute = WorkSchedule.MINUTES_PER_DAY).problem)
    }

    @Test
    fun endingAfterMidnightSaysSo() {
        assertTrue(nineToSix.copy(startMinute = 22 * 60, endMinute = 6 * 60).endsNextDay)
        assertFalse(nineToSix.endsNextDay)
    }

    /** Midnight ends the day that started, it isn't "the next day". */
    @Test
    fun endingAtMidnightIsTheSameDay() {
        assertFalse(nineToSix.copy(startMinute = 18 * 60, endMinute = 0).endsNextDay)
        assertNull(nineToSix.copy(startMinute = 18 * 60, endMinute = 0).problem)
    }
}
