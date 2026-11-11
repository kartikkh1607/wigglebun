package com.kartikkhandelwal.wigglebun.ui.pet

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.feedback.LocalFeel
import com.kartikkhandelwal.wigglebun.ui.motion.animationsEnabled
import kotlinx.coroutines.launch

private const val BUN_TAP_MILLIS = 380
private const val SQUASH = 0.04f // 4% (spec: 3-5%)
private const val TILT_DEGREES = 3f

/**
 * Petting the pet (Home and Pet profile): a small squash and tilt, a light haptic tick and a
 * chirp that follows [mood]. The squash is `graphicsLayer` only, so the layout never changes.
 * Taps inside the 800 ms cooldown do nothing at all, so 20 fast taps cannot pile up. With the
 * phone's "Remove animations" on there is no squash, but the haptic and the sound still happen.
 * TalkBack reads "Pet <name>".
 */
@Composable
fun Modifier.bunTap(mood: Mood, petName: String): Modifier {
    val feel = LocalFeel.current
    val animate = animationsEnabled()
    val pulse = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val label = stringResource(R.string.cd_pet_the_pet, petName)
    val react by rememberUpdatedState {
        if (feel.bunTap(mood) && animate) {
            scope.launch {
                pulse.snapTo(0f)
                pulse.animateTo(0f, keyframes {
                    durationMillis = BUN_TAP_MILLIS
                    1f at BUN_TAP_MILLIS / 3 using FastOutSlowInEasing
                })
            }
        }
    }
    return this
        .graphicsLayer {
            val p = pulse.value
            transformOrigin = TransformOrigin(0.5f, 1f) // squash toward the feet
            scaleX = 1f + SQUASH * p
            scaleY = 1f - SQUASH * p
            rotationZ = TILT_DEGREES * p
        }
        // Not `clickable`: that would split the pet from its speech bubble, which TalkBack reads as
        // one item. The tap is detected here; the action is exposed as a semantic click that merges up.
        .pointerInput(Unit) { detectTapGestures { react() } }
        .semantics {
            onClick(label = label) {
                react()
                true
            }
        }
}
