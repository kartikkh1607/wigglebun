package com.kartikkhandelwal.wigglebun.domain.rewards

import com.kartikkhandelwal.wigglebun.domain.model.AccessoryId
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

    /** The work day each purchase becomes affordable, buying [prices] in order. */
    private fun daysToAfford(types: List<BreakType>, prices: List<Int>): List<Int> {
        val totals = earnings(types, days = 120)
        var spent = 0
        return prices.map { price ->
            spent += price
            totals.indexOfFirst { it >= spent } + 1
        }
    }

    private val petPrices = listOf(PetId.PIP, PetId.TOAST, PetId.NIMBUS).map(PetPrices::of)

    /** The v1 accessories, cheapest first (CLAUDE.md §6.5). */
    private val accessoryPrices = AccessoryId.entries.map { it.price }.sorted()

    private fun daysToEachPet(types: List<BreakType>): List<Int> = daysToAfford(types, petPrices)

    /** Bought after every pet: the work day each accessory becomes affordable. */
    private fun daysToEachAccessory(types: List<BreakType>): List<Int> =
        daysToAfford(types, petPrices + accessoryPrices).drop(petPrices.size)

    @Test
    fun workDaysToEachPet() {
        assertEquals(listOf(5, 15, 35), daysToEachPet(light))
        assertEquals(listOf(3, 7, 15), daysToEachPet(normal))
        assertEquals(listOf(2, 6, 12), daysToEachPet(heavy))
    }

    /**
     * The spark sink after the pets (CLAUDE.md §6.5): with the 4 v1 accessories a normal user
     * keeps buying until about work day 30; the post-launch batch is meant to stretch that to ~8 weeks.
     */
    @Test
    fun workDaysToEachAccessoryAfterThePets() {
        assertEquals(listOf(39, 44, 50, 59), daysToEachAccessory(light))
        assertEquals(listOf(17, 20, 24, 29), daysToEachAccessory(normal))
        assertEquals(listOf(13, 14, 15, 18), daysToEachAccessory(heavy))
    }

    /** The exploit the lifetime milestones close: breaking a streak on purpose never pays more. */
    @Test
    fun restartingNeverOutEarnsKeepingTheStreak() {
        val kept = StreakSimulation().apply { keepStreak(31); keepStreak(30) }
        val restarted = StreakSimulation().apply { keepStreak(30); miss(); keepStreak(30) }
        assertTrue("kept ${kept.total} vs restarted ${restarted.total}", kept.total > restarted.total)
    }
}
