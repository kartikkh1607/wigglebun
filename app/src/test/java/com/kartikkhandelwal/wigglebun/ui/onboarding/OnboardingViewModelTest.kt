package com.kartikkhandelwal.wigglebun.ui.onboarding

import android.app.Application
import android.os.Looper
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kartikkhandelwal.wigglebun.data.repo.TestDataLayer
import com.kartikkhandelwal.wigglebun.domain.model.ActivePet
import com.kartikkhandelwal.wigglebun.domain.model.PetId
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
    fun finishingOpensTheApp() = runBlocking {
        viewModel.finish()
        waitUntil { app.settings.onboardingDone.first() == true }
    }
}
