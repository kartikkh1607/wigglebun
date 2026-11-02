package com.kartikkhandelwal.wigglebun.ui.settings

import android.app.Application
import android.os.Looper
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kartikkhandelwal.wigglebun.data.repo.TestDataLayer
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.rewards.RewardRules
import com.kartikkhandelwal.wigglebun.domain.sitting.WorkSchedule
import com.kartikkhandelwal.wigglebun.ui.onboarding.WorkHoursProblem
import com.kartikkhandelwal.wigglebun.ui.onboarding.WorkTime
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.DayOfWeek

/** Settings saves straight away, and refuses what would leave the app with nothing to do. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class)
class SettingsViewModelTest {

    private val app = TestDataLayer(ApplicationProvider.getApplicationContext())
    private val viewModel = SettingsViewModel(app.settings, app.widgetUpdates)

    init {
        // WhileSubscribed: keep it hot, as the screen does.
        app.scope.launch { viewModel.state.collect {} }
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        app.close()
    }

    /** Only reads the view model's StateFlow, so a save on the main looper can always run. */
    private suspend fun waitUntil(condition: (SettingsUiState) -> Boolean) = withTimeout(10_000) {
        while (viewModel.state.value?.let(condition) != true) {
            shadowOf(Looper.getMainLooper()).idle()
            delay(10)
        }
    }

    @Test
    fun startsWithWhatsStored() = runBlocking {
        waitUntil { it.hours.toSchedule() == WorkSchedule.Default && it.intervalMinutes == 45 && it.enabledBreaks == BreakType.entries }
    }

    @Test
    fun aDayChangeSavesStraightAway() = runBlocking {
        waitUntil { true }
        viewModel.onToggleDay(DayOfWeek.SATURDAY)
        waitUntil { DayOfWeek.SATURDAY in it.hours.days }
        assertEquals(WorkSchedule.Default.days + DayOfWeek.SATURDAY, app.settings.current().schedule.days)
    }

    /** Skipping onboarding must not leave anyone on 9 to 6 for good. */
    @Test
    fun hoursCanBeChanged() = runBlocking {
        waitUntil { true }
        viewModel.onTimePicked(WorkTime.START, 22 * 60)
        waitUntil { it.hours.startMinute == 22 * 60 }
        viewModel.onTimePicked(WorkTime.END, 6 * 60)
        waitUntil { it.hours.endMinute == 6 * 60 && it.hours.endsNextDay }
        assertEquals(22 * 60, app.settings.current().schedule.startMinute)
        assertEquals(6 * 60, app.settings.current().schedule.endMinute)
    }

    @Test
    fun theIntervalCanBeChanged() = runBlocking {
        waitUntil { true }
        viewModel.onIntervalPicked(60)
        waitUntil { it.intervalMinutes == 60 }
        assertEquals(60, app.settings.current().intervalMinutes)
    }

    @Test
    fun theLastWorkDayStaysOnAndTheScreenSaysWhy() = runBlocking {
        waitUntil { true }
        (DayOfWeek.entries.take(4)).forEach(viewModel::onToggleDay)
        waitUntil { it.hours.days == setOf(DayOfWeek.FRIDAY) }

        viewModel.onToggleDay(DayOfWeek.FRIDAY)
        waitUntil { it.hoursProblem == WorkHoursProblem.NO_DAYS }
        assertEquals(setOf(DayOfWeek.FRIDAY), app.settings.current().schedule.days)

        viewModel.onToggleDay(DayOfWeek.MONDAY) // a good change clears the message
        waitUntil { it.hoursProblem == null && DayOfWeek.MONDAY in it.hours.days }
    }

    @Test
    fun theSameStartAndEndIsRefused() = runBlocking {
        waitUntil { true }
        viewModel.onTimePicked(WorkTime.END, WorkSchedule.Default.startMinute)
        waitUntil { it.hoursProblem == WorkHoursProblem.SAME_TIME }
        assertEquals(WorkSchedule.Default, app.settings.current().schedule)
    }

    /** Quick taps apply to what's stored, so none overwrites another. */
    @Test
    fun quickChangesAllLand() = runBlocking {
        waitUntil { true }
        viewModel.onToggleDay(DayOfWeek.SATURDAY)
        viewModel.onToggleDay(DayOfWeek.SUNDAY)
        viewModel.onToggleDay(DayOfWeek.MONDAY)
        waitUntil { it.hours.days == WorkSchedule.Default.days + DayOfWeek.SATURDAY + DayOfWeek.SUNDAY - DayOfWeek.MONDAY }
    }

    @Test
    fun aBreakCanBeHiddenAndComesBackInItsPlace() = runBlocking {
        waitUntil { true }
        viewModel.onBreakToggled(BreakType.WALK, false)
        waitUntil { BreakType.WALK !in it.enabledBreaks }
        viewModel.onBreakToggled(BreakType.WALK, true)
        waitUntil { it.enabledBreaks == BreakType.entries }
    }

    @Test
    fun theLastBreakCantBeSwitchedOff() = runBlocking {
        BreakType.entries.drop(1).forEach { assertTrue(app.settings.setBreakEnabled(it, false)) }
        assertFalse(app.settings.setBreakEnabled(BreakType.entries.first(), false))
        assertEquals(listOf(BreakType.entries.first()), app.settings.enabledBreaks.first())
    }

    /** The widget's button suggests an enabled break, so a change has to redraw it. */
    @Test
    fun switchingABreakRedrawsTheWidget() = runBlocking {
        withTimeout(10_000) { while (app.widgetUpdates.count == 0) delay(10) } // the startup check
        val before = app.widgetUpdates.count
        viewModel.onBreakToggled(BreakType.WATER, false)
        withTimeout(10_000) {
            while (app.widgetUpdates.count == before) {
                shadowOf(Looper.getMainLooper()).idle()
                delay(10)
            }
        }
    }

    /** Fewer types than the goal: the goal comes down, from today, so the streak stays reachable. */
    @Test
    fun theStreakGoalFollowsTheToggles() = runBlocking {
        BreakType.entries.drop(2).forEach { app.settings.setBreakEnabled(it, false) }
        val today = app.clock.instant().atZone(app.clock.zone).toLocalDate()
        val goals = app.settings.dailyGoalHistory.first()
        assertEquals(2, goals.goalOn(today))
        assertEquals(RewardRules.DAILY_STREAK_GOAL, goals.goalOn(today.minusDays(1)))
    }

    @Test
    fun aBreakDoneWithOneTypeOnMeetsTheLowerGoal() = runBlocking {
        app.settings.setSchedule(WorkSchedule.AlwaysOn) // a streak only counts work days, and this may run on a Sunday
        BreakType.entries.drop(1).forEach { app.settings.setBreakEnabled(it, false) }
        assertEquals(1, app.recordBreakDone(BreakType.entries.first()).streak.goal)
        assertEquals(1, app.recordBreakDone(BreakType.entries.first()).streak.days)
    }
}
