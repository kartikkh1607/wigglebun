package com.kartikkhandelwal.wigglebun.ui.settings

import com.kartikkhandelwal.wigglebun.billing.AppAccess
import com.kartikkhandelwal.wigglebun.billing.FakePremiumRepository
import com.kartikkhandelwal.wigglebun.billing.PremiumState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The chip on Settings' "Bun & you" card (CLAUDE.md §6.7). */
class PremiumBadgeTest {

    private val trial = FakePremiumRepository.SAMPLE_TRIAL

    @Test
    fun aTrialAndAPlanSaySo() {
        assertEquals(PremiumBadge.Trial, premiumBadge(trial, AppAccess.Open))
        assertEquals(PremiumBadge.Premium, premiumBadge(trial.copy(isTrial = false), AppAccess.Open))
    }

    @Test
    fun lockedOffersTheWayIn() {
        assertEquals(PremiumBadge.Unlock, premiumBadge(PremiumState.NotEntitled, AppAccess.Locked))
        assertEquals(PremiumBadge.Unlock, premiumBadge(PremiumState.Unavailable, AppAccess.Locked))
    }

    /** A build that doesn't lock (closed test, debug without a key) shows nothing to someone who hasn't bought. */
    @Test
    fun nothingToSayShowsNoChip() {
        assertNull(premiumBadge(PremiumState.NotEntitled, AppAccess.Open))
        assertNull(premiumBadge(PremiumState.Unavailable, AppAccess.Open))
        assertNull(premiumBadge(PremiumState.Unknown, AppAccess.Checking))
    }
}
