package com.kartikkhandelwal.wigglebun.domain.sitting

import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.domain.model.MovementTransition
import com.kartikkhandelwal.wigglebun.domain.model.ResetReason
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
 *
 * Movement rules (M4):
 * - A moving period (walking/running) that lasts ≥ 2 min resets the timer at the moment it
 *   ends, so the sit restarts when you sit back down. Shorter periods change nothing, even if
 *   several short walks are separated by STILL.
 * - While a period has already lasted ≥ 2 min the timer is held at zero (no reminder mid-walk).
 * - ≥ 100 steps since the timer started also resets it.
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
                lastResetReason = state.lastResetReason,
            )
        }

        val onWalk = isOnWalk(state, now)
        // On a long-enough walk the timer is held at zero until the walk ends.
        val start = if (onWalk) now else effectiveStart(state, session, now)
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
            isOnWalk = onWalk,
            lastResetReason = state.lastResetReason,
        )
    }

    /** Feed one Activity Recognition transition (in chronological order). */
    fun onMovement(state: SitState, transition: MovementTransition): SitState {
        val activity = transition.activity
        // Play services replays the current state (with its original time) on registration.
        // An event from before the open moving period started is stale: it changes nothing,
        // so it can neither end the walk nor drop the activities that are still active.
        val since = state.movingSince
        if (since != null && transition.at.isBefore(since)) return state
        return when {
            activity.isMoving && transition.entered -> state.copy(
                activeMoving = state.activeMoving + activity,
                movingSince = since ?: transition.at,
            )
            activity.isMoving -> {
                val remaining = state.activeMoving - activity
                if (remaining.isEmpty()) closeMovingPeriod(state, transition.at) else state.copy(activeMoving = remaining)
            }
            // Entering STILL ends any moving period; leaving STILL alone proves nothing yet.
            transition.entered -> closeMovingPeriod(state, transition.at)
            else -> state
        }
    }

    /** Step-counter fallback: [MIN_STEPS] or more steps since the timer started resets it. */
    fun onSteps(state: SitState, stepsSinceTimerStart: Long): SitState =
        if (stepsSinceTimerStart >= MIN_STEPS) resetAt(state, clock.instant(), ResetReason.STEPS) else state

    /** Forget an open moving period (movement detection was switched off or lost). */
    fun clearMovement(state: SitState): SitState = state.copy(movingSince = null, activeMoving = emptySet())

    /** The moving period really ended at [end]: clear it, and reset the timer if it lasted ≥ 2 min. */
    private fun closeMovingPeriod(state: SitState, end: Instant): SitState {
        val cleared = state.copy(movingSince = null, activeMoving = emptySet())
        val since = state.movingSince ?: return cleared
        return if (Duration.between(since, end) >= MIN_WALK) resetAt(cleared, end, ResetReason.WALKING) else cleared
    }

    /** Restart the sit at [at]; never moves the timer backwards (late events after a Done). */
    private fun resetAt(state: SitState, at: Instant, reason: ResetReason): SitState {
        if (state.sitStartedAt != null && !at.isAfter(state.sitStartedAt)) return state
        return state.copy(
            sitStartedAt = at,
            snoozeCount = 0,
            snoozedUntil = null,
            lastResetReason = reason,
        )
    }

    private fun isOnWalk(state: SitState, now: Instant): Boolean {
        val since = state.movingSince ?: return false
        val walked = Duration.between(since, now)
        // A period open for hours means we missed its exit event; ignore it.
        return walked >= MIN_WALK && walked < STALE_WALK
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
            lastResetReason = ResetReason.DONE,
        )
    }

    /** Skip: reset the timer without reward or celebration. */
    fun skip(state: SitState): SitState = state.copy(
        sitStartedAt = clock.instant(),
        snoozeCount = 0,
        snoozedUntil = null,
        celebrateUntil = null,
        lastResetReason = ResetReason.SKIP,
    )

    /** Push the reminder by 10 min, at most [MAX_SNOOZES] times in a row. */
    fun snooze(state: SitState, settings: SittingSettings): SnoozeResult {
        val status = status(state, settings)
        val due = status.reminderDue
        // Only a due reminder can be snoozed: snoozing earlier would pull the reminder forward
        // (e.g. 10 min after a 10:05 tap instead of the 10:45 due).
        if (due == null || status.now.isBefore(due)) return SnoozeResult.NotDue
        val count = status.snoozeCount
        if (count >= MAX_SNOOZES) return SnoozeResult.LimitReached
        val until = clock.instant().plus(SNOOZE_DURATION)
        return SnoozeResult.Snoozed(state.copy(snoozeCount = count + 1, snoozedUntil = until), until)
    }

    fun markNotified(state: SitState, due: Instant): SitState = state.copy(lastNotifiedDue = due)

    /** Start a fresh sit now (first launch, debug tools, clock changes). Keeps the break history. */
    fun resetTimer(state: SitState): SitState =
        SitState(sitStartedAt = clock.instant(), lastBreakAt = state.lastBreakAt, lastResetReason = ResetReason.MANUAL)

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

        /** CLAUDE.md §5.1: a walk must last at least this long to count. */
        val MIN_WALK: Duration = Duration.ofMinutes(2)
        val STALE_WALK: Duration = Duration.ofHours(3)
        const val MIN_STEPS = 100L

        /** CLAUDE.md §5.2: energy = clamp(1 − sitMinutes / (interval × 1.2), 0.15, 1). */
        fun energyFor(sitMinutes: Float, interval: Int): Float =
            (1f - sitMinutes / (interval * 1.2f)).coerceIn(MIN_ENERGY, 1f)
    }
}
