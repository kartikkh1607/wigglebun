package com.kartikkhandelwal.wigglebun.data.repo

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Instant

/** Who sees onboarding (CLAUDE.md §6.2): new installs until they finish it, and nobody else. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class)
class OnboardingGateTest {

    private val app = TestDataLayer(ApplicationProvider.getApplicationContext())

    @After
    fun tearDown() = app.close()

    private fun onboardingDone() = runBlocking { app.settings.onboardingDone.first() }

    @Test
    fun undecidedUntilTheAppHasStarted() {
        assertNull(onboardingDone())
    }

    @Test
    fun aNewInstallSeesOnboarding() = runBlocking {
        app.sitRepository.onAppStart()
        assertEquals(false, onboardingDone())
        assertNotNull(app.store.current().sitStartedAt) // the timer still starts, as before
    }

    /** Installed over an older build (like the test phone): its timer is already saved. */
    @Test
    fun anExistingInstallNeverSeesOnboarding() = runBlocking {
        app.store.update { it.copy(sitStartedAt = Instant.parse("2026-10-30T10:00:00Z")) }
        app.sitRepository.onAppStart()
        assertEquals(true, onboardingDone())
    }

    /** The first start saves the timer, so a later start must not read that as an existing install. */
    @Test
    fun aNewInstallKilledHalfwayStillSeesOnboarding() = runBlocking {
        app.sitRepository.onAppStart()
        app.sitRepository.onAppStart() // the next process
        assertEquals(false, onboardingDone())
    }

    @Test
    fun finishingOnboardingLasts() = runBlocking {
        app.sitRepository.onAppStart()
        app.settings.finishOnboarding()
        app.sitRepository.onAppStart()
        assertEquals(true, onboardingDone())
    }
}
