package com.kartikkhandelwal.wigglebun.ui.pet.profile

import com.kartikkhandelwal.wigglebun.domain.model.ActivePet
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.DoneBreak
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset

class PetProfileMapperTest {

    private val zone = ZoneOffset.UTC
    private val wednesday = LocalDate.of(2026, 10, 28)
    private val weekdays: (LocalDate) -> Boolean = { it.dayOfWeek !in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) }

    private fun on(date: LocalDate, vararg types: BreakType) =
        types.mapIndexed { i, type -> DoneBreak(type, date.atTime(9 + i, 0).toInstant(zone)) }

    private fun map(done: List<DoneBreak>, today: LocalDate = wednesday) = toPetProfileUiState(
        pet = ActivePet.Default,
        mood = Mood.HAPPY,
        energy = 1f,
        done = done,
        today = today,
        zone = zone,
        isWorkDay = weekdays,
        goal = 4,
    )

    private val fourTypes = arrayOf(BreakType.STAND_UP, BreakType.WALK, BreakType.NECK, BreakType.EYES)

    @Test
    fun firstDayHasNoStreakNoBreaksAndAnEmptyWeek() {
        val state = map(emptyList())
        assertEquals(StreakCardUi(days = 0, todayDone = 0, goal = 4, todayIsWorkDay = true), state.streak)
        assertEquals(0, state.totalBreaks)
        assertEquals(7, state.week.size)
        assertEquals(0, state.weekTotal)
    }

    /** "Today 2 of 4" counts distinct types; the streak ends at the last finished work day. */
    @Test
    fun streakCardShowsTodaysProgress() {
        val done = on(wednesday.minusDays(2), *fourTypes) + // Monday
            on(wednesday.minusDays(1), *fourTypes) + // Tuesday
            on(wednesday, BreakType.WALK, BreakType.WALK, BreakType.WATER) // a repeat: 2 distinct
        val state = map(done)
        assertEquals(2, state.streak.days)
        assertEquals(2, state.streak.todayDone)
        assertEquals(4, state.streak.goal)
        assertFalse(state.streak.todayCounts)
        assertEquals(11, state.totalBreaks) // repeats count in the total
        assertEquals(3, state.week.last().total)
    }

    @Test
    fun todayCountsOnceTheGoalIsMet() {
        val done = on(wednesday.minusDays(1), *fourTypes) + on(wednesday, *fourTypes)
        val state = map(done)
        assertTrue(state.streak.todayCounts)
        assertEquals(2, state.streak.days)
    }

    /** A weekend neither breaks the streak nor shows today's progress bar. */
    @Test
    fun aDayOffSaysSo() {
        val saturday = LocalDate.of(2026, 10, 31)
        val friday = saturday.minusDays(1)
        val state = map(on(friday, *fourTypes), today = saturday)
        assertFalse(state.streak.todayIsWorkDay)
        assertEquals(1, state.streak.days)
        assertEquals(4, state.totalBreaks)
    }
}
