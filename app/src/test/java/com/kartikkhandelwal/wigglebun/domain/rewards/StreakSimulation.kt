package com.kartikkhandelwal.wigglebun.domain.rewards

import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.DoneBreak
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Days of breaks run through the real rules: StreakCalculator, RewardRules keys and a ledger
 * that pays each key once, like spark_ledger. Every simulated day is a work day.
 */
internal class StreakSimulation(private val firstDay: LocalDate = LocalDate.of(2026, 11, 2)) {
    private val zone = ZoneId.of("Europe/London")
    private val done = mutableListOf<DoneBreak>()
    private val paidKeys = mutableSetOf<String>()
    private var day = 0

    var total = 0
        private set

    /** Plays the next work day with [types] done; returns the streak bonuses it paid. */
    fun play(types: List<BreakType>): List<Milestone> {
        val today = firstDay.plusDays(day++.toLong())
        types.forEach { type ->
            done += DoneBreak(type, today.atTime(LocalTime.NOON).atZone(zone).toInstant())
            if (paidKeys.add(RewardRules.breakKey(today, type))) total += type.defaultReward
        }
        val streak = StreakCalculator.compute(done, zone, today, isWorkDay = { true }, goal = RewardRules.DAILY_STREAK_GOAL)
        val start = streak.startDate ?: return emptyList()
        return RewardRules.bonusesFor(streak.days, start)
            .filter { paidKeys.add(it.key) }
            .map { it.milestone }
            .onEach { total += it.bonus }
    }

    /** [days] work days that keep the streak (the daily goal of distinct types). */
    fun keepStreak(days: Int): List<Milestone> = (1..days).flatMap { play(GOAL_TYPES) }

    /** One work day below the goal: the streak ends. */
    fun miss(): List<Milestone> = play(emptyList())

    companion object {
        val GOAL_TYPES: List<BreakType> = BreakType.entries.take(RewardRules.DAILY_STREAK_GOAL)
    }
}
