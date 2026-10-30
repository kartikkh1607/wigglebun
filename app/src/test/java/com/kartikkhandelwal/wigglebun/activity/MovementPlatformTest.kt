package com.kartikkhandelwal.wigglebun.activity

import android.Manifest
import android.app.Application
import android.app.PendingIntent
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kartikkhandelwal.wigglebun.data.prefs.MovementStore
import com.kartikkhandelwal.wigglebun.data.prefs.ReceivedTransition
import com.kartikkhandelwal.wigglebun.data.prefs.StepBaseline
import com.kartikkhandelwal.wigglebun.data.prefs.inMemoryPreferencesDataStore
import com.kartikkhandelwal.wigglebun.domain.model.MovementActivity
import com.kartikkhandelwal.wigglebun.domain.model.MovementTransition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class)
class MovementPlatformTest {

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @After
    fun tearDown() = scope.cancel()

    @Test
    fun transitionPendingIntentIsMutableAndExplicit() {
        val pi = TransitionRegistrar(context, MovementPermissions(context)).pendingIntent()
        val shadow = shadowOf(pi)
        assertTrue(shadow.isBroadcast)
        assertTrue(shadow.flags and PendingIntent.FLAG_MUTABLE != 0)
        assertEquals(MovementReceiver::class.java.name, shadow.savedIntent.component?.className)
    }

    @Test
    fun runtimePermissionGatesEverythingOnApi29Plus() {
        val permissions = MovementPermissions(context)
        assertTrue(permissions.needsRuntimePermission)
        shadowOf(context).denyPermissions(Manifest.permission.ACTIVITY_RECOGNITION)
        assertFalse(permissions.granted())
        shadowOf(context).grantPermissions(Manifest.permission.ACTIVITY_RECOGNITION)
        assertTrue(permissions.granted())
    }

    @Test
    fun noStepSensorMeansNoReading() = runBlocking {
        shadowOf(context).grantPermissions(Manifest.permission.ACTIVITY_RECOGNITION)
        val counter = StepCounter(context, MovementPermissions(context))
        assertFalse(counter.hasSensor) // Robolectric has no step counter: graceful null
        assertNull(counter.readTotal())
    }

    @Test
    fun stepsSinceHandlesReboot() {
        val baseline = StepBaseline(steps = 5_000, bootCount = 7)
        assertEquals(150L, StepCounter.stepsSince(baseline, total = 5_150, bootCount = 7))
        assertEquals(40L, StepCounter.stepsSince(baseline, total = 40, bootCount = 8)) // rebooted
        assertEquals(30L, StepCounter.stepsSince(baseline, total = 30, bootCount = 7)) // counter went back
    }

    @Test
    fun transitionLogKeepsNewestTwenty() = runBlocking {
        val store = MovementStore(inMemoryPreferencesDataStore(scope), inMemoryPreferencesDataStore(scope))
        val base = Instant.parse("2026-10-26T05:00:00Z")
        repeat(25) { i ->
            store.appendLog(listOf(MovementTransition(MovementActivity.WALKING, entered = i % 2 == 0, at = base.plusSeconds(i * 60L))), arrivedAt = base)
        }
        val log = store.current().log
        assertEquals(MovementStore.LOG_SIZE, log.size)
        assertEquals(base.plusSeconds(24 * 60L), log.first().at)
        assertEquals(MovementActivity.WALKING, store.current().currentActivity) // newest (i=24) is an enter
    }

    /** The Pet lab's order: by when things happened, whatever order they were logged in. */
    @Test
    fun logByEventTimeSortsNewestEventFirst() = runBlocking {
        val store = MovementStore(inMemoryPreferencesDataStore(scope), inMemoryPreferencesDataStore(scope))
        val base = Instant.parse("2026-10-26T05:00:00Z")
        fun enter(activity: MovementActivity, seconds: Long) = MovementTransition(activity, entered = true, at = base.plusSeconds(seconds))

        store.appendLog(listOf(enter(MovementActivity.WALKING, 100)), arrivedAt = base.plusSeconds(102))
        // Logged last but happened first, like the debug walk simulation's backdated start.
        store.appendLog(listOf(enter(MovementActivity.STILL, 50)), arrivedAt = base.plusSeconds(200))

        val info = store.current()
        assertEquals(listOf(MovementActivity.STILL, MovementActivity.WALKING), info.log.map { it.activity })
        assertEquals(listOf(MovementActivity.WALKING, MovementActivity.STILL), info.logByEventTime.map { it.activity })
        assertEquals(listOf(base.plusSeconds(102), base.plusSeconds(200)), info.logByEventTime.map { it.arrivedAt })
        assertEquals(MovementActivity.WALKING, info.currentActivity)
    }

    /** Events with the same time stay in delivery order, so "exit STILL, enter WALKING" reads as WALKING now. */
    @Test
    fun eventsWithTheSameTimeKeepTheirOrder() = runBlocking {
        val store = MovementStore(inMemoryPreferencesDataStore(scope), inMemoryPreferencesDataStore(scope))
        val at = Instant.parse("2026-10-26T05:00:00Z")
        store.appendLog(
            listOf(
                MovementTransition(MovementActivity.STILL, entered = false, at = at),
                MovementTransition(MovementActivity.WALKING, entered = true, at = at),
            ),
            arrivedAt = at,
        )
        assertEquals(MovementActivity.WALKING, store.current().currentActivity)
    }

    @Test
    fun acceptedTransitionsAreLoggedWithTheirArrivalTime() = runBlocking {
        val store = MovementStore(inMemoryPreferencesDataStore(scope), inMemoryPreferencesDataStore(scope))
        val at = Instant.parse("2026-10-26T05:00:00Z")
        val event = ReceivedTransition(MovementTransition(MovementActivity.STILL, entered = true, at = at), elapsedRealtimeNanos = 1_000)

        store.acceptAndLog(listOf(event), bootCount = 1, arrivedAt = at.plusSeconds(2))

        assertEquals(at.plusSeconds(2), store.current().log.single().arrivedAt)
    }

    /** An install over an older build keeps its log, whose lines have no arrival time. */
    @Test
    fun logLinesFromAnOlderBuildStillParse() = runBlocking {
        val device = inMemoryPreferencesDataStore(scope)
        val store = MovementStore(inMemoryPreferencesDataStore(scope), device)
        val at = Instant.parse("2026-10-26T05:00:00Z")
        device.edit { it[MovementStore.Keys.Log] = "${at.toEpochMilli()},STILL,1" }

        val line = store.current().log.single()
        assertEquals(at, line.at)
        assertEquals(MovementActivity.STILL, line.activity)
        assertNull(line.arrivedAt)
    }

    /** The switch is a backed-up setting; the log is phone-only (excluded from backup). */
    @Test
    fun switchGoesToSettingsAndLogToDeviceStore() = runBlocking {
        val settings = inMemoryPreferencesDataStore(scope)
        val device = inMemoryPreferencesDataStore(scope)
        val store = MovementStore(settings, device)
        store.setEnabled(false)
        store.appendLog(listOf(MovementTransition(MovementActivity.WALKING, entered = true, at = Instant.parse("2026-10-26T05:00:00Z"))), arrivedAt = Instant.parse("2026-10-26T05:00:02Z"))

        assertEquals(false, store.current().enabled)
        assertEquals(1, store.current().log.size)
        assertEquals(1, settings.data.first().asMap().size)
        assertEquals(1, device.data.first().asMap().size)
    }
}
