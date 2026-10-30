package com.kartikkhandelwal.wigglebun.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kartikkhandelwal.wigglebun.domain.model.MovementActivity
import com.kartikkhandelwal.wigglebun.domain.model.MovementTransition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * When Activity Recognition was last registered. A registration lasts until reboot, app update or
 * force-stop, so it can only still be live in the same boot and the same app version.
 */
data class Registration(val bootCount: Int, val versionCode: Int) {
    /** False when either is unknown (boot count -1): then we can't tell, so assume it's gone. */
    fun mayStillBeLive(currentBoot: Int, currentVersion: Int): Boolean =
        bootCount >= 0 && bootCount == currentBoot && versionCode == currentVersion
}

/** Step-counter baseline: the sensor value when the timer started, and which boot it came from. */
data class StepBaseline(val steps: Long, val bootCount: Int)

data class MovementInfo(
    val enabled: Boolean = true,
    val registered: Boolean = false,
    /** Set with [registered] since this field exists; null for a registration made by an older version. */
    val registration: Registration? = null,
    val baseline: StepBaseline? = null,
    /** Newest first, at most [MovementStore.LOG_SIZE]. Times are real wall-clock time. */
    val log: List<MovementTransition> = emptyList(),
) {
    /** The activity of the newest ENTER that hasn't been exited since. */
    val currentActivity: MovementActivity?
        get() = log.firstOrNull()?.let { latest -> if (latest.entered) latest.activity else null }
}

/**
 * Movement detection. The on/off switch is a user setting (backed up with the other settings);
 * everything else is bookkeeping for this phone's sensors (boot count, elapsed-realtime
 * watermark, step baseline, movement log) and lives in [deviceStore], which is excluded from
 * backup: restored on another phone it would be wrong, and the log is activity data.
 */
@Singleton
class MovementStore @Inject constructor(
    private val settingsStore: DataStore<Preferences>,
    @DeviceLocal private val dataStore: DataStore<Preferences>,
) {
    val info: Flow<MovementInfo> = combine(settingsStore.data.orEmpty(), dataStore.data.orEmpty()) { settings, device ->
        device.toMovementInfo().copy(enabled = settings[Keys.Enabled] ?: true)
    }

    suspend fun current(): MovementInfo = info.first()

    suspend fun setEnabled(enabled: Boolean) = settingsStore.edit { it[Keys.Enabled] = enabled }

    /** Records a successful registration ([registration]) or that there is none (null). */
    suspend fun setRegistration(registration: Registration?) = dataStore.edit {
        it[Keys.Registered] = registration != null
        if (registration == null) {
            it.remove(Keys.RegisteredBoot)
            it.remove(Keys.RegisteredVersion)
        } else {
            it[Keys.RegisteredBoot] = registration.bootCount
            it[Keys.RegisteredVersion] = registration.versionCode
        }
    }

    suspend fun setBaseline(baseline: StepBaseline?) = dataStore.edit {
        if (baseline == null) {
            it.remove(Keys.BaselineSteps)
            it.remove(Keys.BaselineBoot)
        } else {
            it[Keys.BaselineSteps] = baseline.steps
            it[Keys.BaselineBoot] = baseline.bootCount
        }
    }

    /**
     * Keeps only events newer than the stored watermark (see [acceptNewerThanWatermark]), logs
     * them and moves the watermark, all in one atomic write. Returns the accepted transitions.
     */
    suspend fun acceptAndLog(events: List<ReceivedTransition>, bootCount: Int): List<MovementTransition> {
        var accepted = emptyList<MovementTransition>()
        dataStore.edit { prefs ->
            val stored = prefs[Keys.WatermarkNanos]?.let { TransitionWatermark(it, prefs[Keys.WatermarkBoot] ?: -1) }
            val (newer, watermark) = acceptNewerThanWatermark(events, stored, bootCount)
            if (watermark == null) {
                prefs.remove(Keys.WatermarkNanos)
                prefs.remove(Keys.WatermarkBoot)
            } else {
                prefs[Keys.WatermarkNanos] = watermark.elapsedRealtimeNanos
                prefs[Keys.WatermarkBoot] = watermark.bootCount
            }
            accepted = newer.map { it.transition }
            if (accepted.isNotEmpty()) prefs.writeLog((accepted.reversed() + prefs.toMovementInfo().log).take(LOG_SIZE))
        }
        return accepted
    }

    /** Logs [events] (chronological) without any filtering; used for the debug walk simulation. */
    suspend fun appendLog(events: List<MovementTransition>) = dataStore.edit { prefs ->
        prefs.writeLog((events.reversed() + prefs.toMovementInfo().log).take(LOG_SIZE))
    }

    private fun MutablePreferences.writeLog(log: List<MovementTransition>) {
        this[Keys.Log] = log.joinToString("\n") { "${it.at.toEpochMilli()},${it.activity.name},${if (it.entered) 1 else 0}" }
    }

    private fun Flow<Preferences>.orEmpty() = catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

    /** [MovementInfo.enabled] comes from the settings store, see [info]. */
    private fun Preferences.toMovementInfo() = MovementInfo(
        registered = this[Keys.Registered] ?: false,
        registration = this[Keys.RegisteredBoot]?.let { boot -> this[Keys.RegisteredVersion]?.let { Registration(boot, it) } },
        baseline = this[Keys.BaselineSteps]?.let { StepBaseline(it, this[Keys.BaselineBoot] ?: -1) },
        log = this[Keys.Log].orEmpty().lineSequence().mapNotNull(::parseLogLine).toList(),
    )

    private fun parseLogLine(line: String): MovementTransition? {
        val parts = line.split(',')
        if (parts.size != 3) return null
        val activity = MovementActivity.entries.firstOrNull { it.name == parts[1] } ?: return null
        val millis = parts[0].toLongOrNull() ?: return null
        return MovementTransition(activity, entered = parts[2] == "1", at = Instant.ofEpochMilli(millis))
    }

    internal object Keys {
        val Enabled = booleanPreferencesKey("movement_enabled")
        val Registered = booleanPreferencesKey("movement_registered")
        val RegisteredBoot = intPreferencesKey("movement_registered_boot")
        val RegisteredVersion = intPreferencesKey("movement_registered_version")
        val BaselineSteps = longPreferencesKey("step_baseline")
        val BaselineBoot = intPreferencesKey("step_baseline_boot")
        val Log = stringPreferencesKey("movement_log")
        val WatermarkNanos = longPreferencesKey("transition_watermark_nanos")
        val WatermarkBoot = intPreferencesKey("transition_watermark_boot")
    }

    companion object {
        const val LOG_SIZE = 20

        /** Keys that lived in the settings file before the device-local file existed. */
        internal val MovedToDeviceStore: List<Preferences.Key<*>> = listOf(
            Keys.Registered, Keys.BaselineSteps, Keys.BaselineBoot, Keys.Log, Keys.WatermarkNanos, Keys.WatermarkBoot,
        )
    }
}
