package com.kartikkhandelwal.wigglebun.widget

import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.domain.model.PetId
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
        val state = WidgetUiState.from(status(Mood.TIRED, sitMinutes = 32), PetId.BUN, "Bun")
        assertEquals(32, state.minutes)
        assertEquals(Mood.TIRED, state.mood)
        assertEquals(R.drawable.bun_mood_tired, state.petImage)
    }

    @Test
    fun overIntervalIsStiff() {
        val state = WidgetUiState.from(status(Mood.STIFF, sitMinutes = 60), PetId.BUN, "Bun")
        assertEquals(R.drawable.bun_mood_stiff, state.petImage)
    }

    @Test
    fun outsideWorkHoursSleepsWithoutTimer() {
        val state = WidgetUiState.from(status(Mood.SLEEPING, sitMinutes = 0, isWorkTime = false), PetId.BUN, "Bun")
        assertNull(state.minutes)
        assertTrue(state.isSleeping)
        assertEquals(R.drawable.bun_mood_sleep, state.petImage)
    }

    /** The 3-second celebration would otherwise stay on the widget until the next redraw. */
    @Test
    fun celebrateIsShownAsHappy() {
        val state = WidgetUiState.from(status(Mood.CELEBRATE, sitMinutes = 0), PetId.BUN, "Bun")
        assertEquals(Mood.HAPPY, state.mood)
        assertEquals(0, state.minutes)
    }

    @Test
    fun celebrateAfterWorkHoursIsAsleep() {
        val state = WidgetUiState.from(status(Mood.CELEBRATE, sitMinutes = 0, isWorkTime = false), PetId.BUN, "Bun")
        assertEquals(Mood.SLEEPING, state.mood)
    }

    /** M8: the widget draws the active pet's own images. */
    @Test
    fun activePetPicksItsOwnImages() {
        assertEquals(R.drawable.pip_mood_tired, WidgetUiState.from(status(Mood.TIRED, 32), PetId.PIP, "Pip").petImage)
        assertEquals(R.drawable.toast_mood_sleep, WidgetUiState.from(status(Mood.SLEEPING, 0, isWorkTime = false), PetId.TOAST, "Toast").petImage)
        assertEquals(R.drawable.nimbus_mood_stiff, WidgetUiState.from(status(Mood.STIFF, 60), PetId.NIMBUS, "Nimbus").petImage)
    }

    /** Every pet × mood has its own image, so no pet ever shows another pet's art. */
    @Test
    fun everyPetAndMoodHasADistinctImage() {
        val images = PetId.entries.flatMap { pet -> Mood.entries.map { mood -> WidgetUiState.moodImage(pet, mood) } }
        assertEquals(PetId.entries.size * Mood.entries.size, images.toSet().size)
    }
}
