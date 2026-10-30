package com.kartikkhandelwal.wigglebun.ui.onboarding

import com.kartikkhandelwal.wigglebun.domain.sitting.WorkSchedule
import java.time.DayOfWeek

/** The work-hours answer being edited; nothing is saved until Continue. */
data class WorkHoursDraft(
    val days: Set<DayOfWeek> = WorkSchedule.Default.days,
    val startMinute: Int = WorkSchedule.Default.startMinute,
    val endMinute: Int = WorkSchedule.Default.endMinute,
) {
    /** Why Continue isn't available, or null if it is. */
    val problem: WorkHoursProblem?
        get() = when {
            days.isEmpty() -> WorkHoursProblem.NO_DAYS
            startMinute == endMinute % WorkSchedule.MINUTES_PER_DAY -> WorkHoursProblem.SAME_TIME
            else -> null
        }

    /** Finishes after midnight. Ending exactly at midnight still ends the same day. */
    val endsNextDay: Boolean get() = endMinute != 0 && endMinute != WorkSchedule.MINUTES_PER_DAY && endMinute < startMinute

    fun toggled(day: DayOfWeek): WorkHoursDraft = copy(days = if (day in days) days - day else days + day)

    fun toSchedule(): WorkSchedule = WorkSchedule(days, startMinute, endMinute)

    companion object {
        fun of(schedule: WorkSchedule) = WorkHoursDraft(schedule.days, schedule.startMinute, schedule.endMinute)
    }
}

enum class WorkHoursProblem { NO_DAYS, SAME_TIME }
