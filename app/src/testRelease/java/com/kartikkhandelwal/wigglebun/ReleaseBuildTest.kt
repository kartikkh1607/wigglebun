package com.kartikkhandelwal.wigglebun

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the production build. CI runs it (testReleaseUnitTest), so a release build that would
 * let people close the paywall and keep using the app can't get through a PR.
 */
class ReleaseBuildTest {

    /** Only the closedTest build may turn this off (CLAUDE.md §5.5). */
    @Test
    fun closingThePaywallLocksTheApp() {
        assertTrue("release must lock when the paywall is closed", BuildConfig.PAYWALL_LOCKS)
    }

    /** RevenueCat's Test Store key is for debug only; the SDK crashes a release build that uses it. */
    @Test
    fun usesNoTestStoreKey() {
        assertFalse(BuildConfig.REVENUECAT_API_KEY.startsWith("test_"))
    }

    @Test
    fun isNotADebugOrClosedTestBuild() {
        assertFalse(BuildConfig.DEBUG)
        assertEquals("release", BuildConfig.BUILD_TYPE)
    }
}
