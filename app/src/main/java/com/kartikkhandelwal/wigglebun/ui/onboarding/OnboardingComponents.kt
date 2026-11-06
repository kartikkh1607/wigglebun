package com.kartikkhandelwal.wigglebun.ui.onboarding

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.core.view.WindowCompat
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunShapes

private val S = Dimens.Onboarding

/** Wake-up, name, work hours, interval (the mockup's "Step 1 of 4"). */
internal const val ONBOARDING_STEPS = 4

/** The top row every onboarding screen shares: "Skip" is always visible (CLAUDE.md §6.2). */
@Composable
internal fun SkipRow(color: Color, onSkip: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = S.SkipRowTop, start = S.SkipRowHorizontal, end = S.SkipRowHorizontal),
        horizontalArrangement = Arrangement.End,
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = S.SkipMinSize, minHeight = S.SkipMinSize)
                .clip(WigglebunShapes.Pill)
                .clickable(role = Role.Button, onClick = onSkip)
                .padding(horizontal = S.SkipPaddingHorizontal, vertical = S.SkipPaddingVertical),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.onboarding_skip), style = MaterialTheme.typography.titleSmall, color = color)
        }
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
