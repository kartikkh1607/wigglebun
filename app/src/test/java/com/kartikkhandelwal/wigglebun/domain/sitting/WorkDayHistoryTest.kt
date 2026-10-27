package com.kartikkhandelwal.wigglebun.domain.sitting

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.kartikkhandelwal.wigglebun.data.prefs.toWorkDayHistory
import com.kartikkhandelwal.wigglebun.data.prefs.writeSchedule
import com.kartikkhandelwal.wigglebun.data.prefs.writeWorkDayHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class WorkDayHistoryTest {

    private val weekdays = WorkSchedule.Default.days
    private val everyDay = DayOfWeek.entries.toSet()
    private val change = LocalDate.of(2026, 10, 27)
    private val saturdayBefore = LocalDate.of(2026, 10, 24)
    private val saturdayAfter = LocalDate.of(2026, 10, 31)

    @Test
    fun noChangesMeansTheCurrentDaysForever() {
        val history = WorkDayHistory(emptyList(), weekdays)
        assertFalse(history.isWorkDay(saturdayBefore))
        assertTrue(history.isWorkDay(LocalDate.of(1999, 1, 4))) // a Monday long ago
    }

    @Test
    fun aChangeAppliesFromItsDayOnly() {
        val history = WorkDayHistory(emptyList(), weekdays).withChange(everyDay, change)
        assertFalse(history.isWorkDay(saturdayBefore))
        assertTrue(history.isWorkDay(saturdayAfter))
        assertEquals(everyDay, history.current)
    }

    @Test
    fun sameDaysIsNotAChange() {
        val history = WorkDayHistory(emptyList(), weekdays)
        assertSame(history, history.withChange(weekdays, change))
    }

    @Test
    fun aSecondChangeTheSameDayReplacesTheFirst() {
        val weekends = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        val history = WorkDayHistory(emptyList(), weekdays)
            .withChange(everyDay, change)
            .withChange(weekends, change)
        assertEquals(2, history.changes.size) // "since forever: weekdays" + today
        assertEquals(weekends, history.daysOn(change))
        assertEquals(weekdays, history.daysOn(change.minusDays(1)))
    }

    @Test
    fun historyRoundTripsThroughPrefs() {
        val prefs = mutablePreferencesOf()
        val history = WorkDayHistory(emptyList(), weekdays).withChange(everyDay, change)
        prefs.writeSchedule(WorkSchedule(everyDay, 9 * 60, 18 * 60))
        prefs.writeWorkDayHistory(history)
        assertEquals(history, prefs.toWorkDayHistory())
    }

    @Test
    fun emptyPrefsGiveTheDefaultDaysWithNoHistory() {
        assertEquals(WorkDayHistory(emptyList(), weekdays), mutablePreferencesOf().toWorkDayHistory())
    }
}
