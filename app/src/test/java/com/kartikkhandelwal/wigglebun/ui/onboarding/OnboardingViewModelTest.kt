package com.kartikkhandelwal.wigglebun.ui.onboarding

import android.app.Application
import android.os.Looper
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kartikkhandelwal.wigglebun.data.repo.TestDataLayer
import com.kartikkhandelwal.wigglebun.domain.model.ActivePet
import com.kartikkhandelwal.wigglebun.domain.model.PetId
import com.kartikkhandelwal.wigglebun.domain.sitting.WorkSchedule
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.DayOfWeek

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class)
class OnboardingViewModelTest {

    private val app = TestDataLayer(ApplicationProvider.getApplicationContext())
    private val viewModel = OnboardingViewModel(app.settings)

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        app.close()
    }

    /** viewModelScope runs on the main looper, which Robolectric only runs when asked. */
    private suspend fun waitUntil(condition: suspend () -> Boolean) = withTimeout(10_000) {
        while (!condition()) {
            shadowOf(Looper.getMainLooper()).idle()
            delay(10)
        }
    }

    @Test
    fun theNameIsSavedCleanAndNamesBun() = runBlocking {
        viewModel.onNameChange("  Biscuit ")
        viewModel.saveName()
        waitUntil { app.settings.petName.first() == "Biscuit" }
        assertEquals("Biscuit", app.pets.activePet.first().customName)
    }

    @Test
    fun aBlankNameKeepsBunsOwnName() = runBlocking {
        app.settings.setPetName("Old")
        viewModel.onNameChange("   ")
        viewModel.saveName()
        waitUntil { app.settings.petName.first() == null }
        assertEquals(ActivePet(PetId.BUN, chosenName = null), app.pets.activePet.first())
    }

    @Test
    fun theFieldStopsAtTheLimit() {
        viewModel.onNameChange("Wigglebottomley")
        assertEquals("Wigglebottom", viewModel.nameDraft.value)
    }

    /** Killed halfway: the name already chosen is back in the field. */
    @Test
    fun aSavedNameStartsInTheField() = runBlocking {
        app.settings.setPetName("Biscuit")
        val next = OnboardingViewModel(app.settings)
        waitUntil { next.nameDraft.value == "Biscuit" }
        next.viewModelScope.cancel()
    }

    @Test
    fun workHoursAndIntervalAreSavedOnContinue() = runBlocking {
        waitUntil { viewModel.workHours.value == WorkHoursDraft.of(WorkSchedule.Default) }
        viewModel.onToggleDay(DayOfWeek.SATURDAY)
        viewModel.onTimePicked(WorkTime.START, 8 * 60 + 30)
        viewModel.onTimePicked(WorkTime.END, 17 * 60)
        viewModel.onIntervalPicked(60)
        // Nothing is saved until Continue, so Skip can leave the stored values alone.
        assertEquals(WorkSchedule.Default, app.settings.current().schedule)

        viewModel.saveWorkHours()
        viewModel.saveInterval()
        waitUntil { app.settings.current().intervalMinutes == 60 && app.settings.current().schedule != WorkSchedule.Default }
        val schedule = app.settings.current().schedule
        assertEquals(WorkSchedule.Default.days + DayOfWeek.SATURDAY, schedule.days)
        assertEquals(8 * 60 + 30, schedule.startMinute)
        assertEquals(17 * 60, schedule.endMinute)
    }

    @Test
    fun anImpossibleScheduleIsNeverSaved() = runBlocking {
        waitUntil { viewModel.workHours.value == WorkHoursDraft.of(WorkSchedule.Default) }
        DayOfWeek.entries.take(5).forEach(viewModel::onToggleDay)
        viewModel.saveWorkHours()
        viewModel.saveInterval() // something later to wait on
        waitUntil { app.settings.current().intervalMinutes == viewModel.intervalMinutes.value }
        assertEquals(WorkSchedule.Default, app.settings.current().schedule)
    }

    @Test
    fun finishingOpensTheApp() = runBlocking {
        viewModel.finish()
        waitUntil { app.settings.onboardingDone.first() == true }
    }
}
