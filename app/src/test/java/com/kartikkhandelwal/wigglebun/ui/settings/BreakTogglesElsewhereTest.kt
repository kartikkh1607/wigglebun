package com.kartikkhandelwal.wigglebun.ui.settings

import com.kartikkhandelwal.wigglebun.domain.model.ActivePet
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.domain.model.PetId
import com.kartikkhandelwal.wigglebun.domain.sitting.SitStatus
import com.kartikkhandelwal.wigglebun.domain.sitting.SittingSettings
import com.kartikkhandelwal.wigglebun.ui.home.toHomeUiState
import com.kartikkhandelwal.wigglebun.widget.WidgetUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** A break switched off in Settings isn't offered anywhere else either. */
class BreakTogglesElsewhereTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val now = Instant.parse("2026-10-26T06:30:00Z")

    private val stiff = SitStatus(
        now = now,
        isWorkTime = true,
        sitDuration = Duration.ofMinutes(60),
        intervalMinutes = 45,
        energy = 0.15f,
        mood = Mood.STIFF,
        reminderDue = now,
        shouldNotify = false,
        nextAlarmAt = null,
        snoozeCount = 0,
        sessionEnd = now.plusSeconds(3_600),
        nextSessionStart = null,
    )

    private fun home(enabled: List<BreakType>) =
        toHomeUiState(stiff, SittingSettings(), ActivePet.Default, 0, emptyList(), null, zone, enabled)

    @Test
    fun homeListsOnlyWhatsOn() {
        val state = home(listOf(BreakType.WATER, BreakType.EYES))
        assertEquals(listOf(BreakType.WATER, BreakType.EYES), state.breaks.map { it.type })
        assertEquals(2, state.totalCount)
    }

    /** "Take a 2-min break" starts the walk, so it goes when the walk is off. */
    @Test
    fun homesWalkButtonFollowsTheWalkToggle() {
        assertTrue(home(BreakType.entries).showBreakCta)
        assertFalse(home(BreakType.entries - BreakType.WALK).showBreakCta)
    }

    @Test
    fun remindersOnlySuggestWhatsOn() {
        val enabled = listOf(BreakType.WATER, BreakType.EYES)
        (0L..20L).forEach { minute -> assertTrue(BreakType.suggestedFor(minute, enabled) in enabled) }
        assertEquals(setOf(BreakType.WATER, BreakType.EYES), (0L..20L).map { BreakType.suggestedFor(it, enabled) }.toSet())
    }

    @Test
    fun theWidgetOnlySuggestsWhatsOn() {
        val state = WidgetUiState.from(stiff, PetId.BUN, "Bun", listOf(BreakType.WATER))
        assertEquals(BreakType.WATER, state.suggestedBreak)
    }
}
