package com.kartikkhandelwal.wigglebun.feedback

import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.data.ApplicationScope
import com.kartikkhandelwal.wigglebun.data.prefs.SettingsRepository
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only place that plays sounds (SoundPool) or performs haptics (CLAUDE.md §11 "Feel").
 * Screens never call these directly: they use [Feel] from [LocalFeel]. Callers must trigger it
 * from one-shot events (a click, a consumed ViewModel event), never from state or recomposition.
 *
 * Sounds are for the foreground app only. This class is never used from workers, receivers, the
 * widget or notifications: a sound there would play with the screen off.
 */
@Singleton
class FeelPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    settings: SettingsRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val soundsOn: StateFlow<Boolean> = settings.soundsEnabled.stateIn(scope, SharingStarted.Eagerly, true)
    private val hapticsOn: StateFlow<Boolean> = settings.hapticsEnabled.stateIn(scope, SharingStarted.Eagerly, true)
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val main = Handler(Looper.getMainLooper())

    internal val policy = FeelPolicy(
        isForeground = { ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) },
        soundsOn = { soundsOn.value },
        hapticsOn = { hapticsOn.value },
        ringerNormal = { audio.ringerMode == AudioManager.RINGER_MODE_NORMAL },
        nowMillis = { SystemClock.elapsedRealtime() },
    )

    private var pool: SoundPool? = null
    private val soundIds = HashMap<FeelSound, Int>()
    private val loaded = HashSet<Int>()

    /** Loads every sound once, at app start. Until a sound has loaded, playing it is skipped silently. */
    fun preload() {
        if (pool != null) return
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val soundPool = SoundPool.Builder().setMaxStreams(MAX_STREAMS).setAudioAttributes(attributes).build()
        soundPool.setOnLoadCompleteListener { _, id, status ->
            if (status == 0) synchronized(loaded) { loaded += id }
        }
        pool = soundPool
        FeelSound.entries.forEach { soundIds[it] = soundPool.load(context, it.rawRes(), 1) }
    }

    /** Plays [sound] (after [delayMillis]) if the policy allows it; never throws. */
    fun play(sound: FeelSound, delayMillis: Long = 0) {
        if (delayMillis > 0) {
            scope.launch {
                delay(delayMillis)
                // Checked again after the delay: the app may have gone to the background meanwhile.
                playNow(sound)
            }
        } else {
            playNow(sound)
        }
    }

    private fun playNow(sound: FeelSound) {
        if (!policy.maySound()) return
        val id = soundIds[sound] ?: return
        if (!synchronized(loaded) { id in loaded }) return
        runCatching { pool?.play(id, sound.volume, sound.volume, 1, 0, 1f) }
    }

    /** The celebration's sounds, once per logged break. */
    fun celebrate(breakKey: Any, sparksGranted: Int) {
        policy.celebrationCues(breakKey, sparksGranted).forEach { play(it.sound, it.delayMillis) }
    }

    /** A Bun tap reaction; false (and nothing played) while the 800 ms cooldown runs. */
    fun tryBunTap(haptic: HapticFeedback, mood: Mood): Boolean {
        if (!policy.tryBunTap()) return false
        vibrate(haptic, Buzz.Tick)
        play(FeelPolicy.bunChirp(mood))
        return true
    }

    /**
     * Whether this phone's vibrator can play the prebaked ticks the toggle and segment haptics are
     * made of (the SDK only lets us ask about [VibrationEffect.EFFECT_TICK], the same family). Some
     * phones can't (one reports no supported effects) and then drop those haptics silently, so
     * [vibrate] uses the light tick instead (see [resolveBuzz]). Asked once; "unknown" counts as
     * supported, so a phone that can't say keeps the system's own haptic. Below API 30 there is no
     * way to ask.
     */
    private val tickEffectSupported: Boolean by lazy {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@lazy true
        val vibrator = context.getSystemService(Vibrator::class.java) ?: return@lazy true
        vibrator.areAllEffectsSupported(VibrationEffect.EFFECT_TICK) != Vibrator.VIBRATION_EFFECT_SUPPORT_NO
    }

    internal fun vibrate(haptic: HapticFeedback, wanted: Buzz) {
        if (!policy.mayVibrate()) return
        // Still the system's haptic through the view, so the phone's touch-feedback setting applies.
        val buzz = resolveBuzz(wanted, tickEffectSupported)
        when (buzz) {
            // ContextClick is the lighter of the two candidates on most phones; see the PR notes.
            Buzz.Tick -> haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
            Buzz.Segment -> haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
            Buzz.ToggleOn -> haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
            Buzz.ToggleOff -> haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
            Buzz.Confirm -> haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            Buzz.Reject ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    haptic.performHapticFeedback(HapticFeedbackType.Reject)
                } else {
                    // Below API 30 Compose maps Reject to LONG_PRESS, a long buzz. Two short ticks instead.
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    main.postDelayed({ haptic.performHapticFeedback(HapticFeedbackType.ContextClick) }, REJECT_GAP_MILLIS)
                }
        }
    }

    private fun FeelSound.rawRes(): Int = when (this) {
        FeelSound.TapSoft -> R.raw.tap_soft
        FeelSound.Toggle -> R.raw.toggle
        FeelSound.BunChirpBright -> R.raw.bun_chirp_1
        FeelSound.BunChirpMid -> R.raw.bun_chirp_2
        FeelSound.BunChirpLow -> R.raw.bun_chirp_3
        FeelSound.BreakStart -> R.raw.break_start
        FeelSound.Celebrate -> R.raw.celebrate
        FeelSound.Sparks -> R.raw.sparks
        FeelSound.Success -> R.raw.success
    }

    private companion object {
        const val MAX_STREAMS = 3
        const val REJECT_GAP_MILLIS = 90L
    }
}

