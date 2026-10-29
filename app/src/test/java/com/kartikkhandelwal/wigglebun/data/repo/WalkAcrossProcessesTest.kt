package com.kartikkhandelwal.wigglebun.data.repo

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kartikkhandelwal.wigglebun.activity.MovementPermissions
import com.kartikkhandelwal.wigglebun.activity.MovementTracker
import com.kartikkhandelwal.wigglebun.activity.StepCounter
import com.kartikkhandelwal.wigglebun.activity.TransitionRegistrar
import com.kartikkhandelwal.wigglebun.data.db.AppDatabase
import com.kartikkhandelwal.wigglebun.data.prefs.MovementStore
import com.kartikkhandelwal.wigglebun.data.prefs.SettingsRepository
import com.kartikkhandelwal.wigglebun.data.prefs.SitStateStore
import com.kartikkhandelwal.wigglebun.data.prefs.inMemoryPreferencesDataStore
import com.kartikkhandelwal.wigglebun.data.time.AppClock
import com.kartikkhandelwal.wigglebun.domain.model.MovementActivity
import com.kartikkhandelwal.wigglebun.domain.model.MovementTransition
import com.kartikkhandelwal.wigglebun.domain.model.ResetReason
import com.kartikkhandelwal.wigglebun.domain.sitting.SittingEngine
import com.kartikkhandelwal.wigglebun.domain.sitting.WorkSchedule
import com.kartikkhandelwal.wigglebun.notify.ReminderNotifier
import com.kartikkhandelwal.wigglebun.notify.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Movement events usually arrive in a fresh app process, whose first check registers with Play
 * services again. The earlier registration lasts until reboot, app update or force-stop, so if
 * that re-registration fails, the walk's exit event still arrives and must still end the walk.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class)
class WalkAcrossProcessesTest {

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dataStore = inMemoryPreferencesDataStore(storeScope)
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    private val clock = AppClock()
    private val settings = SettingsRepository(dataStore, clock)
    private val store = SitStateStore(dataStore)
    private val movementStore = MovementStore(dataStore, dataStore)

    /** Permission granted and Play services present, as on a real phone with detection on. */
    private val permissions = object : MovementPermissions(context) {
        override fun granted() = true

        override fun playServicesAvailable() = true
    }

    /** Play services registration that succeeds or fails on request. */
    private class FakeRegistrar(context: Context, permissions: MovementPermissions, private val succeeds: Boolean) :
        TransitionRegistrar(context, permissions) {
        override suspend fun register(): Boolean = succeeds

        override suspend fun unregister() = Unit
    }

    /** One app process: its own MovementTracker (with its once-per-process registration) and SitRepository. */
    private inner class AppProcess(registrationSucceeds: Boolean) : AutoCloseable {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val widget = CountingWidgetRefresher()
        private val stepCounter = StepCounter(context, permissions)
        val repo = SitRepository(
            engine = SittingEngine(clock),
            clock = clock,
            settingsRepository = settings,
            store = store,
            breaks = BreakRepository(db.breakLogDao()),
            scheduler = ReminderScheduler(context, context.getSystemService<AlarmManager>()!!, clock),
            notifier = ReminderNotifier(context, NotificationManagerCompat.from(context)).apply { createChannel() },
            movementStore = movementStore,
            movementTracker = MovementTracker(
                permissions,
                FakeRegistrar(context, permissions, registrationSucceeds),
                stepCounter,
                movementStore,
            ),
            stepCounter = stepCounter,
            widget = widget,
            petRepository = PetRepository(WalletRepository(db.sparkLedgerDao()), settings, clock, widget),
            scope = scope,
        )

        /** The process starts with a background check (settings collector); wait until it finished (it redraws the widget last). */
        suspend fun awaitStartupCheck() = withTimeout(10_000) {
            while (widget.count == 0) delay(10)
        }

        override fun close() = scope.cancel()
    }

    @Before
    fun setUp() {
        setBootCount(7)
        runBlocking {
            settings.setSchedule(WorkSchedule.AlwaysOn)
            store.update { it.copy(sitStartedAt = Instant.now().minusSeconds(40 * 60)) }
        }
    }

    @After
    fun tearDown() {
        db.close()
        storeScope.cancel()
    }

    private fun setBootCount(count: Int) {
        Settings.Global.putInt(context.contentResolver, Settings.Global.BOOT_COUNT, count)
    }

    /** Process 1 registers and sees the walk start [walkedMinutes] ago, then dies. */
    private suspend fun startWalkInFirstProcess(walkedMinutes: Long) {
        AppProcess(registrationSucceeds = true).use { first ->
            first.awaitStartupCheck()
            assertTrue(movementStore.current().registered)
            val enter = Instant.now().truncatedTo(ChronoUnit.MILLIS).minusSeconds(walkedMinutes * 60)
            first.repo.simulateTransitions(listOf(MovementTransition(MovementActivity.WALKING, entered = true, at = enter)))
            assertEquals(enter, store.current().movingSince)
        }
    }

    @Test
    fun failedReRegistrationInANewProcessKeepsTheWalk() = runBlocking {
        startWalkInFirstProcess(walkedMinutes = 5)

        // Process 2 (same boot, same app version): Play services is busy and registration fails.
        AppProcess(registrationSucceeds = false).use { second ->
            second.awaitStartupCheck()
            second.repo.checkReminder() // e.g. after a settings change

            // The first registration is still live, so the walk's exit still arrives.
            val exit = Instant.now().truncatedTo(ChronoUnit.MILLIS)
            second.repo.simulateTransitions(listOf(MovementTransition(MovementActivity.WALKING, entered = false, at = exit)))
            assertEquals(exit, store.current().sitStartedAt)
            assertEquals(ResetReason.WALKING, store.current().lastResetReason)
        }
    }

    /** After a reboot the old registration is gone: a failed registration really means no tracking. */
    @Test
    fun afterARebootAFailedRegistrationClearsTheWalk() = runBlocking {
        startWalkInFirstProcess(walkedMinutes = 5)
        setBootCount(8)

        AppProcess(registrationSucceeds = false).use { second ->
            second.awaitStartupCheck()
            second.repo.checkReminder()
            assertNull(store.current().movingSince)
            assertFalse(movementStore.current().registered)
        }
    }
}
