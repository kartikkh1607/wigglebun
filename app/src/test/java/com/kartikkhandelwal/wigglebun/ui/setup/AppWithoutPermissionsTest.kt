package com.kartikkhandelwal.wigglebun.ui.setup

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kartikkhandelwal.wigglebun.data.repo.TestDataLayer
import com.kartikkhandelwal.wigglebun.domain.model.BreakOutcome
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.ResetReason
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** CLAUDE.md §10: the app works fully, timer and manual Done, with both permissions denied. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class)
class AppWithoutPermissionsTest {

    private val app = TestDataLayer(ApplicationProvider.getApplicationContext())

    @After
    fun tearDown() = app.close()

    @Test
    fun theTimerRunsAndDoneStillWorks() = runBlocking {
        app.sittingFor(minutes = 50)
        app.sitRepository.onAppStart()

        // Due, and no way to notify: marked as handled, nothing crashes, tracking stays off.
        app.sitRepository.checkReminder()
        assertNotNull(app.store.current().lastNotifiedDue)
        assertFalse(app.movementStore.current().registered)

        val result = app.recordBreakDone(BreakType.WALK)
        assertEquals(BreakType.WALK.defaultReward, result.sparksEarned)
        assertEquals(ResetReason.DONE, app.store.current().lastResetReason)
        assertEquals(BreakOutcome.DONE, app.logs().single().outcome)
        assertEquals(BreakType.WALK.defaultReward, app.balance())
    }
}
