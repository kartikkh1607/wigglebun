package com.kartikkhandelwal.wigglebun.feedback

import com.kartikkhandelwal.wigglebun.domain.model.Mood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Feel rules (CLAUDE.md §11): when sounds and haptics may fire. Pure Kotlin, no Android. */
class FeelPolicyTest {
    private var foreground = true
    private var sounds = true
    private var haptics = true
    private var ringerNormal = true
    private var now = 10_000L

    private fun policy() = FeelPolicy(
        isForeground = { foreground },
        soundsOn = { sounds },
        hapticsOn = { haptics },
        ringerNormal = { ringerNormal },
        nowMillis = { now },
    )

    @Test fun `sound plays when foreground, Sounds on and ringer normal`() {
        assertTrue(policy().maySound())
    }

    @Test fun `Sounds setting off means no sound`() {
        sounds = false
        assertFalse(policy().maySound())
    }

    @Test fun `silent or vibrate ringer means no sound`() {
        // The platform's silent and vibrate modes are both "not normal" to the policy.
        ringerNormal = false
        assertFalse(policy().maySound())
    }

    @Test fun `background means no sound`() {
        foreground = false
        assertFalse(policy().maySound())
    }

    @Test fun `haptics follow only their own setting`() {
        sounds = false
        ringerNormal = false
        foreground = false
        assertTrue(policy().mayVibrate())
        haptics = false
        assertFalse(policy().mayVibrate())
    }

    @Test fun `twenty fast Bun taps give one reaction`() {
        val p = policy()
        var reactions = 0
        repeat(20) {
            if (p.tryBunTap()) reactions++
            now += 40 // 20 taps in 800 ms
        }
        assertEquals(1, reactions)
    }

    @Test fun `Bun reacts again once the cooldown has passed`() {
        val p = policy()
        assertTrue(p.tryBunTap())
        now += FeelPolicy.BUN_TAP_COOLDOWN_MILLIS - 1
        assertFalse(p.tryBunTap())
        now += 1
        assertTrue(p.tryBunTap())
    }

    @Test fun `no sparks sound when no sparks were granted`() {
        val cues = policy().celebrationCues(breakKey = 1L, sparksGranted = 0)
        assertEquals(listOf(FeelSound.Celebrate), cues.map { it.sound })
    }

    @Test fun `sparks follow the celebration by 400 ms when sparks were granted`() {
        val cues = policy().celebrationCues(breakKey = 1L, sparksGranted = 5)
        assertEquals(listOf(FeelSound.Celebrate, FeelSound.Sparks), cues.map { it.sound })
        assertEquals(0L, cues[0].delayMillis)
        assertEquals(400L, cues[1].delayMillis)
    }

    @Test fun `celebrate plays once per logged break`() {
        val p = policy()
        assertEquals(2, p.celebrationCues(breakKey = "break-7", sparksGranted = 5).size)
        // The same break reported again (recomposition, a repeated Flow emission): nothing.
        assertTrue(p.celebrationCues(breakKey = "break-7", sparksGranted = 5).isEmpty())
        // The next break celebrates again.
        assertEquals(2, p.celebrationCues(breakKey = "break-8", sparksGranted = 5).size)
    }

    @Test fun `celebrate and success are never played together`() {
        val sounds = policy().celebrationCues(breakKey = 1L, sparksGranted = 50).map { it.sound }
        assertFalse(FeelSound.Success in sounds)
    }

    @Test fun `Bun chirp follows the mood`() {
        assertEquals(FeelSound.BunChirpBright, FeelPolicy.bunChirp(Mood.HAPPY))
        assertEquals(FeelSound.BunChirpLow, FeelPolicy.bunChirp(Mood.TIRED))
        assertEquals(FeelSound.BunChirpLow, FeelPolicy.bunChirp(Mood.STIFF))
        assertTrue(FeelSound.BunChirpLow.volume < FeelSound.BunChirpBright.volume)
    }
}
