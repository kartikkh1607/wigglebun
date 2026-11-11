package com.kartikkhandelwal.wigglebun.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.ui.theme.Card
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.LocalSurface
import com.kartikkhandelwal.wigglebun.ui.theme.OnSurface
import com.kartikkhandelwal.wigglebun.ui.theme.Surfaces
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunShapes
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme

private val S = Dimens.Shared

/** The small square buttons in a screen's top corners (design/ui_v2). */
enum class TopIcon(@DrawableRes val icon: Int, @StringRes val description: Int) {
    Back(R.drawable.ic_back, R.string.cd_back),
    Close(R.drawable.ic_close, R.string.cd_close),
    Menu(R.drawable.ic_menu, R.string.cd_open_menu),
}

/**
 * White 44 dp button with a chunky edge in the colour of the surface it sits on (Back on
 * Hours.dc.html, Close on Stretch.dc.html). The touch target is at least 48 dp.
 */
@Composable
fun TopIconButton(
    icon: TopIcon,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    edgeColor: Color = LocalSurface.current.edge,
) {
    val description = stringResource(icon.description)
    ChunkySurface(
        shape = WigglebunShapes.TopButton,
        faceColor = Card,
        edgeHeight = S.TopButtonEdge,
        edgeColor = edgeColor,
        onClick = onClick,
        contentAlignment = Alignment.Center,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(S.TopButtonSize, S.TopButtonSize + S.TopButtonEdge)
            .semantics { contentDescription = description },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(icon.icon), contentDescription = null, tint = Ink, modifier = Modifier.size(S.TopButtonIcon))
        }
    }
}

/** "Skip" as a white pill on a tinted background (Hours.dc.html); always visible (CLAUDE.md §6.2). */
@Composable
fun SkipPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    edgeColor: Color = LocalSurface.current.edge,
    faceColor: Color = Card,
    textColor: Color = Ink,
) {
    ChunkySurface(
        shape = WigglebunShapes.Pill,
        faceColor = faceColor,
        edgeHeight = S.SkipEdge,
        edgeColor = edgeColor,
        onClick = onClick,
        modifier = modifier.minimumInteractiveComponentSize(),
    ) {
        Text(
            stringResource(R.string.onboarding_skip),
            style = MaterialTheme.typography.titleSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = S.SkipPaddingHorizontal, vertical = S.SkipPaddingVertical),
        )
    }
}

@Preview(widthDp = 390)
@Composable
internal fun TopButtonsPreview() {
    WigglebunTheme {
        OnSurface(Surfaces.HomeWall) {
            Row(
                Modifier.background(LocalSurface.current.background).padding(Dimens.ScreenPadding),
                horizontalArrangement = Arrangement.spacedBy(Dimens.ListGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TopIcon.entries.forEach { TopIconButton(it, onClick = {}) }
                SkipPill(onClick = {})
            }
        }
    }
}
