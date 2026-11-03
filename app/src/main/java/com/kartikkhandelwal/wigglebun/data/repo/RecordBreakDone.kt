package com.kartikkhandelwal.wigglebun.data.repo

import com.kartikkhandelwal.wigglebun.data.prefs.SettingsRepository
import com.kartikkhandelwal.wigglebun.data.time.AppClock
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.SparkReason
import com.kartikkhandelwal.wigglebun.domain.rewards.Milestone
import com.kartikkhandelwal.wigglebun.domain.rewards.RewardRules
import com.kartikkhandelwal.wigglebun.domain.rewards.Streak
import com.kartikkhandelwal.wigglebun.domain.rewards.StreakCalculator
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** What a completed break earned, for the celebration. */
data class BreakDoneResult(
    val type: BreakType,
    /** 0 when this type was already completed today (CLAUDE.md §5.4). */
    val sparksEarned: Int,
    /** Milestones and 30-day repeat bonuses paid by this break (usually none). */
    val bonuses: List<Milestone>,
    val streak: Streak,
)

/**
 * The one way a break gets completed: the break session, and the notification's Done.
 * Resets the sit timer and logs the break, pays the break reward and any streak milestone.
 * Every payment is keyed in the ledger, so racing or repeated calls never pay twice.
 */
@Singleton
class RecordBreakDone @Inject constructor(
    private val sitRepository: SitRepository,
    private val breakRepository: BreakRepository,
    private val wallet: WalletRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: AppClock,
) {
    suspend operator fun invoke(type: BreakType): BreakDoneResult {
        sitRepository.done(type)

        val at = clock.instant()
        val zone = clock.zone
        // On a 22:00–06:00 shift, a break at 00:10 is still the evening's day: it doesn't pay again.
        val days = settingsRepository.breakDays()
        val today = days.dayOf(at)
        val earned = wallet.award(RewardRules.breakKey(today, type), type.defaultReward, SparkReason.BREAK, at)

        val history = settingsRepository.workDayHistory.first()
        // Also adopts the current rule, in case this process started for a notification's Done
        // and the app-start adoption hasn't run yet.
        val goals = settingsRepository.adoptDailyGoal()
        val streak = StreakCalculator.compute(
            done = breakRepository.doneBreaks(),
            zone = zone,
            today = today,
            // A date a change of hours skipped has no time, so it can't be a missed work day.
            isWorkDay = { days.exists(it) && history.isWorkDay(it) },
            goalOn = goals::goalOn,
            dayOf = days::dayOf,
        )
        val start = streak.startDate
        val bonuses = if (start == null) {
            emptyList()
        } else {
            RewardRules.bonusesFor(streak.days, start)
                .filter { bonus -> wallet.award(bonus.key, bonus.milestone.bonus, SparkReason.STREAK, at) > 0 }
                .map { it.milestone }
        }
        return BreakDoneResult(type, earned, bonuses, streak)
    }
}
