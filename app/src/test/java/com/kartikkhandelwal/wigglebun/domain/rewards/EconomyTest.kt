package com.kartikkhandelwal.wigglebun.domain.rewards

import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.PetId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The price table against simulated users (CLAUDE.md §5.4): work days until each pet can be
 * adopted, buying Pip, then Toast, then Nimbus. Every simulated day is a work day.
 */
class EconomyTest {

    /** 3 types a day (15 sparks): below the streak goal, so no milestones. */
    private val light = listOf(BreakType.STAND_UP, BreakType.EYES, BreakType.WATER)

    /** 4 types a day (25 sparks): meets the goal, keeps the streak. */
    private val normal = listOf(BreakType.WALK, BreakType.STAND_UP, BreakType.NECK, BreakType.WATER)

    /** Every type every day (40 sparks). */
    private val heavy = BreakType.entries.toList()

    /** Total sparks after each work day. */
    private fun earnings(types: List<BreakType>, days: Int): List<Int> {
        val sim = StreakSimulation()
        return (1..days).map { sim.play(types); sim.total }
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
        assertEquals(listOf(5, 15, 35), daysToEachPet(light))
        assertEquals(listOf(3, 7, 15), daysToEachPet(normal))
        assertEquals(listOf(2, 6, 12), daysToEachPet(heavy))
    }

    /** The exploit the lifetime milestones close: breaking a streak on purpose never pays more. */
    @Test
    fun restartingNeverOutEarnsKeepingTheStreak() {
        val kept = StreakSimulation().apply { keepStreak(31); keepStreak(30) }
        val restarted = StreakSimulation().apply { keepStreak(30); miss(); keepStreak(30) }
        assertTrue("kept ${kept.total} vs restarted ${restarted.total}", kept.total > restarted.total)
    }
}
