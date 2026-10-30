package com.kartikkhandelwal.wigglebun.data.repo

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kartikkhandelwal.wigglebun.data.prefs.MovementStore
import com.kartikkhandelwal.wigglebun.data.prefs.ReceivedTransition
import com.kartikkhandelwal.wigglebun.domain.model.MovementActivity
import com.kartikkhandelwal.wigglebun.domain.model.MovementActivity.STILL
import com.kartikkhandelwal.wigglebun.domain.model.MovementActivity.WALKING
import com.kartikkhandelwal.wigglebun.domain.model.MovementTransition
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * The real-phone log of 2026-10-30, where older times were logged after newer ones. Real events
 * can't do that: the "newest processed" mark is saved in DataStore, so it also holds across app
 * processes. Only the lab's Simulate walk skips it, and it backdates its WALKING line by 3 minutes.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class)
class MovementLogOrderTest {

    private val app = TestDataLayer(ApplicationProvider.getApplicationContext())

    private val zone = ZoneId.of("Asia/Kolkata")

    private fun at(time: String): Instant = LocalDateTime.parse("2026-10-30T$time").atZone(zone).toInstant()

    /** Stands for elapsedRealtime, which only ever grows during a boot. */
    private fun received(activity: MovementActivity, time: String) =
        ReceivedTransition(MovementTransition(activity, entered = true, at = at(time)), elapsedRealtimeNanos = at(time).epochSecond * 1_000_000_000)

    @After
    fun tearDown() = app.close()

    @Test
    fun realEventsOlderThanTheNewestProcessedAreDroppedEvenInANewProcess() = runBlocking {
        val first = MovementStore(app.dataStore, app.dataStore)
        first.acceptAndLog(listOf(received(STILL, "21:52:49")), BOOT, at("21:52:50"))

        // A new process has new objects, and reads the same files.
        val second = MovementStore(app.dataStore, app.dataStore)
        val accepted = second.acceptAndLog(
            listOf(received(STILL, "20:49:56"), received(WALKING, "21:49:51")),
            BOOT,
            at("21:52:52"),
        )

        assertTrue(accepted.isEmpty())
        assertEquals(listOf(at("21:52:49")), second.current().log.map { it.at })
    }

    /** Two Simulate walk taps 2 s apart, then the phone's real state: the lines from the phone, in that order. */
    @Test
    fun simulatedWalksSkipTheCheckAndProduceTheLinesFromThePhone() = runBlocking {
        val repo = app.sitRepository
        repo.simulateTransitions(simulatedWalk("21:49:49", "21:52:49"))
        repo.simulateTransitions(simulatedWalk("21:49:51", "21:52:51"))
        // Simulating never moves the mark, so the real STILL from an hour ago is still news.
        repo.onReceivedTransitions(listOf(received(STILL, "20:49:56")), BOOT, at("21:52:53"))

        val bottomUp = app.movementStore.current().log.reversed().map { it.at }
        assertEquals(
            listOf(at("21:49:49"), at("21:52:49"), at("21:49:51"), at("21:52:51"), at("20:49:56")),
            bottomUp,
        )
        // The engine gets the older event last, and it changes nothing.
        assertEquals(at("21:52:51"), app.store.current().sitStartedAt)
    }

    /** The log marks what was simulated, so nobody has to work it out from the times again. */
    @Test
    fun simulatedLinesAreMarkedAndRealOnesAreNot() = runBlocking {
        val store = app.movementStore
        store.acceptAndLog(listOf(received(WALKING, "21:53:05")), BOOT, at("21:53:07"))
        store.appendLog(simulatedWalk("21:53:10", "21:56:10"), arrivedAt = at("21:56:10"))

        val byTime = store.current().logByEventTime
        assertEquals(listOf(true, true, false), byTime.map { it.simulated })
    }

    private fun simulatedWalk(start: String, end: String) = listOf(
        MovementTransition(WALKING, entered = true, at = at(start)),
        MovementTransition(STILL, entered = true, at = at(end)),
    )

    private companion object {
        const val BOOT = 7
    }
}
