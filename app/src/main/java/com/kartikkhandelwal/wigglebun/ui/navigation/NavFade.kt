package com.kartikkhandelwal.wigglebun.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry

/**
 * Screens swap with a short fade: NavHost's default 700 ms crossfade left both screens showing
 * long enough to look like a glitch. Like every tween, it follows the system animation scale.
 */
private const val NAV_FADE_MILLIS = 200

internal val navFadeIn: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(tween(NAV_FADE_MILLIS, easing = FastOutSlowInEasing))
}

internal val navFadeOut: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(tween(NAV_FADE_MILLIS, easing = FastOutSlowInEasing))
}
