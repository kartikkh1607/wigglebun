package com.kartikkhandelwal.wigglebun.ui.home

import android.app.Application
import android.os.Looper
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kartikkhandelwal.wigglebun.data.prefs.writeSchedule
import com.kartikkhandelwal.wigglebun.data.repo.BreakActions
import com.kartikkhandelwal.wigglebun.data.repo.TestDataLayer
import com.kartikkhandelwal.wigglebun.data.time.FixedAppClock
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.SparkReason
import com.kartikkhandelwal.wigglebun.domain.sitting.DayBoundary
import com.kartikkhandelwal.wigglebun.domain.sitting.WorkSchedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * On a 22:00–06:00 shift the break day starts at 22:00, not at midnight: "Breaks for today", the
 * once-a-day sparks and the streak all treat the shift as one day (CLAUDE.md §5.4).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class)
class NightShiftDayTest {

    /** October 2026 in Kolkata: Sun 25, Mon 26, … Fri 30, Sat 31. */
    private fun at(text: String): Instant = LocalDateTime.parse(text).atZone(FixedAppClock.ZONE).toInstant()

    private val clock = FixedAppClock(at("2026-10-25T12:00"))
    private val app = TestDataLayer(ApplicationProvider.getApplicationContext(), clock)
    private val collector = CoroutineScope(Dispatchers.Main)

    private val weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
    private val nights = WorkSchedule(weekdays, startMinute = 22 * 60, endMinute = 6 * 60)
    private val fourTypes = listOf(BreakType.STAND_UP, BreakType.WALK, BreakType.NECK, BreakType.EYES)

    @After
    fun tearDown() {
        collector.cancel()
        app.close()
    }

    /** Night shifts chosen on Sunday noon, as onboarding or Settings would. */
    private fun nightsFromSunday() = runBlocking { app.settings.setSchedule(nights) }

    private fun doneAt(time: String, type: BreakType) = runBlocking {
        clock.now = at(time)
        app.recordBreakDone(type)
    }

    @Test
    fun aTypeDoneAt2340DoesNotPayAgainAt0010() {
        nightsFromSunday()
        assertEquals(BreakType.NECK.defaultReward, doneAt("2026-10-30T23:40", BreakType.NECK).sparksEarned)
        assertEquals(0, doneAt("2026-10-31T00:10", BreakType.NECK).sparksEarned)
        assertEquals(1, app.ledger().count { it.reason == SparkReason.BREAK })
        // The next shift is a new day.
        assertEquals(BreakType.NECK.defaultReward, doneAt("2026-11-02T22:30", BreakType.NECK).sparksEarned)
    }

    /** Home's "Breaks for today" still shows the 23:40 break at 00:10, and starts over at 22:00. */
    @Test
    fun homeKeepsTheBreakUntilTheNextShiftStarts() {
        nightsFromSunday()
        doneAt("2026-10-30T23:40", BreakType.NECK)
        clock.now = at("2026-10-31T00:10")

        val home = HomeViewModel(
            sitRepository = app.sitRepository,
            settingsRepository = app.settings,
            breakRepository = app.breaks,
            walletRepository = app.wallet,
            petRepository = app.pets,
            actions = BreakActions(app.sitRepository, app.breaks, app.clock),
            clock = app.clock,
        )
        try {
            collector.launch { home.state.collect {} } // WhileSubscribed: keep it hot
            waitUntil { home.state.value?.neckDone() == true }
            assertFalse(home.state.value!!.lastShift) // on shift: "Breaks for today"

            clock.now = at("2026-10-31T21:59")
            Thread.sleep(TICK_MILLIS) // the status ticks every second
            waitUntil { home.state.value?.let { it.neckDone() && it.lastShift } == true } // "Last shift"

            clock.now = at("2026-10-31T22:00")
            waitUntil { home.state.value?.neckDone() == false }
        } finally {
            home.viewModelScope.cancel()
        }
    }

