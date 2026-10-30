package com.kartikkhandelwal.wigglebun.domain.sitting

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.kartikkhandelwal.wigglebun.data.prefs.toSitState
import com.kartikkhandelwal.wigglebun.data.prefs.writeSitState
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.domain.model.MovementActivity.RUNNING
import com.kartikkhandelwal.wigglebun.domain.model.MovementActivity.STILL
import com.kartikkhandelwal.wigglebun.domain.model.MovementActivity.WALKING
import com.kartikkhandelwal.wigglebun.domain.model.MovementActivity
import com.kartikkhandelwal.wigglebun.domain.model.MovementTransition
import com.kartikkhandelwal.wigglebun.domain.model.ResetReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** CLAUDE.md §5.1 movement resets: ≥ 2 min of walking/running, or ≥ 100 steps. */
class MovementRulesTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private fun at(local: String): Instant = LocalDateTime.parse("2026-10-26T$local").atZone(zone).toInstant() // Monday

    private val clock = MutableClock(at("10:30"), zone)
    private val engine = SittingEngine(clock)
    private val settings = SittingSettings()
    private val sitting = SitState(sitStartedAt = at("10:00"))

    private fun SitState.feed(vararg events: Triple<MovementActivity, Boolean, String>): SitState =
        events.fold(this) { s, (activity, entered, time) -> engine.onMovement(s, MovementTransition(activity, entered, at(time))) }

    private fun enter(a: MovementActivity, time: String) = Triple(a, true, time)
    private fun exit(a: MovementActivity, time: String) = Triple(a, false, time)

    @Test
    fun ninetySecondsOfWalkingDoesNotReset() {
        val s = sitting.feed(enter(WALKING, "10:20:00"), exit(WALKING, "10:21:30"))
        assertEquals(at("10:00"), s.sitStartedAt)
        assertNull(s.movingSince)
        assertNull(s.lastResetReason)
    }

    @Test
    fun twoMinutesOfWalkingResetsWhenTheWalkEnds() {
        val s = sitting.feed(enter(WALKING, "10:20:00"), exit(WALKING, "10:22:00"))
        assertEquals(at("10:22"), s.sitStartedAt)
        assertEquals(ResetReason.WALKING, s.lastResetReason)
        clock.now = at("10:30")
        assertEquals(8, engine.status(s, settings).sitMinutes)
    }

    @Test
    fun enteringStillEndsTheWalk() {
        val s = sitting.feed(enter(WALKING, "10:20"), enter(STILL, "10:25"))
        assertEquals(at("10:25"), s.sitStartedAt)
        assertTrue(s.activeMoving.isEmpty())
    }

    @Test
    fun shortWalksSeparatedByStillDoNotAddUp() {
        val s = sitting.feed(
            enter(WALKING, "10:20:00"), enter(STILL, "10:21:00"),
            exit(STILL, "10:21:10"), enter(WALKING, "10:21:10"), enter(STILL, "10:22:10"),
        )
        assertEquals(at("10:00"), s.sitStartedAt)
        assertNull(s.lastResetReason)
    }

    @Test
    fun walkingIntoRunningIsOnePeriod() {
        val s = sitting.feed(
            enter(WALKING, "10:20:00"),
            enter(RUNNING, "10:20:50"), exit(WALKING, "10:20:55"),
            exit(RUNNING, "10:22:30"),
        )
        assertEquals(at("10:22:30"), s.sitStartedAt)
        assertEquals(ResetReason.WALKING, s.lastResetReason)
    }

    @Test
    fun lateBatchUsesEventTimesNotDeliveryTime() {
        clock.now = at("10:40") // delivered 15 min after the walk ended
        val s = sitting.feed(enter(WALKING, "10:20"), exit(WALKING, "10:25"))
        assertEquals(at("10:25"), s.sitStartedAt)
        assertEquals(15, engine.status(s, settings).sitMinutes)
    }

    @Test
    fun lateWalkNeverMovesTimerBackwardsPastADone() {
        clock.now = at("10:30")
        val done = engine.done(sitting) // Done at 10:30
        val s = done.feed(enter(WALKING, "10:20"), exit(WALKING, "10:25"))
        assertEquals(at("10:30"), s.sitStartedAt)
        assertEquals(ResetReason.DONE, s.lastResetReason)
    }

    @Test
    fun ongoingLongWalkHoldsTimerAtZeroAndNoReminder() {
        val walking = sitting.feed(enter(WALKING, "10:40"))
        clock.now = at("10:41") // only 1 min in: still counts as sitting
        assertFalse(engine.status(walking, settings).isOnWalk)
        assertEquals(41, engine.status(walking, settings).sitMinutes)

        clock.now = at("10:50") // 10 min walk, past the 10:45 due
        val status = engine.status(walking, settings)
        assertTrue(status.isOnWalk)
        assertEquals(Duration.ZERO, status.sitDuration)
        assertFalse(status.shouldNotify)
        assertEquals(Mood.HAPPY, status.mood)
        assertEquals(1f, status.energy)
    }

    /** Safety cap: a walk whose exit was lost is closed 45 min after it started, as a walk. */
    @Test
    fun walkWithLostExitIsClosedAfter45Minutes() {
        val walking = sitting.feed(enter(WALKING, "10:20"))
        clock.now = at("11:04:59") // just under the cap: still on the walk
        assertTrue(engine.status(walking, settings).isOnWalk)

        clock.now = at("13:30")
        val status = engine.status(walking, settings)
        assertFalse(status.isOnWalk)
        // The timer carries on from where the walk was capped (11:05), not from before the walk.
        assertEquals(145, status.sitMinutes)

        val closed = engine.closeOverlongWalk(walking)
        assertNull(closed.movingSince)
        assertTrue(closed.activeMoving.isEmpty())
        assertEquals(at("11:05"), closed.sitStartedAt)
        assertEquals(ResetReason.WALKING, closed.lastResetReason)
    }

    @Test
    fun aWalkUnderTheCapIsLeftOpen() {
        val walking = sitting.feed(enter(WALKING, "10:20"))
        clock.now = at("11:04")
        assertEquals(walking, engine.closeOverlongWalk(walking))
    }

    /** The lost exit arrives after the cap closed the walk: nothing more happens. */
    @Test
    fun aLateExitAfterTheCapChangesNothing() {
        val s = sitting.feed(enter(WALKING, "10:20"), exit(WALKING, "11:30"))
        assertEquals(at("11:05"), s.sitStartedAt)
        assertNull(s.movingSince)
    }

    /** An event after the cap first closes the old walk, then starts a new one. */
    @Test
    fun aNewWalkAfterTheCapStartsItsOwnPeriod() {
        val s = sitting.feed(enter(WALKING, "10:20"), enter(WALKING, "11:40"))
        assertEquals(at("11:05"), s.sitStartedAt)
        assertEquals(at("11:40"), s.movingSince)
    }

    /** Play services replays the current state with its original (older) time on registration. */
    @Test
    fun staleReplayedStillDoesNotEndARealWalk() {
        val walking = sitting.feed(enter(WALKING, "10:20"))
        val afterReplay = walking.feed(enter(STILL, "09:50")) // replay from before the walk
        assertEquals(at("10:20"), afterReplay.movingSince)
        val ended = afterReplay.feed(enter(STILL, "10:25"))
        assertEquals(at("10:25"), ended.sitStartedAt)
        assertEquals(ResetReason.WALKING, ended.lastResetReason)
    }

    /** Review item: a stale replay must not drop activities that are still active. */
    @Test
    fun staleStillKeepsActiveActivitiesSoRunningStillHoldsThePeriod() {
        val afterReplay = sitting.feed(
            enter(WALKING, "10:20"),
            enter(RUNNING, "10:22"),
            enter(STILL, "09:50"), // replayed, older than the open period
        )
        assertEquals(setOf(WALKING, RUNNING), afterReplay.activeMoving)

        val walkingEnded = afterReplay.feed(exit(WALKING, "10:24"))
        assertEquals(at("10:20"), walkingEnded.movingSince) // still open: RUNNING is active
        assertEquals(setOf(RUNNING), walkingEnded.activeMoving)
        assertEquals(at("10:00"), walkingEnded.sitStartedAt)

        val runningEnded = walkingEnded.feed(exit(RUNNING, "10:26"))
        assertEquals(at("10:26"), runningEnded.sitStartedAt)
        assertEquals(ResetReason.WALKING, runningEnded.lastResetReason)
        assertNull(runningEnded.movingSince)
        assertTrue(runningEnded.activeMoving.isEmpty())
    }

    @Test
    fun exitWithoutEnterIsHarmless() {
        val s = sitting.feed(exit(WALKING, "10:20"), exit(STILL, "10:21"))
        assertEquals(sitting, s)
    }

    @Test
    fun stepsResetAtOneHundred() {
        clock.now = at("10:30")
        assertEquals(sitting, engine.onSteps(sitting, 99))
        val reset = engine.onSteps(sitting, 100)
        assertEquals(at("10:30"), reset.sitStartedAt)
        assertEquals(ResetReason.STEPS, reset.lastResetReason)
    }

    /**
     * A step reset is stamped when its batch arrives, which can be after the walk really ended.
     * Without waiting for the walk, the later stamp wins and the walk's own end is thrown away.
     */
    @Test
    fun stepsArrivingBeforeTheWalkEndEventDoNotBeatTheWalk() {
        val walking = sitting.feed(enter(WALKING, "10:20"))
        clock.now = at("10:23:30") // batch arrives after the 10:23 end, before its STILL event
        assertEquals(walking, engine.onSteps(walking, 150))

        val ended = engine.onSteps(walking, 150).feed(enter(STILL, "10:23"))
        assertEquals(at("10:23"), ended.sitStartedAt)
        assertEquals(ResetReason.WALKING, ended.lastResetReason)
    }

    /** Steps are the fallback for walks that didn't count: too short, but ≥ 100 steps still reset. */
    @Test
    fun aShortWalkWithEnoughStepsResetsByStepsOnceItEnds() {
        val walking = sitting.feed(enter(WALKING, "10:20"))
        clock.now = at("10:21")
        assertEquals(walking, engine.onSteps(walking, 150))

        val ended = walking.feed(exit(WALKING, "10:21:30"))
        assertEquals(at("10:00"), ended.sitStartedAt) // under 2 min: the walk itself changes nothing
        clock.now = at("10:21:40")
        val reset = engine.onSteps(ended, 150)
        assertEquals(at("10:21:40"), reset.sitStartedAt)
        assertEquals(ResetReason.STEPS, reset.lastResetReason)
    }

    /** Detection lost (walk cleared) or the cap closed the walk: the step fallback works again. */
    @Test
    fun stepsResetAgainOnceAnOpenWalkIsGone() {
        val walking = sitting.feed(enter(WALKING, "10:20"))
        clock.now = at("10:21")
        assertEquals(ResetReason.STEPS, engine.onSteps(engine.clearMovement(walking), 150).lastResetReason)

        clock.now = at("11:10")
        assertEquals(ResetReason.STEPS, engine.onSteps(engine.closeOverlongWalk(walking), 150).lastResetReason)
    }

    /**
     * The real-phone log of 2026-10-30: each state arrives twice, 2 s apart, with its own stamp
     * (so the watermark can't drop it), and steps come in during the walk. One reset, at the
     * first STILL, and the repeats change nothing.
     */
    @Test
    fun duplicateEnterEventsCountOnce() {
        val walking = sitting.feed(enter(WALKING, "10:49:49"), enter(WALKING, "10:49:51"))
        assertEquals(at("10:49:49"), walking.movingSince)

        clock.now = at("10:51")
        val ended = engine.onSteps(walking, 180).feed(enter(STILL, "10:52:49"))
        assertEquals(at("10:52:49"), ended.sitStartedAt)
        assertEquals(ResetReason.WALKING, ended.lastResetReason)

        assertEquals(ended, ended.feed(enter(STILL, "10:52:51")))
    }

    /** An hour-old STILL that does get through (no watermark on this path) can't end a walk or move the timer. */
    @Test
    fun anHourOldStillChangesNothingWithOrWithoutAWalk() {
        assertEquals(sitting, sitting.feed(enter(STILL, "09:49:56")))

        val walking = sitting.feed(enter(WALKING, "10:49:49"))
        assertEquals(walking, walking.feed(enter(STILL, "09:49:56")))
    }

    @Test
    fun resetsClearSnoozes() {
        clock.now = at("10:45")
        val snoozed = (engine.snooze(sitting, settings) as SnoozeResult.Snoozed).state
        clock.now = at("10:50")
        assertEquals(0, engine.status(engine.onSteps(snoozed, 150), settings).snoozeCount)
    }

    @Test
    fun resetReasonsForManualActions() {
        assertEquals(ResetReason.DONE, engine.done(sitting).lastResetReason)
        assertEquals(ResetReason.SKIP, engine.skip(sitting).lastResetReason)
        assertEquals(ResetReason.MANUAL, engine.resetTimer(sitting).lastResetReason)
    }

    @Test
    fun restartMidWalkKeepsThePeriod() {
        val walking = sitting.feed(enter(WALKING, "10:20"), enter(RUNNING, "10:21"))
        val prefs = mutablePreferencesOf()
        prefs.writeSitState(walking)
        val restored = prefs.toSitState()
        assertEquals(walking, restored)
        // After "process death" the exit still closes the 5-minute period.
        val ended = SittingEngine(clock).onMovement(restored, MovementTransition(STILL, entered = true, at = at("10:25")))
        assertEquals(at("10:25"), ended.sitStartedAt)
    }

    @Test
    fun nextWakeIncludesWorkHoursBoundary() {
        clock.now = at("17:30")
        val status = engine.status(SitState(sitStartedAt = at("17:30")), settings) // due 18:15 > end
        assertEquals(at("18:00"), status.nextWakeAt) // wake at session end to stop tracking
    }
}
