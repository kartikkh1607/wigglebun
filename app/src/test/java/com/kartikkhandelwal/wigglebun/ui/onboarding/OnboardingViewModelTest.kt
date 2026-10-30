package com.kartikkhandelwal.wigglebun.ui.onboarding

import android.app.Application
import android.os.Looper
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kartikkhandelwal.wigglebun.data.repo.TestDataLayer
import com.kartikkhandelwal.wigglebun.domain.model.ActivePet
import com.kartikkhandelwal.wigglebun.domain.model.PetId
import com.kartikkhandelwal.wigglebun.domain.sitting.SittingSettings
import com.kartikkhandelwal.wigglebun.domain.sitting.WorkSchedule
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
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

    // Watched from the data layer's scope. Reading DataStore from the test while a save is running
    // would deadlock: the save's transform runs on the main thread, which the test is holding.
    private val petName = app.settings.petName.stateIn(app.scope, SharingStarted.Eagerly, UNSET)
    private val settings = app.settings.settings.stateIn(app.scope, SharingStarted.Eagerly, null)
    private val onboardingDone = app.settings.onboardingDone.stateIn(app.scope, SharingStarted.Eagerly, null)

    private lateinit var viewModel: OnboardingViewModel

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        app.close()
    }

    /** viewModelScope runs on the main looper, which Robolectric only runs when asked. */
    private suspend fun waitUntil(condition: () -> Boolean) = withTimeout(10_000) {
        while (!condition()) {
            shadowOf(Looper.getMainLooper()).idle()
            delay(10)
        }
    }

    /** A view model that has finished reading what's stored, so nothing overwrites a test's edits. */
    private suspend fun loadedViewModel() {
        app.settings.setInterval(STORED_INTERVAL)
        viewModel = OnboardingViewModel(app.settings)
        waitUntil { viewModel.intervalMinutes.value == STORED_INTERVAL }
    }

    @Test
    fun theNameIsSavedCleanAndNamesBun() = runBlocking {
        loadedViewModel()
        viewModel.onNameChange("  Biscuit ")
        viewModel.saveName()
        waitUntil { petName.value == "Biscuit" }
        assertEquals("Biscuit", app.pets.activePet.first().customName)
    }

    @Test
    fun aBlankNameKeepsBunsOwnName() = runBlocking {
        app.settings.setPetName("Old")
        loadedViewModel()
        viewModel.onNameChange("   ")
        viewModel.saveName()
        waitUntil { petName.value == null }
        assertEquals(ActivePet(PetId.BUN, chosenName = null), app.pets.activePet.first())
    }

    @Test
    fun theFieldStopsAtTheLimit() = runBlocking {
        loadedViewModel()
        viewModel.onNameChange("Wigglebottomley")
        assertEquals("Wigglebottom", viewModel.nameDraft.value)
    }

    /** Killed halfway: the name already chosen is back in the field. */
    @Test
    fun aSavedNameStartsInTheField() = runBlocking {
        app.settings.setPetName("Biscuit")
        loadedViewModel()
        assertEquals("Biscuit", viewModel.nameDraft.value)
    }

    @Test
    fun workHoursAndIntervalAreSavedOnContinue() = runBlocking {
        loadedViewModel()
        viewModel.onToggleDay(DayOfWeek.SATURDAY)
        viewModel.onTimePicked(WorkTime.START, 8 * 60 + 30)
        viewModel.onTimePicked(WorkTime.END, 17 * 60)
        viewModel.onIntervalPicked(60)
        // Nothing is saved until Continue, so Skip can leave the stored values alone.
        assertEquals(WorkSchedule.Default, app.settings.current().schedule)
        assertEquals(STORED_INTERVAL, app.settings.current().intervalMinutes)

        viewModel.saveWorkHours()
        viewModel.saveInterval()
        waitUntil { settings.value?.intervalMinutes == 60 && settings.value?.schedule != WorkSchedule.Default }
        val schedule = app.settings.current().schedule
        assertEquals(WorkSchedule.Default.days + DayOfWeek.SATURDAY, schedule.days)
        assertEquals(8 * 60 + 30, schedule.startMinute)
        assertEquals(17 * 60, schedule.endMinute)
    }

    @Test
    fun anImpossibleScheduleIsNeverSaved() = runBlocking {
        loadedViewModel()
        DayOfWeek.entries.take(5).forEach(viewModel::onToggleDay)
        viewModel.saveWorkHours()
        viewModel.onIntervalPicked(60)
        viewModel.saveInterval() // something later to wait for
        waitUntil { settings.value?.intervalMinutes == 60 }
        assertEquals(WorkSchedule.Default, app.settings.current().schedule)
    }

    @Test
    fun finishingOpensTheApp() = runBlocking {
        loadedViewModel()
        viewModel.finish()
        waitUntil { onboardingDone.value == true }
    }

    private companion object {
        const val UNSET = "unset"

        /** Differs from the default, so seeing it proves the stored values have loaded. */
        val STORED_INTERVAL = SittingSettings.DEFAULT_INTERVAL_MINUTES + 5
    }
}
