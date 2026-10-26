package com.kartikkhandelwal.wigglebun.domain.sitting

import com.kartikkhandelwal.wigglebun.domain.model.Mood
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * The sitting rules (CLAUDE.md §5.1, §5.2). Pure and stateless: every call reads the time
 * from [clock] and returns a new [SitState] or a [SitStatus]; callers persist the state.
 *
 * Timer rules:
 * - Only time inside a work session counts. The effective start is the later of the last
 *   reset and the current session's start, so every session starts fresh and the pet
 *   sleeps (no guilt) outside work hours.
 * - The reminder is due one interval after the effective start, or at the snooze time.
 *   A due that would land after the session ends is dropped.
 * - Each due is notified once ([SitState.lastNotifiedDue]); no repeated nagging.
 */
class SittingEngine(private val clock: Clock) {

    fun status(state: SitState, settings: SittingSettings): SitStatus {
        val now = clock.instant()
        val zone = clock.zone
        val schedule = settings.schedule
        val interval = settings.interval
        val celebrating = state.celebrateUntil?.isAfter(now) == true

        val session = schedule.sessionAt(now, zone)
        if (session == null) {
            val next = schedule.nextSessionAfter(now, zone)
            return SitStatus(
                now = now,
                isWorkTime = false,
                sitDuration = Duration.ZERO,
                intervalMinutes = interval,
                energy = 1f,
                mood = if (celebrating) Mood.CELEBRATE else Mood.SLEEPING,
                reminderDue = null,
                shouldNotify = false,
                nextAlarmAt = next?.let { firstDueIn(it, interval) },
                snoozeCount = 0,
                sessionEnd = null,
                nextSessionStart = next?.start,
            )
        }

        val start = effectiveStart(state, session, now)
        val sit = Duration.between(start, now)
        // A snooze only counts if it was set during this sit (not left over from an earlier one).
        val snoozeActive = state.snoozedUntil?.isAfter(start) == true
        val snoozeCount = if (snoozeActive) state.snoozeCount else 0
        val due = if (snoozeActive) state.snoozedUntil else start.plus(Duration.ofMinutes(interval.toLong()))
        val dueInSession = due.isBefore(session.end)
        val alreadyNotified = state.lastNotifiedDue == due

        val nextSession = schedule.nextSessionAfter(session.end.minusMillis(1), zone)
        val nextAlarm = when {
            dueInSession && !alreadyNotified -> due
            else -> nextSession?.let { firstDueIn(it, interval) }
        }

        val sitMinutes = sit.toMillis() / 60_000f
        val fraction = sitMinutes / interval
        return SitStatus(
            now = now,
            isWorkTime = true,
            sitDuration = sit,
            intervalMinutes = interval,
            energy = energyFor(sitMinutes, interval),
            mood = when {
                celebrating -> Mood.CELEBRATE
                fraction < 0.5f -> Mood.HAPPY
                fraction <= 1f -> Mood.TIRED
                else -> Mood.STIFF
            },
            reminderDue = due.takeIf { dueInSession },
            shouldNotify = dueInSession && !now.isBefore(due) && !alreadyNotified,
            nextAlarmAt = nextAlarm,
            snoozeCount = snoozeCount,
            sessionEnd = session.end,
            nextSessionStart = nextSession?.start,
        )
    }

    /** User finished a break: reset the timer, clear snoozes, celebrate for 3 s. */
    fun done(state: SitState): SitState {
        val now = clock.instant()
        return state.copy(
            sitStartedAt = now,
            snoozeCount = 0,
            snoozedUntil = null,
            celebrateUntil = now.plus(CELEBRATE_DURATION),
            lastBreakAt = now,
        )
    }

    /** Skip: reset the timer without reward or celebration. */
    fun skip(state: SitState): SitState = state.copy(
        sitStartedAt = clock.instant(),
        snoozeCount = 0,
        snoozedUntil = null,
        celebrateUntil = null,
    )

    /** Push the reminder by 10 min, at most [MAX_SNOOZES] times in a row. */
    fun snooze(state: SitState, settings: SittingSettings): SnoozeResult {
        val count = status(state, settings).snoozeCount
        if (count >= MAX_SNOOZES) return SnoozeResult.LimitReached
        val until = clock.instant().plus(SNOOZE_DURATION)
        return SnoozeResult.Snoozed(state.copy(snoozeCount = count + 1, snoozedUntil = until), until)
    }

    fun markNotified(state: SitState, due: Instant): SitState = state.copy(lastNotifiedDue = due)

    /** Start a fresh sit now (first launch, debug tools, clock changes). Keeps the break history. */
    fun resetTimer(state: SitState): SitState = SitState(sitStartedAt = clock.instant(), lastBreakAt = state.lastBreakAt)

    private fun effectiveStart(state: SitState, session: WorkSession, now: Instant): Instant {
        // A start in the future (clock moved back, debug fast clock switched off) counts as now.
        val started = state.sitStartedAt?.let { if (it.isAfter(now)) now else it } ?: session.start
        return if (started.isBefore(session.start)) session.start else started
    }

    private fun firstDueIn(session: WorkSession, interval: Int): Instant? =
        session.start.plus(Duration.ofMinutes(interval.toLong())).takeIf { it.isBefore(session.end) }

    companion object {
        const val MAX_SNOOZES = 3
        val SNOOZE_DURATION: Duration = Duration.ofMinutes(10)
        val CELEBRATE_DURATION: Duration = Duration.ofSeconds(3)
        const val MIN_ENERGY = 0.15f

        /** CLAUDE.md §5.2: energy = clamp(1 − sitMinutes / (interval × 1.2), 0.15, 1). */
        fun energyFor(sitMinutes: Float, interval: Int): Float =
            (1f - sitMinutes / (interval * 1.2f)).coerceIn(MIN_ENERGY, 1f)
    }
}
