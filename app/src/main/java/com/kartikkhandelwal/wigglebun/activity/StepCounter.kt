package com.kartikkhandelwal.wigglebun.activity

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.getSystemService
import com.kartikkhandelwal.wigglebun.data.prefs.StepBaseline
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

/** One step-counter value and when we received it (real wall-clock time). */
data class StepReading(val total: Long, val at: Instant)

/**
 * TYPE_STEP_COUNTER, best effort only. Activity Recognition is the primary movement signal.
 *
 * The docs say the "application needs to stay registered for this sensor because step counter
 * does not count steps if it is not activated" (some phones, e.g. the test Motorola, really do
 * sit at 0 until someone listens). We have no foreground service (CLAUDE.md §2.7), so:
 * - [startCounting] keeps a batched, non-wakeup listener registered during work hours while our
 *   process is alive. MovementTracker.sync() calls it on app start, every alarm (including the
 *   work-hours start alarm), boot/app update and each transition batch.
 * - [stopCounting] runs at work-hours end, when movement detection is switched off, or when the
 *   permission is gone.
 * - When Android kills the process in the background, the listener dies with it and steps are
 *   not counted until the next wake-up re-registers it.
 * - [readTotal] returns the live listener's latest value, or does a short one-shot read.
 *
 * Needs ACTIVITY_RECOGNITION on API 29+. Not every device has the sensor.
 */
@Singleton
class StepCounter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissions: MovementPermissions,
) {
    private val sensorManager: SensorManager? = context.getSystemService()
    private val sensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private val _lastReading = MutableStateFlow<StepReading?>(null)

    /** Latest value from any listener (persistent or one-shot); shown in the Pet lab. */
    val lastReading: StateFlow<StepReading?> = _lastReading.asStateFlow()

    private val _isCounting = MutableStateFlow(false)
    val isCounting: StateFlow<Boolean> = _isCounting.asStateFlow()

    private var countingListener: SensorEventListener? = null

    val hasSensor: Boolean get() = sensor != null

    /** Keep the sensor activated (see class docs). Returns whether it is registered. */
    @Synchronized
    fun startCounting(): Boolean {
        if (countingListener != null) return true
        val manager = sensorManager ?: return false
        val stepSensor = sensor ?: return false
        if (!permissions.granted()) return false
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) = record(event)
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        // Batching lets the hardware hold events while the CPU sleeps; a non-wakeup sensor
        // delivers them when the device next wakes, so this costs almost no battery.
        val ok = manager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL, MAX_BATCH_LATENCY_US)
        if (ok) countingListener = listener
        _isCounting.value = ok
        return ok
    }

    @Synchronized
    fun stopCounting() {
        countingListener?.let { sensorManager?.unregisterListener(it) }
        countingListener = null
        _isCounting.value = false
    }

    /** Current total, or null if unavailable (no sensor, no permission, no reading in time). */
    suspend fun readTotal(): Long? {
        if (countingListener != null) _lastReading.value?.let { return it.total }
        val manager = sensorManager ?: return null
        val stepSensor = sensor ?: return null
        if (!permissions.granted()) return null
        return withTimeoutOrNull(READ_TIMEOUT_MILLIS.milliseconds) {
            suspendCancellableCoroutine { cont ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        manager.unregisterListener(this)
                        record(event)
                        if (cont.isActive) cont.resume(event.values[0].toLong())
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }
                if (!manager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)) {
                    cont.resume(null)
                }
                cont.invokeOnCancellation { manager.unregisterListener(listener) }
            }
        }
    }

    private fun record(event: SensorEvent) {
        _lastReading.value = StepReading(event.values[0].toLong(), Instant.now())
    }

    /** Settings.Global.BOOT_COUNT changes on every reboot, which is when the counter resets. */
    fun bootCount(): Int = context.bootCount()

    fun baselineNow(total: Long) = StepBaseline(total, bootCount())

    companion object {
        private const val READ_TIMEOUT_MILLIS = 2_000L
        private const val MAX_BATCH_LATENCY_US = 5 * 60 * 1_000_000 // 5 min

        /**
         * Steps since [baseline]. After a reboot (or if the counter went back, e.g. it was
         * deactivated and restarted at 0) every step on the new count happened after the timer
         * started: count all of them.
         */
        fun stepsSince(baseline: StepBaseline, total: Long, bootCount: Int): Long =
            if (bootCount != baseline.bootCount || total < baseline.steps) total else total - baseline.steps
    }
}
