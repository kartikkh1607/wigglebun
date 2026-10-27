package com.kartikkhandelwal.wigglebun

import android.app.Application
import com.kartikkhandelwal.wigglebun.data.ApplicationScope
import com.kartikkhandelwal.wigglebun.data.repo.SitRepository
import com.kartikkhandelwal.wigglebun.notify.ReminderNotifier
import com.kartikkhandelwal.wigglebun.widget.GlanceWidgetRefresher
import com.kartikkhandelwal.wigglebun.widget.WidgetRefreshWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class WigglebunApp : Application() {

    @Inject lateinit var notifier: ReminderNotifier

    @Inject lateinit var sitRepository: SitRepository

    @Inject @ApplicationScope
    lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        notifier.createChannel()
        // Starts the timer on first run and re-arms the alarm (it is lost on force-stop).
        appScope.launch { sitRepository.onAppStart() }
        // The widget receiver schedules the refresh when the first widget is added; this covers
        // widgets placed before an app update or a WorkManager reset (e.g. after a force-stop).
        appScope.launch {
            if (GlanceWidgetRefresher.hasWidgets(this@WigglebunApp)) WidgetRefreshWorker.schedule(this@WigglebunApp)
        }
    }
}
