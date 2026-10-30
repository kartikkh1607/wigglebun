package com.kartikkhandelwal.wigglebun.ui.home

import android.app.Application
import android.os.Looper
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.data.repo.BreakActions
import com.kartikkhandelwal.wigglebun.data.repo.TestDataLayer
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.domain.model.PetId
import com.kartikkhandelwal.wigglebun.domain.model.SparkReason
import com.kartikkhandelwal.wigglebun.domain.rewards.PetPrices
import com.kartikkhandelwal.wigglebun.domain.sitting.SittingEngine
import com.kartikkhandelwal.wigglebun.widget.WidgetUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Instant

/** M8: switching the active pet reaches Home and the widget (the real repositories, no Hilt). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class)
class ActivePetSwitchTest {

    private val app = TestDataLayer(ApplicationProvider.getApplicationContext())
    private val home = HomeViewModel(
        sitRepository = app.sitRepository,
        settingsRepository = app.settings,
        breakRepository = app.breaks,
        walletRepository = app.wallet,
        petRepository = app.pets,
        actions = BreakActions(app.sitRepository, app.breaks, app.clock),
        clock = app.clock,
    )
    private val collector = CoroutineScope(Dispatchers.Main)

    @After
    fun tearDown() {
        collector.cancel()
        home.viewModelScope.cancel()
        app.close()
    }

    /** Runs main-thread work (viewModelScope) until [condition] holds; IO work resumes there. */
    private fun waitUntil(condition: () -> Boolean) {
        repeat(MAX_WAITS) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(WAIT_MILLIS)
        }
        error("Timed out")
    }

    @Test
    fun adoptingAndChoosingUpdatesHomeAndTheWidget() {
        app.sittingFor(30) // TIRED with the default 45-min interval
        collector.launch { home.state.collect {} } // WhileSubscribed: keep it hot
        waitUntil { home.state.value?.pet?.id == PetId.BUN }

        val redrawsBefore = app.widgetUpdates.count
        runBlocking {
            app.wallet.award("break:test", PetPrices.of(PetId.PIP) + 50, SparkReason.BREAK, Instant.now())
            app.pets.adopt(PetId.PIP)
        }
        // The pet comes from DataStore and the balance from a Room query that re-emits after its
        // invalidation, so Home can show Pip a moment before the new balance: wait for both.
        waitUntil { home.state.value?.let { it.pet?.id == PetId.PIP && it.sparks == 50 } == true }
        assertEquals(50, home.state.value?.sparks)
        assertTrue("adopting redraws the widget", app.widgetUpdates.count > redrawsBefore)

        // What the widget would draw now (loadWidgetState does exactly this with Hilt).
        val status = runBlocking { SittingEngine(app.clock).status(app.store.current(), app.settings.current()) }
        val pet = runBlocking { app.pets.activePet.first() }
        val widget = WidgetUiState.from(status, pet.id, "Pip")
        assertEquals(Mood.TIRED, widget.mood)
        assertEquals(R.drawable.pip_mood_tired, widget.petImage)

        // And back to Bun.
        val redrawsAfterAdopt = app.widgetUpdates.count
        runBlocking { app.pets.choose(PetId.BUN) }
        waitUntil { home.state.value?.pet?.id == PetId.BUN }
        assertTrue("choosing redraws the widget", app.widgetUpdates.count > redrawsAfterAdopt)
    }

    private companion object {
        const val MAX_WAITS = 500
        const val WAIT_MILLIS = 10L
    }
}
