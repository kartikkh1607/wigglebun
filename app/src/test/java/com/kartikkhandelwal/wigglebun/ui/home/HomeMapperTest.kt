package com.kartikkhandelwal.wigglebun.ui.home

import com.kartikkhandelwal.wigglebun.data.db.BreakLogEntity
import com.kartikkhandelwal.wigglebun.domain.model.BreakOutcome
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.domain.sitting.SitStatus
import com.kartikkhandelwal.wigglebun.domain.sitting.SittingSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class HomeMapperTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val now: Instant = LocalDateTime.parse("2026-10-26T10:30").atZone(zone).toInstant()

    private fun status(
        mood: Mood = Mood.HAPPY,
        sitMinutes: Long = 10,
        isWorkTime: Boolean = true,
        isOnWalk: Boolean = false,
        reminderDue: Instant? = now.plusSeconds(35 * 60),
        snoozeCount: Int = 0,
        nextSessionStart: Instant? = null,
    ) = SitStatus(
        now = now,
        isWorkTime = isWorkTime,
        sitDuration = Duration.ofMinutes(sitMinutes),
        intervalMinutes = 45,
        energy = 0.5f,
        mood = mood,
        reminderDue = reminderDue,
        shouldNotify = false,
        nextAlarmAt = null,
        snoozeCount = snoozeCount,
        sessionEnd = null,
        nextSessionStart = nextSessionStart,
        isOnWalk = isOnWalk,
    )

    private fun map(
        status: SitStatus,
        logs: List<BreakLogEntity> = emptyList(),
        focused: BreakType? = null,
    ) = toHomeUiState(status, SittingSettings(), petName = null, sparks = 0, logsToday = logs, focused = focused, zone = zone)

    private fun log(type: BreakType, outcome: BreakOutcome) =
        BreakLogEntity(type = type, completedAt = now.toEpochMilli(), reward = 0, outcome = outcome)

    @Test
    fun progressIsCappedAtTheInterval() {
        val pill = map(status(mood = Mood.STIFF, sitMinutes = 90)).pill as PillState.Sitting
        assertEquals(90, pill.minutes)
        assertEquals(1f, pill.progress)
    }

    @Test
    fun exactlyAtTheIntervalIsAFullBarButStillTired() {
        // The engine calls fraction ≤ 1 TIRED; STIFF (and the CTA) only starts after the interval.
        val state = map(status(mood = Mood.TIRED, sitMinutes = 45, reminderDue = now))
        val pill = state.pill as PillState.Sitting
        assertEquals(1f, pill.progress)
        assertEquals(MoodChip.TIRED, pill.chip)
        assertFalse(state.showBreakCta)
        assertTrue(state.reminderDue)
    }

    @Test
    fun chipFollowsMoodAndCtaOnlyWhenStiff() {
        assertEquals(MoodChip.GREAT, (map(status(Mood.HAPPY)).pill as PillState.Sitting).chip)
        assertEquals(MoodChip.TIRED, (map(status(Mood.TIRED, 30)).pill as PillState.Sitting).chip)
        assertEquals(MoodChip.GREAT, (map(status(Mood.CELEBRATE, 0)).pill as PillState.Sitting).chip)

        val stiff = map(status(Mood.STIFF, 52))
        assertEquals(MoodChip.STIFF, (stiff.pill as PillState.Sitting).chip)
        assertTrue(stiff.showBreakCta)
        assertFalse(map(status(Mood.TIRED, 30)).showBreakCta)
    }

    @Test
    fun walkOverridesTheChipAndHidesTheCta() {
        val walking = map(status(Mood.STIFF, 52, isOnWalk = true))
        assertEquals(MoodChip.WALK, (walking.pill as PillState.Sitting).chip)
        assertFalse(walking.showBreakCta)
    }

    @Test
    fun outsideWorkHoursIsAsleepWithNextStartAndNoCta() {
        val next = LocalDateTime.parse("2026-10-27T09:00").atZone(zone).toInstant()
        val state = map(status(Mood.SLEEPING, 0, isWorkTime = false, reminderDue = null, nextSessionStart = next))
        assertEquals(PillState.Asleep(LocalDateTime.parse("2026-10-27T09:00")), state.pill)
        assertFalse(state.showBreakCta)
        assertFalse(state.reminderDue)
        assertEquals(LocalDate.parse("2026-10-26"), state.today)
    }

    @Test
    fun doneAndSkippedComeFromTodaysLog() {
        val state = map(
            status(),
            logs = listOf(
                log(BreakType.NECK, BreakOutcome.DONE),
                log(BreakType.WATER, BreakOutcome.SKIPPED),
                log(BreakType.WALK, BreakOutcome.SKIPPED),
                log(BreakType.WALK, BreakOutcome.DONE), // done later wins over skipped
            ),
        )
        val byType = state.breaks.associateBy { it.type }
        assertTrue(byType.getValue(BreakType.NECK).doneToday)
        assertTrue(byType.getValue(BreakType.WATER).skippedToday)
        assertFalse(byType.getValue(BreakType.WATER).doneToday)
        assertTrue(byType.getValue(BreakType.WALK).doneToday)
        assertFalse(byType.getValue(BreakType.WALK).skippedToday)
        assertEquals(2, state.doneCount)
        assertEquals(7, state.totalCount)
    }

    @Test
    fun snoozeOnlyOfferedWhenTheReminderIsDue() {
        val notDue = map(status(reminderDue = now.plusSeconds(60)), focused = BreakType.WALK)
        assertFalse(notDue.reminderDue)
        assertFalse(notDue.focus!!.canSnooze)

        val due = map(status(Mood.STIFF, 50, reminderDue = now.minusSeconds(60)), focused = BreakType.WALK)
        assertTrue(due.reminderDue)
        assertTrue(due.focus!!.canSnooze)

        val noSnoozesLeft = map(status(Mood.STIFF, 50, reminderDue = now.minusSeconds(60), snoozeCount = 3), focused = BreakType.WALK)
        assertTrue(noSnoozesLeft.reminderDue)
        assertFalse(noSnoozesLeft.focus!!.canSnooze)
    }

    @Test
    fun focusReflectsDoneToday() {
        val state = map(status(), logs = listOf(log(BreakType.EYES, BreakOutcome.DONE)), focused = BreakType.EYES)
        assertTrue(state.focus!!.doneToday)
        assertNull(map(status()).focus)
    }
}
