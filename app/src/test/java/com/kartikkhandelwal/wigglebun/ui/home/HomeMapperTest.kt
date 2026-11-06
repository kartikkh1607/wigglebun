package com.kartikkhandelwal.wigglebun.ui.home

import com.kartikkhandelwal.wigglebun.data.db.BreakLogEntity
import com.kartikkhandelwal.wigglebun.data.repo.StreakSummary
import com.kartikkhandelwal.wigglebun.domain.model.ActivePet
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
    ) = toHomeUiState(status, SittingSettings(), pet = ActivePet.Default, sparks = 0, logsToday = logs, focused = focused, zone = zone)

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
        assertEquals(PetLine.TIRED, state.line)
        assertFalse(state.showBreakCta)
        assertTrue(state.reminderDue)
    }

    @Test
    fun lineFollowsMoodAndCtaOnlyWhenStiff() {
        assertEquals(PetLine.HAPPY, map(status(Mood.HAPPY)).line)
        assertEquals(PetLine.TIRED, map(status(Mood.TIRED, 30)).line)
        assertEquals(PetLine.CELEBRATE, map(status(Mood.CELEBRATE, 0)).line)

        val stiff = map(status(Mood.STIFF, 52))
        assertEquals(PetLine.STIFF, stiff.line)
        assertTrue(stiff.showBreakCta)
        assertFalse(map(status(Mood.TIRED, 30)).showBreakCta)
    }

    @Test
    fun walkOverridesTheLineAndHidesTheCta() {
        val walking = map(status(Mood.STIFF, 52, isOnWalk = true))
        assertEquals(PetLine.WALK, walking.line)
        assertFalse(walking.showBreakCta)
    }

    /** The z's say it: no bubble while asleep. */
    @Test
    fun noLineOutsideWorkHours() {
        assertNull(map(status(Mood.SLEEPING, 0, isWorkTime = false, reminderDue = null)).line)
    }

    /** "Done · +5 sparks" shows what the ledger paid today; a done type it didn't pay shows just "Done". */
    @Test
    fun doneCardsShowTheSparksActuallyPaid() {
        val state = toHomeUiState(
            status(), SittingSettings(), ActivePet.Default, sparks = 0,
            logsToday = listOf(log(BreakType.NECK, BreakOutcome.DONE), log(BreakType.WALK, BreakOutcome.DONE)),
            focused = null, zone = zone,
            paidToday = setOf(BreakType.NECK, BreakType.STAND_UP),
        )
        val byType = state.breaks.associateBy { it.type }
        assertEquals(BreakType.NECK.defaultReward, byType.getValue(BreakType.NECK).sparksEarned)
        assertEquals(0, byType.getValue(BreakType.WALK).sparksEarned)
        // Paid but not in today's log (a different break day): not done, so nothing to show.
        assertEquals(0, byType.getValue(BreakType.STAND_UP).sparksEarned)
    }

    @Test
    fun streakGoalOnlyOnWorkDaysAndCapped() {
        fun goal(summary: StreakSummary) = toHomeUiState(
            status(), SittingSettings(), ActivePet.Default, sparks = 0, logsToday = emptyList(), focused = null, zone = zone,
            streak = summary,
        ).streakGoal
        assertEquals(StreakGoalUi(done = 2, goal = 3, days = 5), goal(StreakSummary(5, 2, 3, todayIsWorkDay = true, totalBreaks = 40)))
        // Repeats count toward the goal, but the row never shows more than it.
        assertEquals(StreakGoalUi(done = 3, goal = 3, days = 6), goal(StreakSummary(6, 5, 3, todayIsWorkDay = true, totalBreaks = 40)))
        assertNull(goal(StreakSummary(5, 0, 3, todayIsWorkDay = false, totalBreaks = 40)))
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
