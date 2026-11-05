package com.kartikkhandelwal.wigglebun.data.repo

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.rewards.RewardRules
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Duration

/** The streak in one place, for Home, the profile and Settings (real data layer, no Hilt). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class)
class StreakRepositoryTest {

    private val app = TestDataLayer(ApplicationProvider.getApplicationContext())

    @After
    fun tearDown() = app.close()

    private fun summaryWhere(condition: (StreakSummary) -> Boolean): StreakSummary =
        runBlocking { withTimeout(TIMEOUT_MILLIS) { app.streaks.summary.first(condition) } }

    @Test
    fun countsTheStreakTodaysProgressAndEveryBreak() {
        app.alwaysWorkHours()
        app.dailyGoalSince(app.today().minusDays(10))
        val now = app.clock.instant()
        // Yesterday met the goal (repeats count); today has 2 of 3 so far.
        val yesterday = now.minus(Duration.ofDays(1))
        listOf(BreakType.WALK, BreakType.WALK, BreakType.NECK).forEach { app.logDone(it, yesterday) }
        listOf(BreakType.STAND_UP, BreakType.EYES).forEach { app.logDone(it, now) }

        val summary = summaryWhere { it.totalBreaks == 5 }
        assertEquals(StreakSummary(days = 1, todayDone = 2, goal = RewardRules.DAILY_GOAL.count, todayIsWorkDay = true, totalBreaks = 5), summary)
    }

    @Test
    fun aNewBreakUpdatesIt() {
        app.alwaysWorkHours()
        app.dailyGoalSince(app.today().minusDays(10))
        summaryWhere { it.totalBreaks == 0 }

        listOf(BreakType.STAND_UP, BreakType.WATER, BreakType.NECK).forEach { app.logDone(it, app.clock.instant()) }

        val summary = summaryWhere { it.totalBreaks == 3 }
        assertEquals(3, summary.todayDone)
        assertEquals("today met the goal, so it counts", 1, summary.days)
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
    }
}
