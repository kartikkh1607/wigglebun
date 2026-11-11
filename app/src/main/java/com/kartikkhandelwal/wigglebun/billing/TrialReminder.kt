package com.kartikkhandelwal.wigglebun.billing

import java.time.Duration
import java.time.Instant

/** What to do about the "Your free trial ends on …" notification for a premium answer (CLAUDE.md §5.5). */
sealed interface TrialReminderPlan {
    /** Show it at [time]: [Trial.END_REMINDER_DAYS_BEFORE] days before the trial ends. */
    data class At(val time: Instant) : TrialReminderPlan

    /** Nothing to remind about: no trial, a paid plan, or a trial the user already cancelled. */
    data object None : TrialReminderPlan

    /** Leave whatever is scheduled alone: premium isn't known yet, or the reminder's time has passed. */
    data object Unchanged : TrialReminderPlan
}

/**
 * Pure rule for the trial-end reminder. Only a trial that will turn into a paid plan gets one:
 * after cancelling there is no charge to warn about.
 *
 * A reminder time already in the past is [TrialReminderPlan.Unchanged], not "now": the scheduled
 * one has fired by then, and this runs on every app start, so "now" would repeat it. It also
 * means Play's 3-minute test trials never get one.
 */
fun trialReminderPlan(state: PremiumState, now: Instant): TrialReminderPlan = when (state) {
    PremiumState.Unknown, PremiumState.Unavailable -> TrialReminderPlan.Unchanged
    PremiumState.NotEntitled -> TrialReminderPlan.None
    is PremiumState.Entitled -> {
        val ends = state.expiresAt
        if (!state.isTrial || !state.willRenew || ends == null) {
            TrialReminderPlan.None
        } else {
            val at = ends.minus(Duration.ofDays(Trial.END_REMINDER_DAYS_BEFORE.toLong()))
            if (at.isAfter(now)) TrialReminderPlan.At(at) else TrialReminderPlan.Unchanged
        }
    }
}

/** Whether the reminder should still be shown when its time comes: the trial is running and not cancelled. */
fun trialEndToRemindAbout(state: PremiumState, now: Instant): Instant? {
    val trial = state as? PremiumState.Entitled ?: return null
    val ends = trial.expiresAt ?: return null
    return ends.takeIf { trial.isTrial && trial.willRenew && it.isAfter(now) }
}
