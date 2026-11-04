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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.DayOfWeek

/**
 * Work hours are edited freely: no work days turns Save off at once, and the same start and end is
 * checked only on Save. The interval and the break switches save straight away. Nothing can leave
 * the app with no work days or no breaks.
 */
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

    private suspend fun stored(): WorkSchedule = app.settings.current().schedule

    /** Waits for Save to land: the edit is gone and the screen shows what's stored. */
    private suspend fun saveAndWait() {
        viewModel.onSaveHours()
        waitUntil { !it.hoursChanged }
    }

    @Test
    fun startsWithWhatsStored() = runBlocking {
        waitUntil { it.hours.toSchedule() == WorkSchedule.Default && it.intervalMinutes == 45 && it.enabledBreaks == BreakType.entries }
        assertFalse(viewModel.state.value!!.hoursChanged)
    }

    @Test
    fun aDayChangeWaitsForSave() = runBlocking {
        waitUntil { true }
        viewModel.onToggleDay(DayOfWeek.SATURDAY)
        waitUntil { DayOfWeek.SATURDAY in it.hours.days && it.hoursChanged }
        assertEquals(WorkSchedule.Default, stored())

        saveAndWait()
        assertEquals(WorkSchedule.Default.days + DayOfWeek.SATURDAY, stored().days)
    }

    /** Skipping onboarding must not leave anyone on 9 to 6 for good. */
    @Test
    fun hoursCanBeChanged() = runBlocking {
        waitUntil { true }
        viewModel.onTimePicked(WorkTime.START, 22 * 60)
        viewModel.onTimePicked(WorkTime.END, 6 * 60)
        waitUntil { it.hours.startMinute == 22 * 60 && it.hours.endMinute == 6 * 60 && it.hours.endsNextDay }
        saveAndWait()
        assertEquals(22 * 60, stored().startMinute)
        assertEquals(6 * 60, stored().endMinute)
    }

    /** 9–18 to 18–02, start first: 18–18 on the way is fine, and nothing complains until Save. */
    @Test
    fun nineToSixBecomesSixToTwoStartFirst() = runBlocking {
        waitUntil { true }
        viewModel.onTimePicked(WorkTime.START, 18 * 60)
        waitUntil { it.hours.startMinute == 18 * 60 }
        assertNull(viewModel.state.value!!.hoursProblem)

        viewModel.onTimePicked(WorkTime.END, 2 * 60)
        waitUntil { it.hours.endMinute == 2 * 60 }
        saveAndWait()
        assertEquals(WorkSchedule(WorkSchedule.Default.days, 18 * 60, 2 * 60), stored())
    }

    @Test
    fun nineToSixBecomesSixToTwoEndFirst() = runBlocking {
        waitUntil { true }
        viewModel.onTimePicked(WorkTime.END, 2 * 60)
        viewModel.onTimePicked(WorkTime.START, 18 * 60)
        waitUntil { it.hours.startMinute == 18 * 60 && it.hours.endMinute == 2 * 60 }
        saveAndWait()
        assertEquals(WorkSchedule(WorkSchedule.Default.days, 18 * 60, 2 * 60), stored())
    }

    /** The check happens on Save: nothing is saved, the reason shows, and the next edit clears it. */
    @Test
    fun savingTheSameStartAndEndIsRefusedWithAReason() = runBlocking {
        waitUntil { true }
        viewModel.onTimePicked(WorkTime.END, WorkSchedule.Default.startMinute)
        waitUntil { it.hours.endMinute == WorkSchedule.Default.startMinute && it.hoursChanged }
        assertNull(viewModel.state.value!!.hoursProblem)

        viewModel.onSaveHours()
        waitUntil { it.hoursProblem == WorkHoursProblem.SAME_TIME }
        assertTrue(viewModel.state.value!!.hoursChanged) // the edit stays on screen to fix
        assertEquals(WorkSchedule.Default, stored())

        viewModel.onTimePicked(WorkTime.END, 17 * 60)
        waitUntil { it.hoursProblem == null && it.hours.endMinute == 17 * 60 }
        saveAndWait()
        assertEquals(17 * 60, stored().endMinute)
    }

    /** No work days: Save turns off and the reason shows at once, without tapping anything. */
    @Test
    fun noWorkDaysTurnsSaveOffAtOnce() = runBlocking {
        waitUntil { true }
        WorkSchedule.Default.days.forEach(viewModel::onToggleDay)
        waitUntil { it.hours.days.isEmpty() }
        val state = viewModel.state.value!!
        assertEquals(WorkHoursProblem.NO_DAYS, state.hoursProblem)
        assertFalse(state.canSaveHours)

        viewModel.onSaveHours() // the button is off; even if called, nothing is saved
        viewModel.onIntervalPicked(60) // something later to wait for
        waitUntil { it.intervalMinutes == 60 }
        assertEquals(WorkSchedule.Default, stored())

        viewModel.onToggleDay(DayOfWeek.MONDAY)
        waitUntil { it.hoursProblem == null && it.canSaveHours }
    }

    /** Changing the hours back to what's stored leaves nothing to save. */
    @Test
    fun undoingAnEditHidesSave() = runBlocking {
        waitUntil { true }
        viewModel.onToggleDay(DayOfWeek.SUNDAY)
        waitUntil { it.hoursChanged }
        viewModel.onToggleDay(DayOfWeek.SUNDAY)
        waitUntil { !it.hoursChanged }
    }

    /** Quick edits build on each other, so none overwrites another. */
    @Test
    fun quickChangesAllLand() = runBlocking {
        waitUntil { true }
        viewModel.onToggleDay(DayOfWeek.SATURDAY)
        viewModel.onToggleDay(DayOfWeek.SUNDAY)
        viewModel.onToggleDay(DayOfWeek.MONDAY)
        saveAndWait()
        assertEquals(WorkSchedule.Default.days + DayOfWeek.SATURDAY + DayOfWeek.SUNDAY - DayOfWeek.MONDAY, stored().days)
    }

    /** The hours decide whether the pet on the widget sleeps. */
    @Test
    fun savingTheHoursRedrawsTheWidget() = runBlocking {
        waitUntil { true }
        withTimeout(10_000) { while (app.widgetUpdates.count == 0) delay(10) } // the startup check
        val before = app.widgetUpdates.count
        viewModel.onTimePicked(WorkTime.END, 20 * 60)
        saveAndWait()
        withTimeout(10_000) {
            while (app.widgetUpdates.count == before) {
                shadowOf(Looper.getMainLooper()).idle()
                delay(10)
            }
        }
    }

    @Test
    fun theIntervalSavesStraightAway() = runBlocking {
        waitUntil { true }
        viewModel.onIntervalPicked(60)
        waitUntil { it.intervalMinutes == 60 }
        assertEquals(60, app.settings.current().intervalMinutes)
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

    /** The goal is 3 completed breaks whichever types are on, so the switches never move it. */
    @Test
    fun theStreakGoalIgnoresTheSwitches() = runBlocking {
        val before = app.settings.dailyGoalHistory.first()
        BreakType.entries.drop(1).forEach { app.settings.setBreakEnabled(it, false) }
        assertEquals(before, app.settings.dailyGoalHistory.first())
        app.settings.setBreakEnabled(BreakType.WALK, true)
        assertEquals(before, app.settings.dailyGoalHistory.first())
    }

    /** With one type on, three of it keep the streak. */
    @Test
    fun oneTypeOnCanStillMeetTheGoal() = runBlocking {
        app.settings.setSchedule(WorkSchedule.AlwaysOn) // a streak only counts work days, and this may run on a Sunday
        BreakType.entries.drop(1).forEach { app.settings.setBreakEnabled(it, false) }
        val only = BreakType.entries.first()
        repeat(RewardRules.DAILY_GOAL.count - 1) { app.recordBreakDone(only) }
        val last = app.recordBreakDone(only)
        assertEquals(3, last.streak.goal)
        assertTrue(last.streak.todayCounts)
        assertEquals(1, last.streak.days)
    }
}
