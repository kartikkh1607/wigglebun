package com.kartikkhandelwal.wigglebun

import android.app.Application
import com.kartikkhandelwal.wigglebun.data.ApplicationScope
import com.kartikkhandelwal.wigglebun.data.repo.SitRepository
import com.kartikkhandelwal.wigglebun.notify.ReminderNotifier
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
    }
}
