package com.kartikkhandelwal.wigglebun.data.prefs

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.kartikkhandelwal.wigglebun.domain.sitting.SitState
import com.kartikkhandelwal.wigglebun.domain.sitting.SittingEngine
import com.kartikkhandelwal.wigglebun.domain.sitting.SittingSettings
import com.kartikkhandelwal.wigglebun.domain.sitting.WorkSchedule
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

class PrefsMappingTest {

    @Test
    fun emptyPrefsGiveDefaults() {
        assertEquals(SittingSettings(), mutablePreferencesOf().toSittingSettings())
        assertEquals(SitState(), mutablePreferencesOf().toSitState())
    }

    @Test
    fun scheduleRoundTrips() {
        val prefs = mutablePreferencesOf()
        val schedule = WorkSchedule(setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), 22 * 60, 6 * 60)
        prefs.writeSchedule(schedule)
        assertEquals(schedule, prefs.toSittingSettings().schedule)
    }

    @Test
    fun badStoredValuesFallBack() {
        val prefs = mutablePreferencesOf(
            PrefsKeys.IntervalMinutes to 500,
            PrefsKeys.WorkStartMinute to 5000,
            PrefsKeys.WorkDays to setOf("FUNDAY", "MONDAY"),
        )
        val settings = prefs.toSittingSettings()
        assertEquals(SittingSettings.MAX_INTERVAL_MINUTES, settings.intervalMinutes)
        assertEquals(WorkSchedule.Default.startMinute, settings.schedule.startMinute)
        assertEquals(setOf(DayOfWeek.MONDAY), settings.schedule.days)
    }

    @Test
    fun sitStateRoundTripsAndClearsNulls() {
        val full = SitState(
            sitStartedAt = Instant.parse("2026-10-26T04:30:00Z"),
            snoozeCount = 2,
            snoozedUntil = Instant.parse("2026-10-26T05:25:00Z"),
            lastNotifiedDue = Instant.parse("2026-10-26T05:15:00Z"),
            celebrateUntil = Instant.parse("2026-10-26T04:30:03Z"),
            lastBreakAt = Instant.parse("2026-10-26T04:30:00Z"),
        )
        val prefs = mutablePreferencesOf()
        prefs.writeSitState(full)
        assertEquals(full, prefs.toSitState())
        prefs.writeSitState(SitState(sitStartedAt = full.sitStartedAt))
        assertEquals(SitState(sitStartedAt = full.sitStartedAt), prefs.toSitState())
    }

    /** "App restart in the middle of a session": persist, rebuild everything, same answer. */
    @Test
    fun restartMidSessionThroughPrefs() {
        val zone = ZoneId.of("Asia/Kolkata")
        val now = Instant.parse("2026-10-26T05:10:00Z") // Monday 10:40 IST
        val clock = Clock.fixed(now, zone)
        val settings = SittingSettings()
        val state = SitState(sitStartedAt = Instant.parse("2026-10-26T04:30:00Z"), snoozeCount = 1, snoozedUntil = now.plusSeconds(300))

        val prefs = mutablePreferencesOf()
        prefs.writeSitState(state)
        prefs.writeSchedule(settings.schedule)

        val before = SittingEngine(clock).status(state, settings)
        val after = SittingEngine(Clock.fixed(now, zone)).status(prefs.toSitState(), prefs.toSittingSettings())
        assertEquals(before, after)
        assertEquals(40, after.sitMinutes)
        assertEquals(1, after.snoozeCount)
    }
}
