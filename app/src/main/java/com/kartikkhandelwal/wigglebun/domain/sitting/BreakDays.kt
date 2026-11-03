package com.kartikkhandelwal.wigglebun.domain.sitting

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** From [at] on, break days turn at [boundary]. */
data class DayBoundaryChange(val at: Instant, val boundary: DayBoundary)

/**
 * When break days have turned over time. Empty means midnight, which is how every day worked
 * before work hours that cross midnight got days of their own. Kept so a change of hours never
 * moves a past break to another day (which would rewrite the streak and the weekly chart).
 */
data class DayBoundaryHistory(
    /** In the order they happened. */
    val changes: List<DayBoundaryChange> = emptyList(),
) {
    val current: DayBoundary get() = changes.lastOrNull()?.boundary ?: DayBoundary.MIDNIGHT

    /** The history after break days start turning at [boundary] at [at]; unchanged if they already do. */
    fun withChange(boundary: DayBoundary, at: Instant): DayBoundaryHistory =
        if (boundary == current) this else DayBoundaryHistory(changes + DayBoundaryChange(at, boundary))
}

/** The instants of one break day: [start] inclusive, [end] exclusive. */
data class DaySpan(val start: Instant, val end: Instant)

/**
 * Which break day an instant belongs to, given the [DayBoundaryHistory] (CLAUDE.md §5.4). Pure.
 *
 * A change never renames the day in progress or moves a break already done:
 * - The day in progress when the boundary changes carries on until the new boundary starts a
 *   later day. Moving the boundary later (midnight → 22:00 on Monday afternoon) makes Monday last
 *   until Tuesday 22:00, so Monday night's shift is Monday's, as the hours say.
 * - Moving it earlier (22:00 → midnight on Tuesday afternoon, during Monday's day) can leave a date
 *   with no time at all (Tuesday). [exists] is false for it: no breaks, and not a missed work day.
 */
class BreakDays(history: DayBoundaryHistory, private val zone: ZoneId) {

    /** From [from], days turn at [boundary]; before [carriedUntil] it is still [carried]. */
    private class Era(
        val from: Instant,
        val boundary: DayBoundary,
        val carried: LocalDate?,
        val carriedUntil: Instant,
        /** The first day of this era's own, which starts at [carriedUntil]. */
        val firstOwnDay: LocalDate?,
    )

    private val eras: List<Era>
    private val skipped: Set<LocalDate>

    init {
        val list = mutableListOf(Era(Instant.MIN, DayBoundary.MIDNIGHT, carried = null, carriedUntil = Instant.MIN, firstOwnDay = null))
        history.changes.sortedBy { it.at }.forEach { change ->
            val inProgress = list.last().dayOf(change.at)
            var next = inProgress.plusDays(1)
            while (!change.boundary.start(next, zone).isAfter(change.at)) next = next.plusDays(1)
            list += Era(change.at, change.boundary, inProgress, change.boundary.start(next, zone), next)
        }
        eras = list
        // A date between the carried day and the era's first own day never starts, unless another
        // change came first (then the next era carries the same day on).
        skipped = buildSet {
            list.forEachIndexed { i, era ->
                val carried = era.carried ?: return@forEachIndexed
                val firstOwn = era.firstOwnDay ?: return@forEachIndexed
                val nextFrom = list.getOrNull(i + 1)?.from
                if (nextFrom != null && !era.carriedUntil.isBefore(nextFrom)) return@forEachIndexed
                var date = carried.plusDays(1)
                while (date.isBefore(firstOwn)) {
                    add(date)
                    date = date.plusDays(1)
                }
            }
        }
    }

    private fun Era.dayOf(instant: Instant): LocalDate =
        if (carried != null && instant.isBefore(carriedUntil)) carried else boundary.dayOf(instant, zone)

    /** The break day [instant] belongs to. Never goes backwards as [instant] moves on. */
    fun dayOf(instant: Instant): LocalDate = eras.last { !it.from.isAfter(instant) }.dayOf(instant)

    /** False for a date a change of boundary skipped: it has no breaks and isn't a missed work day. */
    fun exists(date: LocalDate): Boolean = date !in skipped

    /** When [date] starts and ends, or null if it was skipped. */
    fun span(date: LocalDate): DaySpan? {
        if (!exists(date)) return null
        val start = firstInstant(date) { !dayOf(it).isBefore(date) }
        val end = firstInstant(date) { dayOf(it).isAfter(date) }
        return DaySpan(start, end)
    }

    /**
     * The first millisecond near [date] where [reached] holds. [dayOf] never goes backwards, so
     * once [reached] holds it keeps holding, and a binary search finds the change.
     */
    private fun firstInstant(date: LocalDate, reached: (Instant) -> Boolean): Instant {
        var low = date.atStartOfDay(zone).toInstant().minus(SEARCH_STEP)
        repeat(MAX_STEPS) { if (reached(low)) low = low.minus(SEARCH_STEP) }
        var high = date.atStartOfDay(zone).toInstant().plus(SEARCH_STEP)
        repeat(MAX_STEPS) { if (!reached(high)) high = high.plus(SEARCH_STEP) }
        var lo = low.toEpochMilli() // never reached
        var hi = high.toEpochMilli() // reached
        while (hi - lo > 1) {
            val mid = lo + (hi - lo) / 2
            if (reached(Instant.ofEpochMilli(mid))) hi = mid else lo = mid
        }
        return Instant.ofEpochMilli(hi)
    }

    private companion object {
        val SEARCH_STEP: Duration = Duration.ofDays(2)

        /** A carried day lasts at most about two days per change, so this is far more than enough. */
        const val MAX_STEPS = 16
    }
}
