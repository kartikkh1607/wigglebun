package com.kartikkhandelwal.wigglebun.domain.sitting

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * When a break day starts: "Breaks for today", the once-a-day spark reward, the streak and the
 * weekly chart (CLAUDE.md §5.4). Midnight, except when work hours cross midnight: then the day
 * starts when the shift starts, so a 22:00–06:00 shift is one day, named after the evening it
 * began (as [WorkSchedule] names the shift). [BreakDays] applies a history of these.
 */
data class DayBoundary(val minuteOfDay: Int) {
    init {
        require(minuteOfDay in 0 until WorkSchedule.MINUTES_PER_DAY) { "minuteOfDay $minuteOfDay" }
    }

    /**
     * When [date] starts. Wall-clock time, so a day around a DST change is 23 or 25 hours long.
     * A boundary inside a DST gap moves to just after it, and inside an overlap it is the first
     * of the two (as atZone resolves them).
     */
    fun start(date: LocalDate, zone: ZoneId): Instant =
        date.atStartOfDay().plusMinutes(minuteOfDay.toLong()).atZone(zone).toInstant()

    /** When [date] ends: the next day's [start]. */
    fun end(date: LocalDate, zone: ZoneId): Instant = start(date.plusDays(1), zone)

    /**
     * The day [instant] belongs to: the one whose [start] is the latest at or before it. Checked
     * against [start], so it never goes backwards, even when a fall-back night repeats the hour
     * the boundary is in.
     */
    fun dayOf(instant: Instant, zone: ZoneId): LocalDate {
        val guess = instant.atZone(zone).toLocalDateTime().minusMinutes(minuteOfDay.toLong()).toLocalDate()
        return when {
            instant.isBefore(start(guess, zone)) -> guess.minusDays(1)
            !instant.isBefore(end(guess, zone)) -> guess.plusDays(1)
            else -> guess
        }
    }

    companion object {
        val MIDNIGHT = DayBoundary(0)

        /** The shift start when the hours cross midnight, else midnight. */
        fun of(schedule: WorkSchedule): DayBoundary =
            if (schedule.crossesMidnight) DayBoundary(schedule.startMinute) else MIDNIGHT
    }
}
