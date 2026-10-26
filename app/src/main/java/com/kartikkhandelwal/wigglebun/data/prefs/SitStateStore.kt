package com.kartikkhandelwal.wigglebun.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.kartikkhandelwal.wigglebun.domain.sitting.SitState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** The sit timer, persisted so it survives process death and reboots. */
@Singleton
class SitStateStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val state: Flow<SitState> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toSitState() }
        .distinctUntilChanged()

    suspend fun current(): SitState = state.first()

    /** Atomically read-modify-write the state; returns the new state. */
    suspend fun update(transform: (SitState) -> SitState): SitState {
        var result = SitState()
        dataStore.edit { prefs ->
            result = transform(prefs.toSitState())
            prefs.writeSitState(result)
        }
        return result
    }
}
