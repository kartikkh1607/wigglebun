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
 * 15 minutes is WorkManager's minimum period, and the run is inexact: Doze and app standby
 * batch it, so it can land later. Work-hour start/end and reminders are covered sooner by the
 * reminder alarm, which redraws the widget too. Outside work hours the pet just sleeps, so
 * the run does nothing.
 */
class WidgetRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val state = loadWidgetState(applicationContext)
        if (!state.isSleeping) WigglebunWidget().updateAll(applicationContext)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "widget_refresh"
        private const val PERIOD_MINUTES = 15L

        /** KEEP: re-scheduling on every app start must not push the next run back. */
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
