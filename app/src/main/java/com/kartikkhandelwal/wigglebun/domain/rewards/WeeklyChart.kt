package com.kartikkhandelwal.wigglebun.domain.rewards

import com.kartikkhandelwal.wigglebun.domain.model.BreakCategory
import com.kartikkhandelwal.wigglebun.domain.model.DoneBreak
import java.time.LocalDate
import java.time.ZoneId

/**
 * One day of the Pet profile's chart: completed breaks per category. Bars stack by
 * [BreakCategory] rather than by break type: seven types share six colours (Shoulder rolls has
 * none of its own), so stacking by type would draw two blues side by side.
 */
data class DayBar(val date: LocalDate, val counts: Map<BreakCategory, Int>) {
    /** Every completed break that day, repeats included (the number above the bar). */
    val total: Int get() = counts.values.sum()

    /** Non-empty segments, bottom to top, in [WeeklyChart.STACK_ORDER]. */
    val segments: List<Pair<BreakCategory, Int>>
        get() = WeeklyChart.STACK_ORDER.mapNotNull { category -> counts[category]?.takeIf { it > 0 }?.let { category to it } }
}

/** The "Last 7 days" chart (CLAUDE.md §6.6). Pure: days are local calendar dates in [zone]. */
object WeeklyChart {
    const val DAYS = 7

    /**
     * Bottom to top (and the legend order). Stretch blue and Eyes lilac look the same to
     * red-green colour-blind eyes, so they are never stacked next to each other; this order keeps
     * every neighbouring pair apart (colour-vision ΔE ≥ 8, dataviz palette check).
     */
    val STACK_ORDER: List<BreakCategory> =
        listOf(BreakCategory.STRETCH, BreakCategory.MOVEMENT, BreakCategory.EYES, BreakCategory.WATER)

    /** Seven bars, oldest first and ending [today]; days with no breaks are included, empty. */
    fun lastSevenDays(done: List<DoneBreak>, today: LocalDate, zone: ZoneId): List<DayBar> {
        val first = today.minusDays(DAYS - 1L)
        // Grouping by local date (not by 24-hour blocks) keeps DST days, 23 or 25 hours long, whole.
        val byDate = done
            .map { it.at.atZone(zone).toLocalDate() to it.type.category }
            .filter { (date, _) -> !date.isBefore(first) && !date.isAfter(today) }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
        return (0 until DAYS).map { offset ->
            val date = first.plusDays(offset.toLong())
            DayBar(date, byDate[date].orEmpty().groupingBy { it }.eachCount())
        }
    }
}
