package com.kartikkhandelwal.wigglebun.data.prefs

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A registration lasts until reboot, app update or force-stop. */
class RegistrationTest {
    private val registration = Registration(bootCount = 7, versionCode = 12)

    @Test
    fun sameBootAndVersionMayStillBeLive() = assertTrue(registration.mayStillBeLive(currentBoot = 7, currentVersion = 12))

    @Test
    fun aRebootDropsIt() = assertFalse(registration.mayStillBeLive(currentBoot = 8, currentVersion = 12))

    @Test
    fun anAppUpdateDropsIt() = assertFalse(registration.mayStillBeLive(currentBoot = 7, currentVersion = 13))

    @Test
    fun anUnknownBootCountIsNeverTrusted() =
        assertFalse(Registration(bootCount = -1, versionCode = 12).mayStillBeLive(currentBoot = -1, currentVersion = 12))
}
