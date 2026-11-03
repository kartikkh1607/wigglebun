package com.kartikkhandelwal.wigglebun.ui.pet.profile

import com.kartikkhandelwal.wigglebun.domain.model.ActivePet
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.DoneBreak
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.domain.rewards.RewardRules
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
        goalOn = { RewardRules.DAILY_GOAL },
    )

    private val threeTypes = arrayOf(BreakType.STAND_UP, BreakType.WALK, BreakType.NECK)

    @Test
    fun firstDayHasNoStreakNoBreaksAndAnEmptyWeek() {
        val state = map(emptyList())
        assertEquals(StreakCardUi(days = 0, todayDone = 0, goal = 3, todayIsWorkDay = true), state.streak)
        assertEquals(0, state.totalBreaks)
        assertEquals(7, state.week.size)
        assertEquals(0, state.weekTotal)
    }

    /** "Today 2 of 3" counts completed breaks; the streak ends at the last finished work day. */
    @Test
    fun streakCardShowsTodaysProgress() {
        val done = on(wednesday.minusDays(2), *threeTypes) + // Monday
            on(wednesday.minusDays(1), *threeTypes) + // Tuesday
            on(wednesday, BreakType.WALK, BreakType.WALK) // a repeat counts too
        val state = map(done)
        assertEquals(2, state.streak.days)
        assertEquals(2, state.streak.todayDone)
        assertEquals(3, state.streak.goal)
        assertFalse(state.streak.todayCounts)
        assertEquals(8, state.totalBreaks)
        assertEquals(2, state.week.last().total)
    }

    @Test
    fun todayCountsOnceTheGoalIsMet() {
        val done = on(wednesday.minusDays(1), *threeTypes) + on(wednesday, BreakType.WATER, BreakType.WATER, BreakType.EYES)
        val state = map(done)
        assertTrue(state.streak.todayCounts)
        assertEquals(2, state.streak.days)
    }

    /** A weekend neither breaks the streak nor shows today's progress bar. */
    @Test
    fun aDayOffSaysSo() {
        val saturday = LocalDate.of(2026, 10, 31)
        val friday = saturday.minusDays(1)
        val state = map(on(friday, *threeTypes), today = saturday)
        assertFalse(state.streak.todayIsWorkDay)
        assertEquals(1, state.streak.days)
        assertEquals(3, state.totalBreaks)
    }
}
