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
        assertEquals(WorkHoursProblem.SAME_TIME, nineToSix.copy(startMinute = 0, endMinute = 0).problem)
    }

    /** No work days turns Continue / Save off and shows the reason straight away. */
    @Test
    fun noDaysShowsAtOnceAndCantBeSaved() {
        val noDays = nineToSix.copy(days = emptySet())
        assertFalse(noDays.canSave)
        assertEquals(WorkHoursProblem.NO_DAYS, noDays.shownProblem(checked = null))
        // Even over what an earlier Continue found: the days are what's wrong now.
        assertEquals(WorkHoursProblem.NO_DAYS, noDays.copy(startMinute = 600, endMinute = 600).shownProblem(WorkHoursProblem.SAME_TIME))
    }

    /** The same start and end is often a step on the way (9–18 → 18–18 → 18–02), so only a check shows it. */
    @Test
    fun theSameStartAndEndOnlyShowsOnceChecked() {
        val sameTime = nineToSix.copy(startMinute = 18 * 60)
        assertTrue(sameTime.canSave) // Continue / Save stays on, and does the check
        assertNull(sameTime.shownProblem(checked = null))
        assertEquals(WorkHoursProblem.SAME_TIME, sameTime.shownProblem(WorkHoursProblem.SAME_TIME))
    }

    /** 0 to 1440 is all day (the Pet lab's "always work hours"), and can still be edited. */
    @Test
    fun allDayIsFine() {
        val allDay = WorkHoursDraft.of(WorkSchedule.AlwaysOn)
        assertNull(allDay.problem)
        assertNull(allDay.toggled(DayOfWeek.SUNDAY).problem)
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
