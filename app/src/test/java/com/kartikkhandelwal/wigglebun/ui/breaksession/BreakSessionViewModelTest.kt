package com.kartikkhandelwal.wigglebun.ui.breaksession

import android.app.Application
import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kartikkhandelwal.wigglebun.data.repo.TestDataLayer
import com.kartikkhandelwal.wigglebun.data.time.ElapsedClock
import com.kartikkhandelwal.wigglebun.domain.model.BreakOutcome
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
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
import java.time.Duration

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class)
class BreakSessionViewModelTest {

    private val app = TestDataLayer(ApplicationProvider.getApplicationContext())
    private var now = 0L
    private val elapsed = ElapsedClock { now }
    private val viewModels = mutableListOf<BreakSessionViewModel>()

    @After
    fun tearDown() {
        viewModels.forEach { it.viewModelScope.cancel() }
        app.close()
    }

    private fun session(type: BreakType) = BreakSessionViewModel(
        savedStateHandle = SavedStateHandle(mapOf("type" to type.name)),
        recordBreakDone = app.recordBreakDone,
        elapsed = elapsed,
        settingsRepository = app.settings,
    ).also { viewModels += it }

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
    fun closingMidSessionRecordsNothing() {
        val started = app.sittingFor(20)
        val vm = session(BreakType.NECK)
        now += 40_000
        vm.nextStep()
        vm.close()
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(vm.state.value.isPaused)
        assertTrue(app.logs().isEmpty())
        assertTrue(app.ledger().isEmpty())
        assertEquals(started, runBlocking { app.store.current().sitStartedAt })
    }

    @Test
    fun doneRecordsOnceAndCelebrates() {
        app.sittingFor(20)
        val vm = session(BreakType.NECK)
        repeat(3) { vm.nextStep() }
        assertTrue(vm.state.value.isLastStep)

        vm.done()
        vm.done() // a double tap must not complete twice
        waitUntil { vm.state.value.result != null }

        val state = vm.state.value
        assertEquals(Mood.CELEBRATE, state.mood)
        assertEquals(BreakType.NECK.defaultReward, state.result!!.sparksEarned)
        assertEquals(BreakType.NECK.defaultReward, app.balance())
        val log = app.logs().single()
        assertEquals(BreakType.NECK, log.type)
        assertEquals(BreakOutcome.DONE, log.outcome)
    }

    @Test
    fun walkKeepsRunningInTheBackgroundOtherSessionsPause() {
        val walk = session(BreakType.WALK)
        walk.onBackground()
        assertFalse(walk.state.value.isPaused)
        now += 90_000 // pocketed for a minute and a half
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(TICK_WAIT_MILLIS))
        // Nobody is watching in the background, so nothing redraws...
        assertEquals(0, walk.state.value.stepIndex)
        // ...but the walk kept running: back on screen, the next tick shows the real time.
        val walkScreen = onScreen(walk)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(TICK_WAIT_MILLIS))
        assertEquals(1, walk.state.value.stepIndex)
        assertEquals(30_000L, walk.state.value.remainingMillis)
        walkScreen.cancel()

        val neck = session(BreakType.NECK)
        neck.onBackground()
        assertTrue(neck.state.value.isPaused)
        now += 90_000
        val neckScreen = onScreen(neck)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(TICK_WAIT_MILLIS))
        assertEquals(0, neck.state.value.stepIndex)
        assertEquals(30_000L, neck.state.value.remainingMillis)
        neckScreen.cancel()
    }

    private companion object {
        const val MAX_WAITS = 500
        const val WAIT_MILLIS = 10L
        const val TICK_WAIT_MILLIS = 250L
    }

    /** Collects the state like the screen does (collectAsStateWithLifecycle while visible). */
    private fun onScreen(vm: BreakSessionViewModel): Job = CoroutineScope(Dispatchers.Main).launch { vm.state.collect {} }
}
