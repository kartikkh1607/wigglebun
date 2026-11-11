package com.kartikkhandelwal.wigglebun.feedback

import com.kartikkhandelwal.wigglebun.domain.model.Mood

/** Every sound the app can make (CLAUDE.md §11 "Feel"). Files live in res/raw; see design/sounds/LICENSES.md. */
enum class FeelSound(val volume: Float) {
    TapSoft(0.6f),
    Toggle(0.35f),
    BunChirpBright(0.6f),
    BunChirpMid(0.5f),
    BunChirpLow(0.4f),
    BreakStart(0.7f),
    Celebrate(0.8f),
    Sparks(0.5f),
    Success(0.8f),
}

/** A sound and how long after the event it starts. */
data class SoundCue(val sound: FeelSound, val delayMillis: Long = 0)

/**
 * The rules for when feedback may fire. Pure Kotlin: the phone's state arrives as lambdas so the
 * rules are unit-tested without Android. Nothing here plays anything.
 */
class FeelPolicy(
    private val isForeground: () -> Boolean,
    private val soundsOn: () -> Boolean,
    private val hapticsOn: () -> Boolean,
    /** Silent and vibrate ringer modes mean no sound. */
    private val ringerNormal: () -> Boolean,
    private val nowMillis: () -> Long,
) {
    private var lastBunTap = Long.MIN_VALUE
    private val celebrated = ArrayDeque<Any>()

    fun mayVibrate(): Boolean = hapticsOn()

    /** Sounds need all three: foreground, the Sounds setting, and a normal ringer. */
    fun maySound(): Boolean = isForeground() && soundsOn() && ringerNormal()

    /** One Bun tap per [BUN_TAP_COOLDOWN_MILLIS]; 20 fast taps give 1 reaction, not 20. */
    fun tryBunTap(): Boolean {
        val now = nowMillis()
        if (lastBunTap != Long.MIN_VALUE && now - lastBunTap < BUN_TAP_COOLDOWN_MILLIS) return false
        lastBunTap = now
        return true
    }

    /**
     * The celebration's sounds, once per logged break ([breakKey] identifies it). The sparks sound
     * follows only when sparks were granted, and `success` is never part of it.
     */
    fun celebrationCues(breakKey: Any, sparksGranted: Int): List<SoundCue> {
        if (breakKey in celebrated) return emptyList()
        celebrated.addLast(breakKey)
        if (celebrated.size > REMEMBERED_CELEBRATIONS) celebrated.removeFirst()
        return buildList {
            add(SoundCue(FeelSound.Celebrate))
            if (sparksGranted > 0) add(SoundCue(FeelSound.Sparks, SPARKS_DELAY_MILLIS))
        }
    }

    companion object {
        const val BUN_TAP_COOLDOWN_MILLIS = 800L
        const val SPARKS_DELAY_MILLIS = 400L
        private const val REMEMBERED_CELEBRATIONS = 8

        /** HAPPY brighter; TIRED and STIFF lower and softer. Others use the middle chirp. */
        fun bunChirp(mood: Mood): FeelSound = when (mood) {
            Mood.HAPPY, Mood.CELEBRATE, Mood.STRETCH -> FeelSound.BunChirpBright
            Mood.TIRED, Mood.STIFF -> FeelSound.BunChirpLow
            else -> FeelSound.BunChirpMid
        }
    }
}
