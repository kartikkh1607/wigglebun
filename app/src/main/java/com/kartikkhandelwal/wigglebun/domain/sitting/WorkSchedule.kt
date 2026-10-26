package com.kartikkhandelwal.wigglebun.domain.sitting

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** One stretch of work hours, as absolute instants. */
data class WorkSession(val start: Instant, val end: Instant) {
    operator fun contains(instant: Instant): Boolean = !instant.isBefore(start) && instant.isBefore(end)
}

/**
 * Work hours in local time. [startMinute]/[endMinute] are minutes after midnight (end may be
 * 1440 = midnight). If [endMinute] <= [startMinute] the session runs overnight and belongs to
 * the day it starts on, e.g. Fri 22:00 → Sat 06:00 counts as Friday.
 */
data class WorkSchedule(
    val days: Set<DayOfWeek>,
    val startMinute: Int,
    val endMinute: Int,
) {
    init {
        require(startMinute in 0 until MINUTES_PER_DAY) { "startMinute $startMinute" }
        require(endMinute in 0..MINUTES_PER_DAY) { "endMinute $endMinute" }
    }

    val isOvernight: Boolean get() = endMinute <= startMinute

    /** The session that starts on [date], or null if [date] is not a work day. */
    fun sessionStartingOn(date: LocalDate, zone: ZoneId): WorkSession? {
        if (date.dayOfWeek !in days) return null
        val endDate = if (isOvernight) date.plusDays(1) else date
        // atZone resolves DST gaps/overlaps, so a 02:30 start on a spring-forward day still works.
        val start = date.atTime(LocalTime.MIDNIGHT).plusMinutes(startMinute.toLong()).atZone(zone)
        val end = endDate.atTime(LocalTime.MIDNIGHT).plusMinutes(endMinute.toLong()).atZone(zone)
        return WorkSession(start.toInstant(), end.toInstant())
    }

    /** The session [now] falls in, if any. Checks yesterday too, for overnight sessions. */
    fun sessionAt(now: Instant, zone: ZoneId): WorkSession? {
        val today = now.atZone(zone).toLocalDate()
        return sessionStartingOn(today, zone)?.takeIf { now in it }
            ?: sessionStartingOn(today.minusDays(1), zone)?.takeIf { now in it }
    }

    /** The first session starting after [now], or null if no day is a work day. */
    fun nextSessionAfter(now: Instant, zone: ZoneId): WorkSession? {
        val today = now.atZone(zone).toLocalDate()
        for (offset in 0L..7L) {
            val session = sessionStartingOn(today.plusDays(offset), zone) ?: continue
            if (session.start.isAfter(now)) return session
        }
        return null
    }

    companion object {
        const val MINUTES_PER_DAY = 24 * 60

        /** CLAUDE.md §5.1 default: Mon–Fri 09:00–18:00. */
        val Default = WorkSchedule(
            days = setOf(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
            ),
            startMinute = 9 * 60,
            endMinute = 18 * 60,
        )

        /** Every day, all day (debug: test reminders at any time). */
        val AlwaysOn = WorkSchedule(days = DayOfWeek.entries.toSet(), startMinute = 0, endMinute = MINUTES_PER_DAY)
    }
}
