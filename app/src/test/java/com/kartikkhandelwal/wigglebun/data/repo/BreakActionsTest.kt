package com.kartikkhandelwal.wigglebun.data.repo

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kartikkhandelwal.wigglebun.domain.model.BreakOutcome
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.ResetReason
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Home's Skip rules (M5). Done now goes through the break session: see RecordBreakDoneTest. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class)
class BreakActionsTest {

    private val app = TestDataLayer(ApplicationProvider.getApplicationContext())
    private val actions = BreakActions(app.sitRepository, app.breaks, app.clock)

    @After
    fun tearDown() = app.close()

    @Test
    fun skipBeforeDueOnlyLogsAndKeepsTheTimer() = runBlocking {
        val started = app.sittingFor(5) // due in 40 min
        actions.skip(BreakType.NECK, reminderDue = false)
        assertEquals(started, app.store.current().sitStartedAt) // not a cheat: the pet stays as tired as before
        val log = app.logs().single()
        assertEquals(BreakType.NECK, log.type)
        assertEquals(BreakOutcome.SKIPPED, log.outcome)
        assertEquals(0, log.reward)
    }

    @Test
    fun skipWhenDueIsTheNotificationSkip() = runBlocking {
        val started = app.sittingFor(50)
        actions.skip(BreakType.WALK, reminderDue = true)
        val state = app.store.current()
        assert(state.sitStartedAt!!.isAfter(started))
        assertEquals(ResetReason.SKIP, state.lastResetReason)
        assertEquals(BreakOutcome.SKIPPED, app.logs().single().outcome)
    }
}