internal enum class Buzz { Tick, Segment, ToggleOn, ToggleOff, Confirm, Reject }

/**
 * The buzz to perform: a toggle or a segment tick falls back to the light [Buzz.Tick] (a context
 * click, never a long buzz) when the phone can't play the tick effects they are made of;
 * everything else is left as asked.
 */
internal fun resolveBuzz(buzz: Buzz, tickEffectSupported: Boolean): Buzz =
    if (buzz in NeedsTickEffect && !tickEffectSupported) Buzz.Tick else buzz

private val NeedsTickEffect = setOf(Buzz.ToggleOn, Buzz.ToggleOff, Buzz.Segment)

/**
 * What composables use. Previews and screenshot tests get [Silent], which does nothing.
 * Tick, segment and toggle are for controls the user just touched; [confirm] and [reject] are for
 * outcomes, so call them from a consumed one-shot event after the work has succeeded.
 */
@Immutable
class Feel internal constructor(private val player: FeelPlayer?, private val haptic: HapticFeedback?) {
    /** A light tick on a click, and [sound] if given (primary CTAs only). */
    fun tick(sound: FeelSound? = null) {
        val p = player ?: return
        haptic?.let { p.vibrate(it, Buzz.Tick) }
        sound?.let { p.play(it) }
    }

    /** One step of a slider or one segment of a group (a day, a tab). */
    fun segment() {
        val p = player ?: return
        haptic?.let { p.vibrate(it, Buzz.Segment) }
    }

    fun toggle(on: Boolean, withSound: Boolean = true) {
        val p = player ?: return
        haptic?.let { p.vibrate(it, if (on) Buzz.ToggleOn else Buzz.ToggleOff) }
        if (withSound) p.play(FeelSound.Toggle)
    }

    /** Something changed the app's data. Never fire it before the repository call has returned. */
    fun confirm(sound: FeelSound? = null) {
        val p = player ?: return
        haptic?.let { p.vibrate(it, Buzz.Confirm) }
        sound?.let { p.play(it) }
    }

    /** An action that was blocked. */
    fun reject() {
        val p = player ?: return
        haptic?.let { p.vibrate(it, Buzz.Reject) }
    }

    fun play(sound: FeelSound) {
        player?.play(sound)
    }

    /** Break celebration: sounds once per [breakKey]; the sparks sound only if [sparksGranted] > 0. */
    fun celebrate(breakKey: Any, sparksGranted: Int) {
        player?.celebrate(breakKey, sparksGranted)
    }

    /** Bun was tapped. Returns false inside the cooldown, so the caller skips its squash too. */
    fun bunTap(mood: Mood): Boolean {
        val p = player ?: return true
        val h = haptic ?: return true
        return p.tryBunTap(h, mood)
    }

    companion object {
        val Silent = Feel(null, null)
    }
}

val LocalFeel = staticCompositionLocalOf { Feel.Silent }

/** `clickable` with the light tick, for plain tappable text, chips and rows that are not a `ChunkySurface`. */
@Composable
fun Modifier.tickClickable(
    onClickLabel: String? = null,
    role: Role = Role.Button,
    onClick: () -> Unit,
): Modifier {
    val feel = LocalFeel.current
    return clickable(onClickLabel = onClickLabel, role = role) {
        feel.tick()
        onClick()
    }
}

/** Makes [Feel] available below, wrapping the host's [LocalHapticFeedback]. */
@Composable
fun ProvideFeel(player: FeelPlayer, content: @Composable () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val feel = remember(player, haptic) { Feel(player, haptic) }
    CompositionLocalProvider(LocalFeel provides feel, content = content)
}
