package com.kartikkhandelwal.wigglebun.activity

import android.util.Log
import com.kartikkhandelwal.wigglebun.data.prefs.MovementStore
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "Movement"

/**
 * Keeps movement detection in line with the rules: on only while it is enabled, permitted and
 * work time. Called from SitRepository.checkReminder(), which runs on every alarm (including the
 * work-hours start/end alarm), on boot/app update (RescheduleReceiver), on app start and after
 * settings changes.
 */
@Singleton
class MovementTracker @Inject constructor(
    private val permissions: MovementPermissions,
    private val registrar: TransitionRegistrar,
    private val stepCounter: StepCounter,
    private val store: MovementStore,
) {
    /**
     * Registrations survive our process dying but not reboot, app update or force-stop, and each
     * of those starts a fresh process. So registering once per process is enough, and avoids
     * Play services replaying the current state on every check.
     */
    @Volatile
    private var registeredInThisProcess = false

    /** Returns whether Activity Recognition transitions are being tracked now. */
    suspend fun sync(isWorkTime: Boolean): Boolean {
        val info = store.current()
        val granted = permissions.granted()
        val playServices = permissions.playServicesAvailable()
        val allowed = info.enabled && isWorkTime && granted
        Log.i(
            TAG,
            "sync enabled=${info.enabled} workTime=$isWorkTime permission=$granted playServices=$playServices " +
                "registered=${info.registered} thisProcess=$registeredInThisProcess",
        )

        // Step counter: best effort, only while our process lives (see StepCounter).
        if (allowed) stepCounter.startCounting() else stepCounter.stopCounting()

        if (allowed && playServices) {
            if (registeredInThisProcess && info.registered) return true
            val ok = registrar.register()
            registeredInThisProcess = ok
            if (ok != info.registered) store.setRegistered(ok)
            return ok
        }
        if (info.registered || registeredInThisProcess) {
            registrar.unregister()
            registeredInThisProcess = false
            store.setRegistered(false)
        }
        return false
    }
}
