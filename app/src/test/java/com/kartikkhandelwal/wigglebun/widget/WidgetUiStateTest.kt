package com.kartikkhandelwal.wigglebun.widget

import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.domain.sitting.SitStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class WidgetUiStateTest {

    private fun status(mood: Mood, sitMinutes: Long, isWorkTime: Boolean = true) = SitStatus(
        now = Instant.parse("2026-10-27T10:00:00Z"),
        isWorkTime = isWorkTime,
        sitDuration = Duration.ofMinutes(sitMinutes),
        intervalMinutes = 45,
        energy = 1f,
        mood = mood,
        reminderDue = null,
        shouldNotify = false,
        nextAlarmAt = null,
        snoozeCount = 0,
        sessionEnd = null,
        nextSessionStart = null,
    )

    @Test
    fun workTimeShowsMinutesAndMood() {
        val state = WidgetUiState.from(status(Mood.TIRED, sitMinutes = 32), "Bun")
        assertEquals(32, state.minutes)
        assertEquals(Mood.TIRED, state.mood)
        assertEquals(R.drawable.bun_mood_tired, state.petImage)
    }

    @Test
    fun overIntervalIsStiff() {
        val state = WidgetUiState.from(status(Mood.STIFF, sitMinutes = 60), "Bun")
        assertEquals(R.drawable.bun_mood_stiff, state.petImage)
    }

    @Test
    fun outsideWorkHoursSleepsWithoutTimer() {
        val state = WidgetUiState.from(status(Mood.SLEEPING, sitMinutes = 0, isWorkTime = false), "Bun")
        assertNull(state.minutes)
        assertTrue(state.isSleeping)
        assertEquals(R.drawable.bun_mood_sleep, state.petImage)
    }

    /** The 3-second celebration would otherwise stay on the widget until the next redraw. */
    @Test
    fun celebrateIsShownAsHappy() {
        val state = WidgetUiState.from(status(Mood.CELEBRATE, sitMinutes = 0), "Bun")
        assertEquals(Mood.HAPPY, state.mood)
        assertEquals(0, state.minutes)
    }

    @Test
    fun celebrateAfterWorkHoursIsAsleep() {
        val state = WidgetUiState.from(status(Mood.CELEBRATE, sitMinutes = 0, isWorkTime = false), "Bun")
        assertEquals(Mood.SLEEPING, state.mood)
    }
}
