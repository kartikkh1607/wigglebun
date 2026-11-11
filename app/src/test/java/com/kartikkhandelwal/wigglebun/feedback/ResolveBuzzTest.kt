package com.kartikkhandelwal.wigglebun.feedback

import org.junit.Assert.assertEquals
import org.junit.Test

/** Which haptic a buzz becomes on a phone that can't (or can) play the tick effects. */
class ResolveBuzzTest {

    private val needTickEffect = listOf(Buzz.ToggleOn, Buzz.ToggleOff, Buzz.Segment)

    @Test
    fun togglesAndSegmentTicksFallBackToTheLightTickWhenUnsupported() {
        needTickEffect.forEach { buzz -> assertEquals(Buzz.Tick, resolveBuzz(buzz, tickEffectSupported = false)) }
    }

    /** Supported, or unknown (Feel passes true for it): the system's own haptic stays. */
    @Test
    fun theyKeepTheirOwnHapticWhenSupported() {
        needTickEffect.forEach { buzz -> assertEquals(buzz, resolveBuzz(buzz, tickEffectSupported = true)) }
    }

    /** Only those change; in particular Reject (two short ticks, not a long buzz) is left as it is. */
    @Test
    fun otherBuzzesAreNeverChanged() {
        Buzz.entries.filterNot { it in needTickEffect }.forEach { buzz ->
            assertEquals(buzz, resolveBuzz(buzz, tickEffectSupported = false))
            assertEquals(buzz, resolveBuzz(buzz, tickEffectSupported = true))
        }
    }
}
