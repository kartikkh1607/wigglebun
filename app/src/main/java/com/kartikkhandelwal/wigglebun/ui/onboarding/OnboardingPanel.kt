package com.kartikkhandelwal.wigglebun.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButton
import com.kartikkhandelwal.wigglebun.ui.components.SheetPanel
import com.kartikkhandelwal.wigglebun.ui.components.SkipPill
import com.kartikkhandelwal.wigglebun.ui.components.StepDots
import com.kartikkhandelwal.wigglebun.ui.components.TopIcon
import com.kartikkhandelwal.wigglebun.ui.components.TopIconButton
import com.kartikkhandelwal.wigglebun.ui.theme.Card
import com.kartikkhandelwal.wigglebun.ui.theme.Coral
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.InkMuted
import com.kartikkhandelwal.wigglebun.ui.theme.LocalSurface
import com.kartikkhandelwal.wigglebun.ui.theme.OnSurface
import com.kartikkhandelwal.wigglebun.ui.theme.Surfaces

private val P = Dimens.OnboardingPanel

/**
 * The frame of the Hours and Interval questions (design/ui_v2/Hours.dc.html, Interval.dc.html):
 * a green wall with a scene on top, Back and Skip in the corners, and a white [SheetPanel] with
 * the step dots, the question and the answer. Scene and panel scroll together; Continue stays at
 * the bottom.
 *
 * The scene is drawn for a phone as tall as the mockup. On a shorter phone, or with a bigger
 * font, it shrinks (never below [Dimens.OnboardingPanel.MinScale]) so the whole question and its
 * answer stay on screen: [panelHeight], what the panel needs at normal font size, is what the
 * scene leaves room for. Above [Dimens.OnboardingPanel.LargeFontScale] Continue moves into the
 * panel, where it no longer takes a share of the screen.
 *
 * [error], if any, shows right above Continue.
 */
@Composable
internal fun OnboardingPanelScreen(
    step: Int,
    title: String,
    body: String,
    panelHeight: Dp,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    continueEnabled: Boolean = true,
    error: String? = null,
    scene: @Composable (scale: Float) -> Unit,
    answer: @Composable ColumnScope.() -> Unit,
) {
    val fontScale = LocalDensity.current.fontScale
    val buttonInPanel = fontScale > P.LargeFontScale
    val errorText: @Composable () -> Unit = {
        if (error != null) {
            Text(
                error,
                style = MaterialTheme.typography.bodyMedium,
                color = Coral,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = P.ErrorTop).semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
    val button: @Composable () -> Unit = {
        ChunkyButton(
            text = stringResource(R.string.onboarding_continue),
            onClick = onContinue,
            enabled = continueEnabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    OnSurface(Surfaces.HomeWall) {
        Box(modifier.fillMaxSize().background(Card)) {
            Column(Modifier.fillMaxSize().imePadding()) {
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val viewport = maxHeight
                    // What is left for the scene once the panel has what it needs (at this font size).
                    val scale = ((viewport - panelHeight * fontScale) / P.SceneHeight).coerceIn(P.MinScale, 1f)
                    val sceneHeight = P.SceneHeight * scale
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        // At least as tall as the mockup's, scaled; a scene whose text grows with the font may be taller.
                        Box(Modifier.fillMaxWidth().heightIn(min = sceneHeight).background(LocalSurface.current.background)) {
                            scene(scale)
                        }
                        SheetPanel(Modifier.fillMaxWidth().heightIn(min = viewport - sceneHeight)) {
                            StepDots(step = step, count = ONBOARDING_STEPS)
                            Column(verticalArrangement = Arrangement.spacedBy(P.BodyGap)) {
                                Text(
                                    title,
                                    style = MaterialTheme.typography.displayMedium.copy(fontSize = P.TitleSize),
                                    color = Ink,
                                    modifier = Modifier.semantics { heading() },
                                )
                                Text(body, style = MaterialTheme.typography.bodyLarge, color = InkMuted)
                            }
                            answer()
                            if (buttonInPanel) {
                                Column(Modifier.padding(bottom = P.CtaBottom)) {
                                    errorText()
                                    Box(Modifier.padding(top = P.CtaTop)) { button() }
                                }
                            }
                        }
                    }
                }
                if (!buttonInPanel) {
                    // Part of the panel: white to the bottom of the screen, under the system bar.
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(Card)
                            .navigationBarsPadding()
                            .padding(start = P.CtaHorizontal, end = P.CtaHorizontal, top = P.CtaTop, bottom = P.CtaBottom),
                    ) {
                        errorText()
                        button()
                    }
                }
            }
            TopIconButton(
                TopIcon.Back,
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(start = P.TopButtonsInset, top = P.TopButtonsInset),
            )
            Box(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = P.SkipTop, end = P.SkipEnd)) {
                SkipPill(onClick = onSkip)
            }
        }
    }
}
