package com.kartikkhandelwal.wigglebun.domain.sitting

import java.time.DayOfWeek
import java.time.LocalDate

/** From [from] on, [days] were the work days. */
data class WorkDaysChange(val from: LocalDate, val days: Set<DayOfWeek>)

/**
 * Which weekdays were work days over time, so the streak judges each past day by the work days
 * in force back then: changing work days never breaks (or rescues) a streak retroactively.
 */
data class WorkDayHistory(
    /** Sorted by [WorkDaysChange.from]. Empty means "[current], since forever". */
    val changes: List<WorkDaysChange>,
    val current: Set<DayOfWeek>,
) {
    fun daysOn(date: LocalDate): Set<DayOfWeek> =
        changes.lastOrNull { !it.from.isAfter(date) }?.days ?: changes.firstOrNull()?.days ?: current

    fun isWorkDay(date: LocalDate): Boolean = date.dayOfWeek in daysOn(date)

    /** The history after switching to [newDays] on [today]; a same-day re-change replaces today's entry. */
    fun withChange(newDays: Set<DayOfWeek>, today: LocalDate): WorkDayHistory {
        if (newDays == current) return this
        // The first change also records what applied before it.
        val base = changes.ifEmpty { listOf(WorkDaysChange(SINCE_FOREVER, current)) }
        return WorkDayHistory(base.filter { it.from.isBefore(today) } + WorkDaysChange(today, newDays), newDays)
    }

    companion object {
        // Not LocalDate.EPOCH: that field only exists on API 34+ and crashes older phones.
        val SINCE_FOREVER: LocalDate = LocalDate.of(1970, 1, 1)
    }
}