    /** Four types between 23:40 and 03:00: Friday's shift counts for the streak, as one day. */
    @Test
    fun theWholeShiftCountsForTheStreak() {
        nightsFromSunday()
        doneAt("2026-10-30T23:40", BreakType.NECK)
        doneAt("2026-10-31T00:10", BreakType.WALK)
        doneAt("2026-10-31T01:00", BreakType.EYES)
        val last = doneAt("2026-10-31T03:00", BreakType.WATER)
        assertEquals(4, last.streak.todayDone)
        assertTrue(last.streak.todayCounts)
        assertEquals(1, last.streak.days)
        assertEquals(LocalDate.of(2026, 10, 30), last.streak.startDate)
    }

    /** Daytime hours keep midnight: the same type on the other side of it pays again. */
    @Test
    fun daytimeHoursStillTurnAtMidnight() {
        runBlocking { app.settings.setSchedule(WorkSchedule.Default) }
        assertEquals(BreakType.NECK.defaultReward, doneAt("2026-10-26T23:40", BreakType.NECK).sparksEarned)
        assertEquals(BreakType.NECK.defaultReward, doneAt("2026-10-27T00:10", BreakType.NECK).sparksEarned)
    }

    /**
     * Daytime breaks on Thursday and Friday, then nights from Monday afternoon. Moving the old
     * breaks to a 22:00 boundary would make them Wednesday's and Thursday's, and leave Friday
     * empty. They keep their days, so Monday night makes it 3 days.
     */
    @Test
    fun switchingToNightsKeepsEarlierBreaksOnTheirDays() {
        listOf("2026-10-22", "2026-10-23").forEach { date ->
            fourTypes.forEachIndexed { i, type -> app.logDone(type, at("${date}T1$i:00")) }
        }
        clock.now = at("2026-10-26T15:00")
        runBlocking { app.settings.setSchedule(nights) }

        clock.now = at("2026-10-26T23:00")
        fourTypes.dropLast(1).forEach { app.logDone(it, clock.now) }
        val last = runBlocking { app.recordBreakDone(fourTypes.last()) }
        assertEquals(3, last.streak.days)
        assertEquals(LocalDate.of(2026, 10, 22), last.streak.startDate)
    }

    /**
     * Back to daytime hours on Tuesday afternoon, during Monday's shift day: Monday runs on to
     * Wednesday 00:00 and Tuesday never starts. It isn't a missed work day, so the streak holds.
     */
    @Test
    fun aDateSkippedByAChangeOfHoursDoesNotBreakTheStreak() {
        nightsFromSunday()
        fourTypes.forEach { doneAt("2026-10-26T23:00", it) }
        clock.now = at("2026-10-27T15:00")
        runBlocking { app.settings.setSchedule(WorkSchedule.Default) }

        fourTypes.dropLast(1).forEach { app.logDone(it, at("2026-10-28T10:00")) }
        val wednesday = doneAt("2026-10-28T10:00", fourTypes.last())
        assertTrue(wednesday.streak.todayCounts)
        assertEquals(2, wednesday.streak.days) // Monday and Wednesday
        assertEquals(LocalDate.of(2026, 10, 26), wednesday.streak.startDate)
    }

    /** Night hours saved before this version: the first start adopts the shift's days, from today. */
    @Test
    fun nightHoursFromBeforeAreAdoptedOnStart() {
        runBlocking {
            app.dataStore.edit { it.writeSchedule(nights) }
            clock.now = at("2026-10-26T12:00")
            app.sitRepository.onAppStart()
            assertEquals(DayBoundary(22 * 60), app.settings.dayBoundaryHistory.first().current)
        }
        assertEquals(BreakType.NECK.defaultReward, doneAt("2026-10-26T23:40", BreakType.NECK).sparksEarned)
        assertEquals(0, doneAt("2026-10-27T00:10", BreakType.NECK).sparksEarned)
    }

    private fun HomeUiState.neckDone(): Boolean = breaks.single { it.type == BreakType.NECK }.doneToday

    /** Runs main-thread work (viewModelScope) until [condition] holds; IO work resumes there. */
    private fun waitUntil(condition: () -> Boolean) {
        repeat(MAX_WAITS) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(WAIT_MILLIS)
        }
        error("Timed out")
    }

    private companion object {
        const val MAX_WAITS = 500
        const val WAIT_MILLIS = 10L
        const val TICK_MILLIS = 1_100L
    }
}
