package com.kartikkhandelwal.wigglebun.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.kartikkhandelwal.wigglebun.data.time.AppClock
import com.kartikkhandelwal.wigglebun.domain.model.PetId
import com.kartikkhandelwal.wigglebun.domain.sitting.SittingSettings
import com.kartikkhandelwal.wigglebun.domain.sitting.WorkDayHistory
import com.kartikkhandelwal.wigglebun.domain.sitting.WorkSchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Work hours, break interval, pet name and the active pet (CLAUDE.md §5.1). Backed up. */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val clock: AppClock,
) {
    private val prefs: Flow<Preferences> = dataStore.data.catch { e ->
        // A corrupt/unreadable file must not crash the app: fall back to defaults.
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val settings: Flow<SittingSettings> = prefs.map { it.toSittingSettings() }.distinctUntilChanged()

    /** Null until the user names the pet in onboarding (M9). */
    val petName: Flow<String?> = prefs.map { it[PrefsKeys.PetName] }.distinctUntilChanged()

    /** The stored active pet; PetRepository checks it is owned. Unknown or missing = the starter pet. */
    val activePetId: Flow<PetId> =
        prefs.map { PetId.fromKey(it[PrefsKeys.ActivePet]) ?: PetId.Starter }.distinctUntilChanged()

    /** Work days over time, so the streak judges past days by the days in force back then. */
    val workDayHistory: Flow<WorkDayHistory> = prefs.map { it.toWorkDayHistory() }.distinctUntilChanged()

    suspend fun current(): SittingSettings = settings.first()

    /** Changing the work days takes effect today and is recorded in [workDayHistory]. */
    suspend fun setSchedule(schedule: WorkSchedule) {
        val today = clock.instant().atZone(clock.zone).toLocalDate()
        dataStore.edit {
            val history = it.toWorkDayHistory().withChange(schedule.days, today)
            it.writeSchedule(schedule)
            it.writeWorkDayHistory(history)
        }
    }

    suspend fun setActivePet(pet: PetId) {
        dataStore.edit { it[PrefsKeys.ActivePet] = pet.key }
    }

    suspend fun setInterval(minutes: Int) {
        dataStore.edit {
            it[PrefsKeys.IntervalMinutes] =
                minutes.coerceIn(SittingSettings.MIN_INTERVAL_MINUTES, SittingSettings.MAX_INTERVAL_MINUTES)
        }
    }
}
