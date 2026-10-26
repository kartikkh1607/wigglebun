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

    @Test
    fun staleWalkWithLostExitIsIgnored() {
        val walking = sitting.feed(enter(WALKING, "10:20"))
        clock.now = at("13:30")
        val status = engine.status(walking, settings)
        assertFalse(status.isOnWalk)
        assertTrue(status.sitMinutes > 180)
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
