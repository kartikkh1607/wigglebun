package com.kartikkhandelwal.wigglebun.billing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Who gets in (CLAUDE.md §5.5). */
class AppAccessTest {

    private val trial = FakePremiumRepository.SAMPLE_TRIAL

    @Test
    fun anEntitlementOpensAndNoneLocks() {
        assertEquals(AppAccess.Open, appAccess(trial, locks = true, debug = false))
        assertEquals(AppAccess.Locked, appAccess(PremiumState.NotEntitled, locks = true, debug = false))
    }

    @Test
    fun noAnswerYetIsNeitherOpenNorLocked() {
        assertEquals(AppAccess.Checking, appAccess(PremiumState.Unknown, locks = true, debug = false))
    }

    /** The closed test: Play's test subscriptions lapse within hours, so nothing locks there. */
    @Test
    fun aBuildThatDoesNotLockIsAlwaysOpen() {
        listOf(PremiumState.Unknown, PremiumState.NotEntitled, PremiumState.Unavailable, trial).forEach {
            assertEquals(AppAccess.Open, appAccess(it, locks = false, debug = false))
        }
    }

    /** No RevenueCat key: fine while developing, never a free pass in a build that ships. */
    @Test
    fun withoutBillingOnlyADebugBuildIsOpen() {
        assertEquals(AppAccess.Open, appAccess(PremiumState.Unavailable, locks = true, debug = true))
        assertEquals(AppAccess.Locked, appAccess(PremiumState.Unavailable, locks = true, debug = false))
    }

    /** Debug with a Test Store key locks like release, so the locked Home can be tried. */
    @Test
    fun aDebugBuildWithBillingLocksLikeRelease() {
        assertEquals(AppAccess.Locked, appAccess(PremiumState.NotEntitled, locks = true, debug = true))
    }

    @Test
    fun backgroundWorkCountsOnlyOpenAsOpen() = runBlocking {
        val state = MutableStateFlow(AppAccess.Checking)
        val gate = object : AccessGate {
            override val access = state
        }
        assertFalse(gate.isOpen())
        state.value = AppAccess.Locked
        assertFalse(gate.isOpen())
        state.value = AppAccess.Open
        assertTrue(gate.isOpen())
        assertTrue(AccessGate.AlwaysOpen.isOpen())
    }
}
