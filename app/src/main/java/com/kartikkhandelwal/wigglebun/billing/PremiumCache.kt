package com.kartikkhandelwal.wigglebun.billing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import com.kartikkhandelwal.wigglebun.data.prefs.DeviceLocal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** The last entitlement RevenueCat reported, and when we saved it (real wall-clock time). */
data class CachedPremium(val state: PremiumState, val savedAt: Instant)

/**
 * Mirrors the last known entitlement to DataStore, so a cold start without network shows the
 * cached state instead of flashing "locked" (CLAUDE.md §5.5). Kept in the device-only file: it is
 * not backed up, so a restored phone asks RevenueCat instead of trusting another phone's answer.
 */
@Singleton
class PremiumCache @Inject constructor(@DeviceLocal private val store: DataStore<Preferences>) {

    val cached: Flow<CachedPremium?> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map(::read)

    /** Only real answers are cached; Unknown and Unavailable say nothing about the user. */
    suspend fun save(state: PremiumState, savedAt: Instant) {
        if (state !is PremiumState.Entitled && state !is PremiumState.NotEntitled) return
        store.edit { p ->
            p[SAVED_AT] = savedAt.toEpochMilli()
            p[ENTITLED] = state is PremiumState.Entitled
            if (state is PremiumState.Entitled) {
                p[IS_TRIAL] = state.isTrial
                p[WILL_RENEW] = state.willRenew
                p[IS_LIFETIME] = state.isLifetime
                val expiresAt = state.expiresAt
                if (expiresAt != null) p[EXPIRES_AT] = expiresAt.toEpochMilli() else p.remove(EXPIRES_AT)
            } else {
                listOf(IS_TRIAL, WILL_RENEW, IS_LIFETIME).forEach { p.remove(it) }
                p.remove(EXPIRES_AT)
            }
        }
    }

    private fun read(p: Preferences): CachedPremium? {
        val savedAt = p[SAVED_AT] ?: return null
        val entitled = p[ENTITLED] ?: return null
        val state = if (entitled) {
            val isLifetime = p[IS_LIFETIME] ?: false
            val expiresAt = p[EXPIRES_AT]?.let(Instant::ofEpochMilli)
            // A subscription always has an end date; without one the entry is damaged, so don't trust it.
            if (!isLifetime && expiresAt == null) return null
            PremiumState.Entitled(
                isTrial = p[IS_TRIAL] ?: false,
                expiresAt = expiresAt,
                willRenew = p[WILL_RENEW] ?: false,
                isLifetime = isLifetime,
            )
        } else {
            PremiumState.NotEntitled
        }
        return CachedPremium(state, Instant.ofEpochMilli(savedAt))
    }

    private companion object {
        val SAVED_AT = longPreferencesKey("premium_saved_at")
        val ENTITLED = booleanPreferencesKey("premium_entitled")
        val IS_TRIAL = booleanPreferencesKey("premium_is_trial")
        val WILL_RENEW = booleanPreferencesKey("premium_will_renew")
        val IS_LIFETIME = booleanPreferencesKey("premium_is_lifetime")
        val EXPIRES_AT = longPreferencesKey("premium_expires_at")
    }
}
