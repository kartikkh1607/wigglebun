package com.kartikkhandelwal.wigglebun.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Moves the widget's "32 min" along during work hours (CLAUDE.md §8).
 *
 * Runs only in work hours: SitRepository.checkReminder() schedules it when a work session starts
 * and cancels it when the session ends (the reminder alarm wakes us at both boundaries, and also
 * redraws the widget then). Outside work hours the pet sleeps and the widget shows no timer.
 *
 * 15 minutes is WorkManager's minimum period, and the run is inexact: Doze and app standby
 * batch it, so it can land later.
 */
class WidgetRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val state = loadWidgetState(applicationContext)
        if (state.isSleeping) {
            // Outside work hours: the widget was just placed in the evening, or the end-of-work
            // alarm is running late (Doze). Stop until the next session's start schedules it again.
            cancel(applicationContext)
        } else {
            WigglebunWidget().updateAll(applicationContext)
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "widget_refresh"
        private const val PERIOD_MINUTES = 15L

        /** KEEP: every alarm in a session re-schedules, and that must not push the next run back. */
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<WidgetRefreshWorker>(PERIOD_MINUTES, TimeUnit.MINUTES).build(),
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
