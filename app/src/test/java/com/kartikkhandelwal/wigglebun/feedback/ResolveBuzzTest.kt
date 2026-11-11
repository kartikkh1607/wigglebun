package com.kartikkhandelwal.wigglebun.feedback

import org.junit.Assert.assertEquals
import org.junit.Test

/** Which haptic a buzz becomes on a phone that can't (or can) play the toggle effect. */
class ResolveBuzzTest {

    @Test
    fun togglesFallBackToTheLightTickWhenUnsupported() {
        assertEquals(Buzz.Tick, resolveBuzz(Buzz.ToggleOn, toggleEffectSupported = false))
        assertEquals(Buzz.Tick, resolveBuzz(Buzz.ToggleOff, toggleEffectSupported = false))
    }

    @Test
    fun togglesKeepTheirOwnHapticWhenSupported() {
        assertEquals(Buzz.ToggleOn, resolveBuzz(Buzz.ToggleOn, toggleEffectSupported = true))
        assertEquals(Buzz.ToggleOff, resolveBuzz(Buzz.ToggleOff, toggleEffectSupported = true))
    }

    /** Only toggles change; in particular Reject (two short ticks, not a long buzz) is left as it is. */
    @Test
    fun otherBuzzesAreNeverChanged() {
        Buzz.entries.filterNot { it == Buzz.ToggleOn || it == Buzz.ToggleOff }.forEach { buzz ->
            assertEquals(buzz, resolveBuzz(buzz, toggleEffectSupported = false))
            assertEquals(buzz, resolveBuzz(buzz, toggleEffectSupported = true))
        }
    }
}
