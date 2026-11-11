package com.kartikkhandelwal.wigglebun.billing

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** When the "Your free trial ends on …" notification is due (CLAUDE.md §5.5). */
class TrialReminderTest {

    private val start = Instant.parse("2026-11-11T10:00:00Z")
    private val ends = start.plus(Duration.ofDays(Trial.FREE_TRIAL_DAYS.toLong()))
    private val trial = PremiumState.Entitled(isTrial = true, expiresAt = ends, willRenew = true, isLifetime = false)

    /** Install day is day 1: the reminder comes on day 6, five days in, two before the end. */
    @Test
    fun aTrialIsRemindedTwoDaysBeforeItEnds() {
        assertEquals(TrialReminderPlan.At(start.plus(Duration.ofDays(5))), trialReminderPlan(trial, start))
    }

    @Test
    fun noReminderWithoutATrialThatWillBeCharged() {
        assertEquals(TrialReminderPlan.None, trialReminderPlan(PremiumState.NotEntitled, start))
        assertEquals(TrialReminderPlan.None, trialReminderPlan(trial.copy(willRenew = false), start))
        assertEquals(TrialReminderPlan.None, trialReminderPlan(trial.copy(isTrial = false), start))
        assertEquals(
            TrialReminderPlan.None,
            trialReminderPlan(PremiumState.Entitled(isTrial = false, expiresAt = null, willRenew = false, isLifetime = true), start),
        )
    }

    /** A cold start knows nothing for a moment: that must not drop a reminder already scheduled. */
    @Test
    fun notKnowingYetLeavesTheScheduleAlone() {
        assertEquals(TrialReminderPlan.Unchanged, trialReminderPlan(PremiumState.Unknown, start))
        assertEquals(TrialReminderPlan.Unchanged, trialReminderPlan(PremiumState.Unavailable, start))
    }

    /** Runs on every app start: once the time has passed it must not fire again. */
    @Test
    fun aReminderWhoseTimeHasPassedIsNotRepeated() {
        assertEquals(TrialReminderPlan.Unchanged, trialReminderPlan(trial, start.plus(Duration.ofDays(5))))
        assertEquals(TrialReminderPlan.Unchanged, trialReminderPlan(trial, start.plus(Duration.ofDays(6))))
    }

    @Test
    fun theWorkerRemindsOnlyAboutATrialStillRunningAndNotCancelled() {
        val due = start.plus(Duration.ofDays(5))
        assertEquals(ends, trialEndToRemindAbout(trial, due))
        assertNull(trialEndToRemindAbout(trial.copy(willRenew = false), due))
        assertNull(trialEndToRemindAbout(trial.copy(isTrial = false), due))
        assertNull(trialEndToRemindAbout(trial, ends))
        assertNull(trialEndToRemindAbout(PremiumState.NotEntitled, due))
        assertNull(trialEndToRemindAbout(PremiumState.Unknown, due))
    }
}
