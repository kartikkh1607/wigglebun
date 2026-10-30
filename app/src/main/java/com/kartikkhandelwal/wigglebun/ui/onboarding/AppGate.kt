package com.kartikkhandelwal.wigglebun.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kartikkhandelwal.wigglebun.ui.theme.Cream
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme

/**
 * First-launch gate (CLAUDE.md §6.2). Onboarding runs apart from the main app, so finishing it
 * leaves nothing on the tabs' back stack. While [onboardingDone] is still null (the saved answer
 * is loading) it shows the plain background, so a cold start never flashes the wrong screen.
 */
@Composable
fun AppGate(
    onboardingDone: Boolean?,
    onboarding: @Composable () -> Unit,
    app: @Composable () -> Unit,
) {
    when (onboardingDone) {
        null -> Box(Modifier.fillMaxSize().background(Cream))
        false -> onboarding()
        true -> app()
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun AppGateLoadingPreview() {
    WigglebunTheme { AppGate(onboardingDone = null, onboarding = {}, app = {}) }
}
