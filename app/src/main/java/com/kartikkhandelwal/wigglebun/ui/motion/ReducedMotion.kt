package com.kartikkhandelwal.wigglebun.ui.motion

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * True when the phone's "Remove animations" is on (Settings → Accessibility; it sets the animator
 * duration scale to 0, as Developer options can too).
 *
 * Compose's own springs and tweens already follow that scale and finish at once. What they can't
 * know about is the app's own motion: the pet's frame loop (breathing, blinking, bob, the
 * celebrate hop), the Home cards' stagger, the wake-up stretch and the break session's pose cycle.
 * Those read this. Provided in MainActivity by [ProvideReducedMotion]; false in previews and tests
 * unless a test provides it.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

/**
 * False in previews and screenshot tests (still frames) and when the user removed animations;
 * the default for the composables that run their own motion.
 */
@Composable
@ReadOnlyComposable
fun animationsEnabled(): Boolean = !LocalInspectionMode.current && !LocalReducedMotion.current

/** Feeds [LocalReducedMotion] from the system setting, following changes while the app is visible. */
@Composable
fun ProvideReducedMotion(content: @Composable () -> Unit) {
    val resolver = LocalContext.current.contentResolver
    val changes = remember(resolver) { resolver.reducedMotionFlow() }
    // Read at once, so the first frame already knows (no pet starting to breathe, then stopping).
    val reduced by changes.collectAsStateWithLifecycle(initialValue = remember(resolver) { resolver.reducedMotion() })
    CompositionLocalProvider(LocalReducedMotion provides reduced, content = content)
}

internal fun ContentResolver.reducedMotion(): Boolean =
    Settings.Global.getFloat(this, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

/**
 * The setting now, then again after every change, while collected. The observer is registered
 * only while collecting (collectAsStateWithLifecycle stops at ON_STOP), and a fresh collection
 * reads the setting again, so a change made in system Settings while the app was away is seen
 * on return.
 */
internal fun ContentResolver.reducedMotionFlow(): Flow<Boolean> = callbackFlow {
    val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            trySend(reducedMotion())
        }
    }
    registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
    trySend(reducedMotion())
    awaitClose { unregisterContentObserver(observer) }
}.distinctUntilChanged()
