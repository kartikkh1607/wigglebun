package com.kartikkhandelwal.wigglebun.activity

import android.Manifest
import android.app.Application
import android.app.PendingIntent
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kartikkhandelwal.wigglebun.data.prefs.MovementStore
import com.kartikkhandelwal.wigglebun.data.prefs.StepBaseline
import com.kartikkhandelwal.wigglebun.domain.model.MovementActivity
import com.kartikkhandelwal.wigglebun.domain.model.MovementTransition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class)
class MovementPlatformTest {

    @get:Rule
    val tmp = TemporaryFolder()

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
        val store = MovementStore(PreferenceDataStoreFactory.create(scope = scope) { tmp.newFile("m.preferences_pb") })
        val base = Instant.parse("2026-10-26T05:00:00Z")
        repeat(25) { i ->
            store.appendLog(listOf(MovementTransition(MovementActivity.WALKING, entered = i % 2 == 0, at = base.plusSeconds(i * 60L))))
        }
        val log = store.current().log
        assertEquals(MovementStore.LOG_SIZE, log.size)
        assertEquals(base.plusSeconds(24 * 60L), log.first().at)
        assertEquals(MovementActivity.WALKING, store.current().currentActivity) // newest (i=24) is an enter
    }
}
