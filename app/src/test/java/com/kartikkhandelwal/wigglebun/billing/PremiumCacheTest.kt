package com.kartikkhandelwal.wigglebun.billing

import com.kartikkhandelwal.wigglebun.data.prefs.inMemoryPreferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/** The offline cache: what is stored, and what a cold start may conclude from it. */
class PremiumCacheTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cache = PremiumCache(inMemoryPreferencesDataStore(scope))
    private val saved = Instant.parse("2026-11-11T09:00:00Z")
    private val trial = PremiumState.Entitled(isTrial = true, expiresAt = saved.plusSeconds(7 * DAY), willRenew = true, isLifetime = false)
    private val lifetime = PremiumState.Entitled(isTrial = false, expiresAt = null, willRenew = false, isLifetime = true)

    @After
    fun tearDown() = scope.cancel()

    // --- storage ---

    @Test
    fun emptyCacheIsNull() = runBlocking {
        assertNull(cache.cached.first())
    }

    @Test
    fun entitledRoundTrips() = runBlocking {
        cache.save(trial, saved)
        assertEquals(CachedPremium(trial, saved), cache.cached.first())
    }

    @Test
    fun lifetimeRoundTripsWithoutExpiry() = runBlocking {
        cache.save(lifetime, saved)
        assertEquals(CachedPremium(lifetime, saved), cache.cached.first())
    }

    @Test
    fun notEntitledReplacesEntitled() = runBlocking {
        cache.save(trial, saved)
        cache.save(PremiumState.NotEntitled, saved.plusSeconds(60))
        assertEquals(CachedPremium(PremiumState.NotEntitled, saved.plusSeconds(60)), cache.cached.first())
    }

    @Test
    fun unknownAndUnavailableAreNotCached() = runBlocking {
        cache.save(trial, saved)
        cache.save(PremiumState.Unknown, saved.plusSeconds(60))
        cache.save(PremiumState.Unavailable, saved.plusSeconds(120))
        assertEquals(CachedPremium(trial, saved), cache.cached.first())
    }

    // --- cold-start rules ---

    @Test
    fun nothingCachedIsUnknown() {
        assertEquals(PremiumState.Unknown, premiumStateFromCache(null, saved))
    }

    @Test
    fun subscriptionCountsUntilItExpires() {
        val cached = CachedPremium(trial, saved)
        assertEquals(trial, premiumStateFromCache(cached, saved.plusSeconds(3 * DAY)))
        assertEquals(PremiumState.NotEntitled, premiumStateFromCache(cached, trial.expiresAt!!))
        assertEquals(PremiumState.NotEntitled, premiumStateFromCache(cached, saved.plusSeconds(30 * DAY)))
    }

    @Test
    fun clockWoundBackCannotStretchAnExpiredTrial() {
        // Saved after the trial had already ended (e.g. the last answer before going offline was
        // a cancelled trial on its last second), then the phone's clock was moved back.
        val endsSoon = trial.copy(expiresAt = saved.plusSeconds(60))
        val cached = CachedPremium(endsSoon, saved.plusSeconds(120))
        assertEquals(PremiumState.NotEntitled, premiumStateFromCache(cached, saved))
    }

    @Test
    fun lifetimeNeverExpires() {
        assertEquals(lifetime, premiumStateFromCache(CachedPremium(lifetime, saved), saved.plusSeconds(3650 * DAY)))
    }

    @Test
    fun cachedNotEntitledStaysNotEntitled() {
        assertEquals(PremiumState.NotEntitled, premiumStateFromCache(CachedPremium(PremiumState.NotEntitled, saved), saved))
    }

    private companion object {
        const val DAY = 24 * 60 * 60L
    }
}
