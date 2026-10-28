package com.kartikkhandelwal.wigglebun.domain.rewards

import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.DoneBreak
import com.kartikkhandelwal.wigglebun.domain.model.PetId
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * The price table against simulated users (CLAUDE.md §5.4): work days until each pet can be
 * adopted, buying Pip, then Toast, then Nimbus. Every simulated day is a work day.
 */
class EconomyTest {

    private val zone = ZoneId.of("Europe/London")
    private val firstDay = LocalDate.of(2026, 11, 2)

    /** 3 types a day (15 sparks): below the streak goal, so no milestones. */
    private val light = listOf(BreakType.STAND_UP, BreakType.EYES, BreakType.WATER)

    /** 4 types a day (25 sparks): meets the goal, keeps the streak. */
    private val normal = listOf(BreakType.WALK, BreakType.STAND_UP, BreakType.NECK, BreakType.WATER)

    /** Every type every day (40 sparks). */
    private val heavy = BreakType.entries.toList()

    /** Total sparks after each work day. */
    private fun earnings(types: List<BreakType>, days: Int): List<Int> {
        val done = mutableListOf<DoneBreak>()
        val paid = mutableSetOf<String>()
        var total = 0
        return (1..days).map { n ->
            val today = firstDay.plusDays(n - 1L)
            types.forEach { type ->
                done += DoneBreak(type, today.atTime(LocalTime.NOON).atZone(zone).toInstant())
                if (paid.add(RewardRules.breakKey(today, type))) total += type.defaultReward
            }
            val streak = StreakCalculator.compute(done, zone, today, isWorkDay = { true }, goal = RewardRules.DAILY_STREAK_GOAL)
            val start = streak.startDate
            if (start != null) {
                RewardRules.milestonesReached(streak.days).forEach { m ->
                    if (paid.add(RewardRules.streakKey(m.days, start))) total += m.bonus
                }
            }
            total
        }
    }

    /** The work day each pet becomes affordable, buying them cheapest first. */
    private fun daysToEachPet(types: List<BreakType>): List<Int> {
        val totals = earnings(types, days = 120)
        var spent = 0
        return listOf(PetId.PIP, PetId.TOAST, PetId.NIMBUS).map { pet ->
            spent += PetPrices.of(pet)
            totals.indexOfFirst { it >= spent } + 1
        }
    }

    @Test
    fun workDaysToEachPet() {
        assertEquals(listOf(5, 17, 37), daysToEachPet(light))
        assertEquals(listOf(3, 8, 16), daysToEachPet(normal))
        assertEquals(listOf(2, 6, 12), daysToEachPet(heavy))
    }
}
