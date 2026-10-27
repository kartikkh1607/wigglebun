package com.kartikkhandelwal.wigglebun.ui.breaksession

import com.kartikkhandelwal.wigglebun.data.repo.BreakDoneResult
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.domain.rewards.Streak
import com.kartikkhandelwal.wigglebun.domain.session.SessionTimer
import com.kartikkhandelwal.wigglebun.ui.pet.EyeShape
import com.kartikkhandelwal.wigglebun.ui.pet.Poses
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BreakRoutinesTest {

    private val minute = 60_000L

    private fun timerFor(type: BreakType, now: Long = 0) =
        SessionTimer.start(type.durationMinutes * minute, type.sessionSteps().size, now)

    @Test
    fun everyTypeHasStepsThatShareItsDurationEvenly() {
        BreakType.entries.forEach { type ->
            val steps = type.sessionSteps()
            assertTrue("$type has steps", steps.isNotEmpty())
            val durations = SessionTimer.evenSplit(type.durationMinutes * minute, steps.size)
            assertEquals(type.durationMinutes * minute, durations.sum())
            assertEquals("$type steps are even", 1, durations.toSet().size)
            steps.forEach { assertTrue(it.text != 0) }
        }
    }

    @Test
    fun stepCountsMatchThePoseFrames() {
        assertEquals(4, BreakType.NECK.sessionSteps().size) // mockup: "Step 2 of 4"
        assertEquals(4, BreakType.STAND_UP.sessionSteps().size) // sit, push up, stand, reach
        assertEquals(3, BreakType.EYES.sessionSteps().size) // 20 s each
    }

    @Test
    fun eyesRestMovesOnlyTheEyes() {
        BreakType.EYES.sessionSteps().flatMap { it.poses }.forEach { pose ->
            assertEquals(Poses.EyesOpen.copy(eyes = pose.eyes), pose)
        }
        assertEquals(EyeShape.Closed, BreakType.EYES.sessionSteps().last().poses.single().eyes)
    }

    @Test
    fun drinkingWaterIsAHappyIdleWithoutPoses() {
        assertTrue(BreakType.WATER.sessionSteps().all { it.poses.isEmpty() })
        assertEquals(Mood.HAPPY, BreakType.WATER.sessionMood)
        assertEquals(Mood.STRETCH, BreakType.NECK.sessionMood)
        assertNull(sessionUiState(BreakType.WATER, null, timerFor(BreakType.WATER), 0, null).pose)
    }

    @Test
    fun safetyNoteOnlyOnTheFirstStep() {
        val timer = timerFor(BreakType.NECK)
        assertTrue(sessionUiState(BreakType.NECK, null, timer, 0, null).showSafetyNote)
        assertFalse(sessionUiState(BreakType.NECK, null, timer.next(1), 1, null).showSafetyNote)
    }

    @Test
    fun walkPosesLoopMarchingInPlace() {
        val timer = timerFor(BreakType.WALK)
        val poses = listOf(0L, 1_200L, 2_400L, 3_600L).map { sessionUiState(BreakType.WALK, null, timer, it, null).pose }
        assertEquals(listOf(Poses.MarchA, Poses.MarchB, Poses.MarchC, Poses.MarchB), poses)
    }

    @Test
    fun screenStaysOnOnlyWhileTheCountdownRuns() {
        val timer = timerFor(BreakType.NECK)
        assertTrue(sessionUiState(BreakType.NECK, null, timer, 0, null).keepScreenOn)
        assertFalse(sessionUiState(BreakType.NECK, null, timer.pause(1_000), 1_000, null).keepScreenOn)

        val finished = timer.advance(10 * minute)
        val atEnd = sessionUiState(BreakType.NECK, null, finished, 10 * minute, null)
        assertTrue(atEnd.isFinished)
        assertTrue(atEnd.isLastStep)
        assertFalse(atEnd.keepScreenOn)

        val result = BreakDoneResult(BreakType.NECK, 5, emptyList(), Streak(0, null, 4, 1, true))
        val celebrating = sessionUiState(BreakType.NECK, null, finished.pause(10 * minute), 10 * minute, result)
        assertEquals(Mood.CELEBRATE, celebrating.mood)
        assertFalse(celebrating.keepScreenOn)
    }
}
