package com.kartikkhandelwal.wigglebun.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.kartikkhandelwal.wigglebun.domain.sitting.SittingSettings
import com.kartikkhandelwal.wigglebun.domain.sitting.WorkSchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Work hours, break interval and pet name (CLAUDE.md §5.1). */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val prefs: Flow<Preferences> = dataStore.data.catch { e ->
        // A corrupt/unreadable file must not crash the app: fall back to defaults.
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val settings: Flow<SittingSettings> = prefs.map { it.toSittingSettings() }.distinctUntilChanged()

    /** Null until the user names the pet in onboarding (M9). */
    val petName: Flow<String?> = prefs.map { it[PrefsKeys.PetName] }.distinctUntilChanged()

    suspend fun current(): SittingSettings = settings.first()

    suspend fun setSchedule(schedule: WorkSchedule) {
        dataStore.edit { it.writeSchedule(schedule) }
    }

    suspend fun setInterval(minutes: Int) {
        dataStore.edit {
            it[PrefsKeys.IntervalMinutes] =
                minutes.coerceIn(SittingSettings.MIN_INTERVAL_MINUTES, SittingSettings.MAX_INTERVAL_MINUTES)
        }
    }
}
