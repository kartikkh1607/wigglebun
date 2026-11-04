package com.kartikkhandelwal.wigglebun.ui.onboarding

import com.kartikkhandelwal.wigglebun.domain.sitting.WorkSchedule
import java.time.DayOfWeek

/** The work-hours answer being edited; nothing is saved until Continue. */
data class WorkHoursDraft(
    val days: Set<DayOfWeek> = WorkSchedule.Default.days,
    val startMinute: Int = WorkSchedule.Default.startMinute,
    val endMinute: Int = WorkSchedule.Default.endMinute,
) {
    /** Why these hours can't be saved, or null if they can. */
    val problem: WorkHoursProblem?
        get() = when {
            days.isEmpty() -> WorkHoursProblem.NO_DAYS
            startMinute == endMinute -> WorkHoursProblem.SAME_TIME
            else -> null
        }

    /** Continue or Save is off, straight away: without a work day there's nothing to save. */
    val canSave: Boolean get() = days.isNotEmpty()

    /**
     * The reason to show. No work days shows at once (and [canSave] is false). The same start and
     * end only shows once Continue or Save found it ([checked]), because it's often a step on the
     * way, as in 9–18 → 18–18 → 18–02.
     */
    fun shownProblem(checked: WorkHoursProblem?): WorkHoursProblem? =
        if (days.isEmpty()) WorkHoursProblem.NO_DAYS else checked

    /** Finishes after midnight. Ending exactly at midnight still ends the same day. */
    val endsNextDay: Boolean get() = endMinute != 0 && endMinute != WorkSchedule.MINUTES_PER_DAY && endMinute < startMinute

    fun toggled(day: DayOfWeek): WorkHoursDraft = copy(days = if (day in days) days - day else days + day)

    fun toSchedule(): WorkSchedule = WorkSchedule(days, startMinute, endMinute)

    companion object {
        fun of(schedule: WorkSchedule) = WorkHoursDraft(schedule.days, schedule.startMinute, schedule.endMinute)
    }
}

enum class WorkHoursProblem { NO_DAYS, SAME_TIME }
