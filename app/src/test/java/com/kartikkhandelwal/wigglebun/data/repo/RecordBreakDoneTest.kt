package com.kartikkhandelwal.wigglebun.data.repo

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kartikkhandelwal.wigglebun.domain.model.BreakOutcome
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.ResetReason
import com.kartikkhandelwal.wigglebun.domain.model.SparkReason
import com.kartikkhandelwal.wigglebun.domain.rewards.DailyGoal
import com.kartikkhandelwal.wigglebun.domain.rewards.Milestone
import com.kartikkhandelwal.wigglebun.domain.rewards.RewardRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.temporal.ChronoUnit

/** The one completion path (break session + notification Done), CLAUDE.md §5.4. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class)
class RecordBreakDoneTest {

    private val app = TestDataLayer(ApplicationProvider.getApplicationContext())

    @After
    fun tearDown() = app.close()

    @Test
    fun doneResetsCelebratesAndLogsTheType() = runBlocking {
        app.sittingFor(30)
        app.recordBreakDone(BreakType.WATER)
        assertEquals(ResetReason.DONE, app.store.current().lastResetReason)
        val log = app.logs().single()
        assertEquals(BreakType.WATER, log.type)
        assertEquals(BreakOutcome.DONE, log.outcome)
    }

    @Test
    fun firstDoneOfATypePaysItsReward() = runBlocking {
        app.sittingFor(30)
        val result = app.recordBreakDone(BreakType.WALK)
        assertEquals(BreakType.WALK.defaultReward, result.sparksEarned)
        assertEquals(BreakType.WALK.defaultReward, app.balance())
        val row = app.ledger().single()
        assertEquals(SparkReason.BREAK, row.reason)
        assertTrue(row.key.startsWith("break:") && row.key.endsWith(":WALK"))
    }

    @Test
    fun repeatDoneOfTheSameTypeStillResetsTheTimerButPaysOnce() = runBlocking {
        app.sittingFor(30)
        val first = app.recordBreakDone(BreakType.WALK)
        val firstReset = app.store.current().sitStartedAt!!
        // Later the same day: walked again.
        app.store.update { it.copy(sitStartedAt = firstReset.minusSeconds(50 * 60)) }
        val second = app.recordBreakDone(BreakType.WALK)

        val state = app.store.current()
        assert(!state.sitStartedAt!!.isBefore(firstReset)) // back to a fresh sit, not 50 min in
        assertEquals(ResetReason.DONE, state.lastResetReason)
        assertEquals(listOf(BreakOutcome.DONE, BreakOutcome.DONE), app.logs().map { it.outcome })
        assertEquals(setOf(BreakType.WALK), app.logs().map { it.type }.toSet())

        assertEquals(BreakType.WALK.defaultReward, first.sparksEarned)
        assertEquals(0, second.sparksEarned)
        assertEquals(BreakType.WALK.defaultReward, app.balance())
    }

    /** The notification's Done and the in-app Done both go through RecordBreakDone, even at once. */
    @Test
    fun notificationDoneAndInAppDoneForTheSameTypePayOnce() = runBlocking {
        app.sittingFor(50)
        val results = List(2) { async(Dispatchers.IO) { app.recordBreakDone(BreakType.NECK) } }.awaitAll()

        assertEquals(BreakType.NECK.defaultReward, results.sumOf { it.sparksEarned })
        assertEquals(BreakType.NECK.defaultReward, app.balance())
        assertEquals(1, app.ledger().size)
        assertEquals(2, app.logs().size) // both completions are logged; only one pays
    }

    @Test
    fun differentTypesEachPay() = runBlocking {
        app.sittingFor(30)
        app.recordBreakDone(BreakType.NECK)
        app.recordBreakDone(BreakType.WALK)
        assertEquals(BreakType.NECK.defaultReward + BreakType.WALK.defaultReward, app.balance())
    }

    @Test
    fun streakMilestoneIsPaidOncePerStreak() = runBlocking {
        app.sittingFor(10)
        val now = Instant.now()
        app.dailyGoalSince(app.today().minusDays(2)) // the 3-break rule on all three days
        val goalTypes = BreakType.entries.take(RewardRules.DAILY_GOAL.count)
        // Two earlier work days that met the goal (every day is a work day here).
        listOf(2L, 1L).forEach { daysAgo -> goalTypes.forEach { app.logDone(it, now.minus(daysAgo, ChronoUnit.DAYS)) } }
        // Today: all but the last goal type already done (straight to the log, so unpaid).
        goalTypes.dropLast(1).forEach { app.logDone(it, now) }

        val third = app.recordBreakDone(goalTypes.last())
        assertEquals(3, third.streak.days)
        assertTrue(third.streak.todayCounts)
        assertEquals(listOf(Milestone(days = 3, bonus = 20)), third.bonuses)

        // Another break the same day: the streak is still 3, the bonus is not paid again.
        val extra = app.recordBreakDone(BreakType.WATER)
        assertEquals(3, extra.streak.days)
        assertEquals(emptyList<Milestone>(), extra.bonuses)

        val streakRows = app.ledger().filter { it.reason == SparkReason.STREAK }
        assertEquals(1, streakRows.size)
        assertEquals(20, streakRows.single().amount)
        assertEquals(goalTypes.last().defaultReward + BreakType.WATER.defaultReward + 20, app.balance())
    }

    @Test
    fun belowTheGoalTodayShowsProgressAndPaysNoBonus() = runBlocking {
        app.sittingFor(10)
        val result = app.recordBreakDone(BreakType.NECK)
        assertEquals(1, result.streak.todayDone)
        assertEquals(RewardRules.DAILY_GOAL.count, result.streak.goal)
        assertEquals(0, result.streak.days)
        assertTrue(result.bonuses.isEmpty())
    }

    /** A new install's first Done adopts the 3-break rule from today, so repeats of one type count. */
    @Test
    fun threeOfOneTypeMeetTheGoal() = runBlocking {
        app.sittingFor(10)
        repeat(2) { app.recordBreakDone(BreakType.WATER) }
        val third = app.recordBreakDone(BreakType.WATER)
        assertEquals(3, third.streak.todayDone)
        assertTrue(third.streak.todayCounts)
        assertEquals(1, third.streak.days)
        assertEquals(BreakType.WATER.defaultReward, app.balance()) // the type still pays once a day
    }

    /** The rule change is recorded from the first day it's seen, so earlier days keep the old rule. */
    @Test
    fun earlierDaysKeepTheOldRule() = runBlocking {
        app.sittingFor(10)
        val now = Instant.now()
        // Yesterday: 3 different types, which the old rule (4 different types) didn't count.
        BreakType.entries.take(3).forEach { app.logDone(it, now.minus(1, ChronoUnit.DAYS)) }
        repeat(2) { app.logDone(BreakType.WATER, now) }

        val result = app.recordBreakDone(BreakType.WATER)
        assertEquals(1, result.streak.days) // today only
        assertEquals(app.today(), result.streak.startDate)
        assertEquals(DailyGoal.Breaks(3), app.settings.dailyGoalHistory.first().goalOn(app.today()))
    }
}
