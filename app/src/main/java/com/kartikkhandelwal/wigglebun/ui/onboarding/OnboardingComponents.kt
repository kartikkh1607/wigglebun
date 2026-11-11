package com.kartikkhandelwal.wigglebun.ui.onboarding

import android.app.Activity
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.kartikkhandelwal.wigglebun.ui.components.SkipPill
import com.kartikkhandelwal.wigglebun.ui.components.TopIcon
import com.kartikkhandelwal.wigglebun.ui.components.TopIconButton
import com.kartikkhandelwal.wigglebun.ui.theme.Card
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.LocalSurface

private val S = Dimens.Onboarding

/** Wake-up, name, work hours, interval (the mockup's "Step 1 of 4"). */
internal const val ONBOARDING_STEPS = 4

/**
 * The top row every onboarding screen shares: Back on the left where there is a step to go back
 * to, and "Skip" on the right, always visible (CLAUDE.md §6.2). Both sit on one centre line and
 * the same distance from their edge on every screen, so nothing jumps from step to step.
 * [skipFace], [skipText] and [skipEdge] restyle the pill for the night room.
 */
@Composable
internal fun OnboardingTopBar(
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    skipFace: Color = Card,
    skipText: Color = Ink,
    skipEdge: Color = LocalSurface.current.edge,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = S.TopBarTop, start = S.TopBarStart, end = S.TopBarEnd)
            .heightIn(min = S.TopBarButton),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) TopIconButton(TopIcon.Back, onClick = onBack)
        Spacer(Modifier.weight(1f))
        SkipPill(onClick = onSkip, edgeColor = skipEdge, faceColor = skipFace, textColor = skipText)
    }
}

/**
 * The app forces dark status-bar icons (it is light-only), which can't be read on Night. Switches
 * to light icons while [dark] and puts them back when the screen leaves. No window in previews.
 */
@Composable
internal fun StatusBarIcons(dark: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, dark) {
        val controller = (view.context as? Activity)?.window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.isAppearanceLightStatusBars = !dark
        onDispose { controller?.isAppearanceLightStatusBars = true }
    }
}
