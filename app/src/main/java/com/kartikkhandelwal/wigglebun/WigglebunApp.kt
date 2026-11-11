package com.kartikkhandelwal.wigglebun

import android.app.Application
import com.kartikkhandelwal.wigglebun.billing.AccessGate
import com.kartikkhandelwal.wigglebun.billing.PremiumRepository
import com.kartikkhandelwal.wigglebun.data.ApplicationScope
import com.kartikkhandelwal.wigglebun.data.repo.SitRepository
import com.kartikkhandelwal.wigglebun.feedback.FeelPlayer
import com.kartikkhandelwal.wigglebun.notify.ReminderNotifier
import com.kartikkhandelwal.wigglebun.notify.TrialReminderScheduler
import com.kartikkhandelwal.wigglebun.widget.WidgetRefresher
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class WigglebunApp : Application() {

    @Inject lateinit var notifier: ReminderNotifier

    @Inject lateinit var sitRepository: SitRepository

    @Inject lateinit var feel: FeelPlayer

    @Inject @ApplicationScope
    lateinit var appScope: CoroutineScope

    @Inject lateinit var premium: PremiumRepository

    @Inject lateinit var access: AccessGate

    @Inject lateinit var widget: WidgetRefresher

    @Inject lateinit var trialReminder: TrialReminderScheduler

    override fun onCreate() {
        super.onCreate()
        // Configures RevenueCat once per process and starts following the premium entitlement.
        premium.start()
        notifier.createChannel()
        feel.preload()
        // Starts the timer on first run and re-arms the alarm (it is lost on force-stop). Also
        // re-syncs the widget's work-hours refresh, which a force-stop or app update may have lost.
        appScope.launch { sitRepository.onAppStart() }
        // The widget shows the pet asleep while the app is locked: redraw it when that changes
        // (a trial started, a subscription ended).
        appScope.launch { access.access.collect { widget.requestUpdate() } }
        // "Your free trial ends on …": scheduled when a trial starts, dropped if it is cancelled.
        appScope.launch { premium.premium.collect { trialReminder.sync(it) } }
    }
}
