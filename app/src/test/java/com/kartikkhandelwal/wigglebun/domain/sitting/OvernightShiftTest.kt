package com.kartikkhandelwal.wigglebun.domain.sitting

import com.kartikkhandelwal.wigglebun.domain.model.ActivePet
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.ui.home.PillState
import com.kartikkhandelwal.wigglebun.ui.home.toHomeUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** A shift that crosses midnight is one sit: the timer must not restart at 00:00. */
class OvernightShiftTest {

    private val zone = ZoneId.of("Asia/Kolkata")

    /** 2026-10-26 is a Monday. */
    private fun at(day: Int, time: String): Instant =
        LocalDateTime.parse("2026-10-${day}T$time").atZone(zone).toInstant()

    private val nights = SittingSettings(
        schedule = WorkSchedule(
            days = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
            startMinute = 22 * 60,
            endMinute = 6 * 60,
        ),
    )

    private val clock = MutableClock(at(27, "00:10"), zone)
    private val engine = SittingEngine(clock)
    private val sinceBeforeMidnight = SitState(sitStartedAt = at(26, "23:40"))

    @Test
    fun sittingSince2340Shows30MinutesAt0010() {
        val status = engine.status(sinceBeforeMidnight, nights)
        assertTrue(status.isWorkTime)
        assertEquals(30, status.sitMinutes)
    }

    /** What the Home timer pill shows, not only what the engine knows. */
    @Test
    fun homeShows30MinutesToo() {
        val home = toHomeUiState(
            status = engine.status(sinceBeforeMidnight, nights),
            settings = nights,
            pet = ActivePet.Default,
            sparks = 0,
            logsToday = emptyList(),
            focused = null,
            zone = zone,
        )
        assertEquals(30, (home.pill as PillState.Sitting).minutes)
    }

    @Test
    fun theMoodKeepsDrainingAcrossMidnight() {
        assertEquals(Mood.TIRED, engine.status(sinceBeforeMidnight, nights).mood) // 30 of 45 min
        clock.now = at(27, "00:30") // 50 min
        assertEquals(Mood.STIFF, engine.status(sinceBeforeMidnight, nights).mood)
    }

    /** Sat since 23:40 with a 45-minute interval: due at 00:25, on the other side of midnight. */
    @Test
    fun theReminderFallsDueAfterMidnight() {
        val waiting = engine.status(sinceBeforeMidnight, nights)
        assertEquals(at(27, "00:25"), waiting.reminderDue)
        assertFalse(waiting.shouldNotify)

        clock.now = at(27, "00:25")
        assertTrue(engine.status(sinceBeforeMidnight, nights).shouldNotify)
    }

    /** Friday's shift runs into Saturday, which isn't a work day: it still counts as Friday's. */
    @Test
    fun theLastNightOfTheWeekStillRunsPastMidnight() {
        clock.now = at(31, "00:10") // Saturday
        val status = engine.status(SitState(sitStartedAt = at(30, "23:40")), nights)
        assertTrue(status.isWorkTime)
        assertEquals(30, status.sitMinutes)
    }

    @Test
    fun aSitFromBeforeTheShiftCountsFromTheShiftStart() {
        val status = engine.status(SitState(sitStartedAt = at(26, "20:00")), nights)
        assertEquals(130, status.sitMinutes) // 22:00 to 00:10
    }

    @Test
    fun theShiftEndsAtSixAndThePetSleeps() {
        clock.now = at(27, "05:59")
        assertTrue(engine.status(sinceBeforeMidnight, nights).isWorkTime)
        clock.now = at(27, "06:00")
        val after = engine.status(sinceBeforeMidnight, nights)
        assertFalse(after.isWorkTime)
        assertEquals(Mood.SLEEPING, after.mood)
    }

    /** The next night starts fresh: yesterday's sit doesn't carry over. */
    @Test
    fun theNextShiftStartsFresh() {
        clock.now = at(27, "22:10")
        assertEquals(10, engine.status(sinceBeforeMidnight, nights).sitMinutes)
    }
}
