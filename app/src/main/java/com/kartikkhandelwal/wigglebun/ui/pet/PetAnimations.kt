package com.kartikkhandelwal.wigglebun.ui.pet

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.random.Random

private const val NANOS_PER_SEC = 1_000_000_000f
private const val BLINK_SEC = 0.12f
private const val BLINK_MIN_GAP_SEC = 3f
private const val BLINK_MAX_GAP_SEC = 6f
private const val TWO_PI = (2 * Math.PI).toFloat()

/** Celebrate hops: two jumps of [HOP_SEC] each, starting at these times. */
private const val HOP_SEC = 0.9f
private const val SECOND_HOP_AT_SEC = 1.1f
private const val HOP_HEIGHT = 20f

/**
 * Idle motion of a pet, advanced once per frame by [step]. Only [frameNanos] is snapshot
 * state: the Canvas reads it inside its draw lambda, so each frame triggers a redraw but
 * never a recomposition. All other fields are plain floats read by [drawPet].
 *
 * A fresh instance (never stepped) is the still pose used by previews and screenshots.
 */
@Stable
internal class PetMotion {
    var frameNanos by mutableLongStateOf(0L)
        private set

    /** 0..1: how far into the current breath (0 and 1 = exhaled). */
    var breathPhase = 0f
        private set
    var breathAmplitude = 0f
        private set
    var bobAmplitude = 0f
        private set

    /** 0 = eyes open, 1 = closed. */
    var blink = 0f
        private set

    var jumpY = 0f
        private set
    var jumpScaleX = 1f
        private set
    var jumpScaleY = 1f
        private set

    /** Seconds since the pet entered CELEBRATE, or -1. */
    var celebrateSec = -1f
        private set

    /** Free-running seconds for looping extras (z's, sweat). Wraps every hour. */
    var loopSec = 0f
        private set

    private var lastNanos = -1L
    private var startNanos = 0L
    private var nextBlinkAt = 0L
    private var blinkStartedAt = -1L
    private var celebrateStartedAt = -1L
    private var lastMood: Mood? = null

    /** 0 → 1 → 0 over one breath, eased (cosine). */
    val breath: Float get() = 0.5f - 0.5f * cos(TWO_PI * breathPhase)

    fun step(nanos: Long, mood: Mood, spec: MoodSpec) {
        if (lastNanos < 0) {
            lastNanos = nanos
            startNanos = nanos
            breathAmplitude = spec.breathAmplitude
            bobAmplitude = spec.bobAmplitude
            nextBlinkAt = nanos + randomBlinkGap()
        }
        // Clamp so a long pause (app in background) doesn't make the pet lurch.
        val dt = ((nanos - lastNanos) / NANOS_PER_SEC).coerceIn(0f, 0.1f)
        lastNanos = nanos

        // Accumulate phase so a mood change alters the speed without jumping mid-breath,
        // and ease amplitudes towards the new mood.
        breathPhase = (breathPhase + dt / spec.breathPeriodSec) % 1f
        val ease = min(1f, dt * 3f)
        breathAmplitude += (spec.breathAmplitude - breathAmplitude) * ease
        bobAmplitude += (spec.bobAmplitude - bobAmplitude) * ease

        stepBlink(nanos, spec)

        if (mood != lastMood) {
            celebrateStartedAt = if (mood == Mood.CELEBRATE) nanos else -1L
            lastMood = mood
        }
        celebrateSec = if (celebrateStartedAt >= 0) (nanos - celebrateStartedAt) / NANOS_PER_SEC else -1f
        stepJump()

        loopSec = ((nanos - startNanos) / NANOS_PER_SEC) % 3600f
        frameNanos = nanos
    }

    private fun stepBlink(nanos: Long, spec: MoodSpec) {
        if (spec.blinks && spec.eyes == EyeShape.Dots && nanos >= nextBlinkAt) {
            blinkStartedAt = nanos
            nextBlinkAt = nanos + randomBlinkGap()
        }
        blink = if (blinkStartedAt >= 0) {
            val t = (nanos - blinkStartedAt) / NANOS_PER_SEC / BLINK_SEC
            if (t >= 1f) {
                blinkStartedAt = -1L
                0f
            } else {
                1f - abs(2f * t - 1f) // close then open
            }
        } else {
            0f
        }
    }

    /** Squash and stretch hops (CLAUDE.md §7): 1.1×0.85 → 0.92×1.1 → 1×1. */
    private fun stepJump() {
        val c = celebrateSec
        val u = when {
            c < 0f -> -1f
            c < HOP_SEC -> c
            c >= SECOND_HOP_AT_SEC && c < SECOND_HOP_AT_SEC + HOP_SEC -> c - SECOND_HOP_AT_SEC
            else -> -1f
        }
        if (u < 0f) {
            jumpY = 0f
            jumpScaleX = 1f
            jumpScaleY = 1f
            return
        }
        when {
            u < 0.12f -> { // crouch
                val t = u / 0.12f
                jumpY = 0f
                jumpScaleX = lerp(1f, 1.1f, t)
                jumpScaleY = lerp(1f, 0.85f, t)
            }
            u < 0.45f -> { // launch: stretch tall, rise, then relax at the top
                val t = (u - 0.12f) / 0.33f
                jumpY = -HOP_HEIGHT * (1f - (1f - t) * (1f - t))
                val stretch = min(1f, t * 3f)
                val relax = ((t - 0.5f) / 0.5f).coerceIn(0f, 1f)
                jumpScaleX = lerp(lerp(1.1f, 0.92f, stretch), 1f, relax)
                jumpScaleY = lerp(lerp(0.85f, 1.1f, stretch), 1f, relax)
            }
            u < 0.70f -> { // fall
                val t = (u - 0.45f) / 0.25f
                jumpY = -HOP_HEIGHT * (1f - t * t)
                jumpScaleX = lerp(1f, 0.96f, t)
                jumpScaleY = lerp(1f, 1.06f, t)
            }
            else -> { // land: squash, then wobble back to 1×1
                val t = ((u - 0.70f) / (HOP_SEC - 0.70f)).coerceIn(0f, 1f)
                val wobble = (1f - t) * cos(TWO_PI * t)
                jumpY = 0f
                jumpScaleX = 1f + 0.1f * wobble
                jumpScaleY = 1f - 0.12f * wobble
            }
        }
    }

    private fun randomBlinkGap(): Long {
        val sec = BLINK_MIN_GAP_SEC + Random.nextFloat() * (BLINK_MAX_GAP_SEC - BLINK_MIN_GAP_SEC)
        return (sec * NANOS_PER_SEC).toLong()
    }
}

internal fun lerp(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction
