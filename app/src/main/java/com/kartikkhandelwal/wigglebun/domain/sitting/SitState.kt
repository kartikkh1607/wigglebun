package com.kartikkhandelwal.wigglebun.domain.sitting

import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.domain.model.MovementActivity
import com.kartikkhandelwal.wigglebun.domain.model.ResetReason
import java.time.Duration
import java.time.Instant

/** User settings that drive the engine (CLAUDE.md §5.1). */
data class SittingSettings(
    val schedule: WorkSchedule = WorkSchedule.Default,
    val intervalMinutes: Int = DEFAULT_INTERVAL_MINUTES,
) {
    /** Always within 20..90, whatever was stored. */
    val interval: Int get() = intervalMinutes.coerceIn(MIN_INTERVAL_MINUTES, MAX_INTERVAL_MINUTES)

    companion object {
        const val DEFAULT_INTERVAL_MINUTES = 45
        const val MIN_INTERVAL_MINUTES = 20
        const val MAX_INTERVAL_MINUTES = 90
    }
}

/**
 * Everything the engine needs to remember between app runs. Persisted as-is (all instants
 * are UTC), so the timer survives process death and reboots.
 */
data class SitState(
    /** When the current sit started (last Done/Skip/reset). Null = never started. */
    val sitStartedAt: Instant? = null,
    val snoozeCount: Int = 0,
    val snoozedUntil: Instant? = null,
    /** The reminder due time that was already notified, so each due fires once. */
    val lastNotifiedDue: Instant? = null,
    val celebrateUntil: Instant? = null,
    val lastBreakAt: Instant? = null,
    val lastResetReason: ResetReason? = null,
    /** Start of the current moving period (walking/running), or null when not moving. */
    val movingSince: Instant? = null,
    /** Moving activities currently entered, so WALKING → RUNNING stays one period. */
    val activeMoving: Set<MovementActivity> = emptySet(),
)

/** A snapshot computed by [SittingEngine.status]. */
data class SitStatus(
    val now: Instant,
    val isWorkTime: Boolean,
    /** Time sat in the current work session (zero outside work hours). */
    val sitDuration: Duration,
    val intervalMinutes: Int,
    val energy: Float,
    val mood: Mood,
    /** When this session's reminder is due; null outside work hours or if it would land after the session. */
    val reminderDue: Instant?,
    /** True once [reminderDue] has passed and has not been notified yet. */
    val shouldNotify: Boolean,
    /** When an alarm should next wake us (this session's due, or the next session's first due). */
    val nextAlarmAt: Instant?,
    val snoozeCount: Int,
    val sessionEnd: Instant?,
    val nextSessionStart: Instant?,
    /** Moving for 2+ min right now: the sit timer is held at zero until the walk ends. */
    val isOnWalk: Boolean = false,
    val lastResetReason: ResetReason? = null,
) {
    /**
     * When something needs re-evaluating: the next reminder, or the next work-hours boundary
     * (start/end), so movement detection is switched on and off with work hours.
     */
    val nextWakeAt: Instant?
        get() = listOfNotNull(nextAlarmAt, sessionEnd ?: nextSessionStart).minOrNull()

    val sitMinutes: Int get() = sitDuration.toMinutes().toInt()
    val sitFraction: Float get() = sitDuration.toMillis() / 60_000f / intervalMinutes
    val snoozesLeft: Int get() = (SittingEngine.MAX_SNOOZES - snoozeCount).coerceAtLeast(0)
}

sealed interface SnoozeResult {
    data class Snoozed(val state: SitState, val until: Instant) : SnoozeResult
    data object LimitReached : SnoozeResult

    /** Nothing to snooze yet (reminder not due, or outside work hours). */
    data object NotDue : SnoozeResult
}
