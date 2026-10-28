package com.kartikkhandelwal.wigglebun.ui.pet.profile

import com.kartikkhandelwal.wigglebun.domain.model.ActivePet
import com.kartikkhandelwal.wigglebun.domain.model.DoneBreak
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.domain.rewards.DayBar
import com.kartikkhandelwal.wigglebun.domain.rewards.StreakCalculator
import com.kartikkhandelwal.wigglebun.domain.rewards.WeeklyChart
import java.time.LocalDate
import java.time.ZoneId

/** The streak card: "3 days" and today's progress, or a day off. */
data class StreakCardUi(
    val days: Int,
    /** Distinct break types done today. */
    val todayDone: Int,
    val goal: Int,
    val todayIsWorkDay: Boolean,
) {
    val todayCounts: Boolean get() = todayIsWorkDay && todayDone >= goal
}

data class PetProfileUiState(
    val pet: ActivePet,
    /** The live mood and energy, so the profile shows the same pet as Home. */
    val mood: Mood,
    val energy: Float,
    val streak: StreakCardUi,
    /** Every completed break so far (repeats included). */
    val totalBreaks: Int,
    /** Seven bars, oldest first, ending today. */
    val week: List<DayBar>,
) {
    val weekTotal: Int get() = week.sumOf { it.total }
}

/** Pure mapping from the break log to the profile (unit-tested). */
fun toPetProfileUiState(
    pet: ActivePet,
    mood: Mood,
    energy: Float,
    done: List<DoneBreak>,
    today: LocalDate,
    zone: ZoneId,
    isWorkDay: (LocalDate) -> Boolean,
    goal: Int,
): PetProfileUiState {
    val streak = StreakCalculator.compute(done, zone, today, isWorkDay, goal)
    return PetProfileUiState(
        pet = pet,
        mood = mood,
        energy = energy,
        streak = StreakCardUi(
            days = streak.days,
            todayDone = streak.todayDistinct,
            goal = streak.goal,
            todayIsWorkDay = streak.todayIsWorkDay,
        ),
        totalBreaks = done.size,
        week = WeeklyChart.lastSevenDays(done, today, zone),
    )
}
