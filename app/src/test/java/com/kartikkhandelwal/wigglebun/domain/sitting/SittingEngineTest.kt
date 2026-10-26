package com.kartikkhandelwal.wigglebun.domain.sitting

import com.kartikkhandelwal.wigglebun.domain.model.Mood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** A clock the test moves by hand. */
private class MutableClock(var now: Instant, private val zone: ZoneId) : Clock() {
    override fun getZone(): ZoneId = zone
    override fun withZone(zone: ZoneId): Clock = MutableClock(now, zone)
    override fun instant(): Instant = now
    fun advance(minutes: Long) { now = now.plusSeconds(minutes * 60) }
}

class SittingEngineTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private fun at(local: String, z: ZoneId = zone): Instant = LocalDateTime.parse(local).atZone(z).toInstant()

    // 2026-10-26 is a Monday, 2026-10-30 a Friday.
    private val clock = MutableClock(at("2026-10-26T10:00"), zone)
    private val engine = SittingEngine(clock)
    private val settings = SittingSettings()

    private fun started(local: String) = SitState(sitStartedAt = at(local))

    @Test
    fun timerRunsDuringWorkHours() {
        clock.now = at("2026-10-26T10:30")
        val s = engine.status(started("2026-10-26T10:00"), settings)
        assertTrue(s.isWorkTime)
        assertEquals(30, s.sitMinutes)
        assertEquals(at("2026-10-26T10:45"), s.reminderDue)
        assertFalse(s.shouldNotify)
        assertEquals(at("2026-10-26T10:45"), s.nextAlarmAt)
    }

    @Test
    fun outsideWorkHoursSleepsWithFullEnergy() {
        clock.now = at("2026-10-26T20:00")
        val s = engine.status(started("2026-10-26T10:00"), settings)
        assertFalse(s.isWorkTime)
        assertEquals(Mood.SLEEPING, s.mood)
        assertEquals(1f, s.energy)
        assertEquals(Duration.ZERO, s.sitDuration)
        assertNull(s.reminderDue)
        assertEquals(at("2026-10-27T09:45"), s.nextAlarmAt)
    }

    @Test
    fun timeBeforeTheSessionDoesNotCount() {
        // Reset yesterday evening; this morning the timer starts at 09:00, not yesterday.
        clock.now = at("2026-10-27T09:10")
        val s = engine.status(started("2026-10-26T17:30"), settings)
        assertEquals(10, s.sitMinutes)
        assertEquals(Mood.HAPPY, s.mood)
    }

    @Test
    fun weekendSleepsAndFridayRollsToMonday() {
        clock.now = at("2026-10-31T11:00") // Saturday
        val sat = engine.status(started("2026-10-30T17:00"), settings)
        assertFalse(sat.isWorkTime)
        assertEquals(at("2026-11-02T09:00"), sat.nextSessionStart)
        assertEquals(at("2026-11-02T09:45"), sat.nextAlarmAt)

        clock.now = at("2026-11-02T09:20") // Monday
        assertEquals(20, engine.status(started("2026-10-30T17:00"), settings).sitMinutes)
    }

    @Test
    fun overnightScheduleCrossesMidnight() {
        val night = SittingSettings(WorkSchedule(DayOfWeek.entries.toSet(), 22 * 60, 6 * 60))
        clock.now = at("2026-10-27T00:30")
        val s = engine.status(started("2026-10-26T23:30"), night)
        assertTrue(s.isWorkTime)
        assertEquals(60, s.sitMinutes)
        assertEquals(at("2026-10-27T06:00"), s.sessionEnd)
    }

    @Test
    fun noReminderPastSessionEnd() {
        clock.now = at("2026-10-26T17:40")
        val s = engine.status(started("2026-10-26T17:30"), settings) // due 18:15 > 18:00
        assertNull(s.reminderDue)
        assertEquals(at("2026-10-27T09:45"), s.nextAlarmAt)
    }

    @Test
    fun intervalReachedNotifiesOnce() {
        var state = started("2026-10-26T10:00")
        clock.now = at("2026-10-26T10:45")
        val s = engine.status(state, settings)
        assertTrue(s.shouldNotify)
        state = engine.markNotified(state, s.reminderDue!!)
        clock.advance(5)
        assertFalse(engine.status(state, settings).shouldNotify)
    }

    @Test
    fun snoozeThreeTimesThenLimit() {
        var state = started("2026-10-26T10:00")
        clock.now = at("2026-10-26T10:45")
        repeat(3) { i ->
            val r = engine.snooze(state, settings) as SnoozeResult.Snoozed
            assertEquals(clock.now.plusSeconds(600), r.until)
            state = r.state
            assertEquals(i + 1, engine.status(state, settings).snoozeCount)
            assertEquals(r.until, engine.status(state, settings).reminderDue)
            clock.advance(10)
            assertTrue(engine.status(state, settings).shouldNotify)
            state = engine.markNotified(state, r.until)
        }
        assertEquals(SnoozeResult.LimitReached, engine.snooze(state, settings))
        assertEquals(0, engine.status(state, settings).snoozesLeft)
    }

    @Test
    fun doneAndSkipResetSnoozes() {
        clock.now = at("2026-10-26T10:45")
        val snoozed = (engine.snooze(started("2026-10-26T10:00"), settings) as SnoozeResult.Snoozed).state
        assertEquals(0, engine.status(engine.done(snoozed), settings).snoozeCount)
        assertEquals(0, engine.status(engine.skip(snoozed), settings).snoozeCount)
    }

    @Test
    fun snoozeFromAnEarlierSessionIsIgnored() {
        clock.now = at("2026-10-26T17:55")
        val snoozed = (engine.snooze(started("2026-10-26T17:00"), settings) as SnoozeResult.Snoozed).state
        clock.now = at("2026-10-27T09:05")
        val s = engine.status(snoozed, settings)
        assertEquals(0, s.snoozeCount)
        assertEquals(at("2026-10-27T09:45"), s.reminderDue)
    }

    @Test
    fun doneCelebratesButSkipDoesNot() {
        clock.now = at("2026-10-26T11:00")
        val state = started("2026-10-26T10:00")
        val done = engine.done(state)
        assertEquals(Mood.CELEBRATE, engine.status(done, settings).mood)
        assertEquals(clock.now, done.lastBreakAt)
        val skipped = engine.skip(state)
        assertEquals(Mood.HAPPY, engine.status(skipped, settings).mood)
        assertNull(skipped.lastBreakAt)
        clock.advance(1)
        assertEquals(Mood.HAPPY, engine.status(done, settings).mood) // celebrate lasts 3 s
    }

    @Test
    fun moodThresholds() {
        val state = started("2026-10-26T10:00") // interval 45
        fun moodAfter(seconds: Long): Mood {
            clock.now = at("2026-10-26T10:00").plusSeconds(seconds)
            return engine.status(state, settings).mood
        }
        assertEquals(Mood.HAPPY, moodAfter((45 * 60 * 0.49).toLong()))
        assertEquals(Mood.TIRED, moodAfter((45 * 60 * 0.5).toLong()))
        assertEquals(Mood.TIRED, moodAfter(45 * 60))
        assertEquals(Mood.STIFF, moodAfter((45 * 60 * 1.01).toLong()))
    }

    @Test
    fun energyFormulaAndClamp() {
        assertEquals(1f, SittingEngine.energyFor(0f, 45), 1e-4f)
        assertEquals(1f - 45f / 54f, SittingEngine.energyFor(45f, 45), 1e-4f)
        assertEquals(0.15f, SittingEngine.energyFor(500f, 45), 1e-4f)
        assertEquals(1f, SittingEngine.energyFor(-5f, 45), 1e-4f)
    }

    @Test
    fun intervalIsCoerced() {
        assertEquals(20, SittingSettings(intervalMinutes = 5).interval)
        assertEquals(90, SittingSettings(intervalMinutes = 500).interval)
    }

    @Test
    fun futureStartIsClampedToNow() {
        clock.now = at("2026-10-26T10:00")
        val s = engine.status(started("2026-10-26T15:00"), settings)
        assertEquals(Duration.ZERO, s.sitDuration)
        assertEquals(1f, s.energy)
    }

    @Test
    fun dstSpringForwardCountsRealMinutes() {
        val london = ZoneId.of("Europe/London") // 2026-03-29 01:00 GMT → 02:00 BST
        val c = MutableClock(at("2026-03-29T02:20", london), london)
        val all = SittingSettings(WorkSchedule.AlwaysOn)
        val s = SittingEngine(c).status(SitState(sitStartedAt = at("2026-03-29T00:30", london)), all)
        assertEquals(50, s.sitMinutes)
    }

    @Test
    fun restartMidSessionGivesSameStatus() {
        clock.now = at("2026-10-26T10:20")
        val state = engine.resetTimer(SitState())
        clock.advance(25)
        val before = engine.status(state, settings)
        // "Process death": a new engine over a copy of the persisted state.
        val after = SittingEngine(clock).status(state.copy(), settings)
        assertEquals(before, after)
        assertEquals(25, after.sitMinutes)
    }
}
