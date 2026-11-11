package com.kartikkhandelwal.wigglebun.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kartikkhandelwal.wigglebun.billing.PremiumRepository
import com.kartikkhandelwal.wigglebun.billing.PremiumState
import com.kartikkhandelwal.wigglebun.billing.TrialReminderPlan
import com.kartikkhandelwal.wigglebun.billing.trialEndToRemindAbout
import com.kartikkhandelwal.wigglebun.billing.trialReminderPlan
import com.kartikkhandelwal.wigglebun.data.time.AppClock
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface TrialReminderEntryPoint {
    fun premium(): PremiumRepository

    fun notifier(): ReminderNotifier

    fun clock(): AppClock
}

/**
 * Keeps the trial-end reminder in step with premium (CLAUDE.md §5.5): scheduled when a trial
 * starts, dropped when it is cancelled or becomes a paid plan. WigglebunApp calls [sync] for
 * every premium answer.
 */
@Singleton
class TrialReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: AppClock,
) {
    fun sync(state: PremiumState) {
        val now = clock.instant()
        when (val plan = trialReminderPlan(state, now)) {
            is TrialReminderPlan.At -> TrialReminderWorker.schedule(context, Duration.between(now, plan.time))
            TrialReminderPlan.None -> TrialReminderWorker.cancel(context)
            TrialReminderPlan.Unchanged -> Unit
        }
    }
}

/**
 * Posts "Your free trial ends on …". WorkManager, not an alarm: the time is days away and only
 * needs to be about right, the work survives a reboot, and exact alarms are not allowed (§2.7).
 * Doze can make it late by a while.
 */
class TrialReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = EntryPointAccessors.fromApplication(applicationContext, TrialReminderEntryPoint::class.java)
        // The process may have been started just for this: wait for the cached answer to load.
        val state = withTimeoutOrNull(PREMIUM_WAIT_MILLIS) { app.premium().premium.first { it != PremiumState.Unknown } }
        // Checked again here: the trial may have been cancelled since this was scheduled.
        val ends = state?.let { trialEndToRemindAbout(it, app.clock().instant()) }
        if (ends != null) app.notifier().showTrialEnding(ends)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "trial_end_reminder"
        private const val PREMIUM_WAIT_MILLIS = 10_000L

        /** REPLACE: every premium answer re-schedules with the delay that is left, so the time stays put. */
        fun schedule(context: Context, delay: Duration) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<TrialReminderWorker>()
                    .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                    .build(),
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
