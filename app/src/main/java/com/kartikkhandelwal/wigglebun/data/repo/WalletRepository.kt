package com.kartikkhandelwal.wigglebun.data.repo

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The sparks balance (CLAUDE.md §5.4). Read-only for now: earning sparks for completed breaks
 * and streaks arrives in M6, spending in M8.
 */
@Singleton
class WalletRepository @Inject constructor(
    dataStore: DataStore<Preferences>,
) {
    val balance: Flow<Int> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[SPARKS] ?: 0 }
        .distinctUntilChanged()

    private companion object {
        val SPARKS = intPreferencesKey("sparks")
    }
}
