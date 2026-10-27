package com.kartikkhandelwal.wigglebun.domain.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTimerTest {

    private val second = 1_000L

    /** Neck stretch: 2 min in 4 steps. */
    private fun neck(now: Long = 0) = SessionTimer.start(totalMillis = 120 * second, stepCount = 4, now = now)

    @Test
    fun durationIsSplitEvenly() {
        assertEquals(List(4) { 30 * second }, SessionTimer.evenSplit(120 * second, 4))
        assertEquals(List(3) { 20 * second }, SessionTimer.evenSplit(60 * second, 3))
        // A remainder goes to the last step, so the steps always add up to the whole break.
        assertEquals(listOf(333L, 333L, 334L), SessionTimer.evenSplit(1_000, 3))
    }

    @Test
    fun countsDownFromElapsedTime() {
        val timer = neck(now = 1_000)
        assertEquals(30 * second, timer.remainingMillis(1_000))
        assertEquals(11 * second, timer.remainingMillis(1_000 + 19 * second))
        assertEquals(0.5f, timer.progress(1_000 + 15 * second))
    }

    @Test
    fun pausedTimeDoesNotCount() {
        val paused = neck().pause(10 * second)
        assertFalse(paused.isRunning)
        assertEquals(20 * second, paused.remainingMillis(10 * 60 * second)) // ten minutes later
        val resumed = paused.resume(10 * 60 * second)
        assertEquals(15 * second, resumed.remainingMillis(10 * 60 * second + 5 * second))
    }

    /** Most sessions pause on ON_STOP; the timer just stays where it was. */
    @Test
    fun backgroundPauseKeepsTheRemainingTime() {
        val inBackground = neck().pause(12 * second)
        val back = inBackground.advance(30 * 60 * second)
        assertEquals(0, back.stepIndex)
        assertEquals(18 * second, back.remainingMillis(30 * 60 * second))
    }

    /** The 2-min walk is not paused on ON_STOP: on return it shows the real time left. */
    @Test
    fun walkKeepsRunningInTheBackground() {
        val walk = SessionTimer.start(totalMillis = 120 * second, stepCount = 2, now = 0)

        val halfway = walk.advance(90 * second) // screen off, phone in pocket
        assertEquals(1, halfway.stepIndex)
        assertEquals(30 * second, halfway.remainingMillis(90 * second))
        assertTrue(halfway.isRunning)

        val longAfter = walk.advance(10 * 60 * second)
        assertTrue(longAfter.isLastStep)
        assertEquals(0L, longAfter.remainingMillis(10 * 60 * second))
        assertTrue(longAfter.isFinished(10 * 60 * second)) // last step at 0:00, waiting for Done
    }

    @Test
    fun stepsAutoAdvanceCarryingTheOverflow() {
        val timer = neck().advance(35 * second)
        assertEquals(1, timer.stepIndex)
        assertEquals(25 * second, timer.remainingMillis(35 * second))
    }

    @Test
    fun lastStepStopsAtZero() {
        val end = neck().advance(500 * second)
        assertEquals(3, end.stepIndex)
        assertEquals(0L, end.remainingMillis(500 * second))
        assertTrue(end.isFinished(500 * second))
        assertFalse(neck().isFinished(0))
    }

    @Test
    fun nextSkipsAheadAndIsANoOpOnTheLastStep() {
        val second2 = neck().next(5 * second)
        assertEquals(1, second2.stepIndex)
        assertEquals(30 * second, second2.remainingMillis(5 * second))
        assertTrue(second2.isRunning)

        val last = second2.next(6 * second).next(7 * second)
        assertTrue(last.isLastStep)
        assertEquals(last, last.next(8 * second))
    }

    @Test
    fun nextWhilePausedStaysPaused() {
        val next = neck().pause(3 * second).next(4 * second)
        assertEquals(1, next.stepIndex)
        assertFalse(next.isRunning)
        assertEquals(30 * second, next.remainingMillis(100 * second))
    }

    @Test
    fun posesLoopEveryHold() {
        val timer = neck()
        val hold = 1_200L
        assertEquals(listOf(0, 1, 2, 3, 0), listOf(0L, 1_200L, 2_400L, 3_600L, 4_800L).map { timer.poseIndex(it, 4, hold) })
        assertEquals(0, timer.poseIndex(5_000, 1, hold))
    }

    @Test
    fun aClockThatGoesBackNeverCountsBackwards() {
        val timer = neck(now = 10 * second)
        assertEquals(30 * second, timer.remainingMillis(5 * second))
    }
}
