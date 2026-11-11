package com.kartikkhandelwal.wigglebun.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.feedback.FeelSound
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButton
import com.kartikkhandelwal.wigglebun.ui.components.PillCard
import com.kartikkhandelwal.wigglebun.ui.components.TopIcon
import com.kartikkhandelwal.wigglebun.ui.components.TopIconButton
import com.kartikkhandelwal.wigglebun.ui.pet.PetView
import com.kartikkhandelwal.wigglebun.ui.pet.displayName
import com.kartikkhandelwal.wigglebun.ui.pet.spec
import com.kartikkhandelwal.wigglebun.ui.theme.BreakEyesTint
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.InkMuted
import com.kartikkhandelwal.wigglebun.ui.theme.OnSurface
import com.kartikkhandelwal.wigglebun.ui.theme.RoomPalette
import com.kartikkhandelwal.wigglebun.ui.theme.SurfaceColors
import com.kartikkhandelwal.wigglebun.ui.theme.TabPetTint
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunShapes
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme
import com.kartikkhandelwal.wigglebun.ui.theme.balanced

private val H = Dimens.Home
private val L = Dimens.LockedHome

/** Where the locked Home can go. */
class LockedHomeNavigation(
    val onOpenMenu: () -> Unit = {},
    val onOpenPaywall: () -> Unit = {},
)

@Composable
fun LockedHomeScreenRoute(
    navigation: LockedHomeNavigation,
    modifier: Modifier = Modifier,
    viewModel: LockedHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LockedHomeScreen(state, modifier, onOpenMenu = navigation.onOpenMenu, onOpenPaywall = navigation.onOpenPaywall)
}

/**
 * Home with no entitlement (design/ui_v2/Locked.dc.html; CLAUDE.md §5.5): the room at dusk, the
 * pet asleep, and one card with one button that opens the paywall. Calm on purpose: no timer, no
 * countdown, nothing that nags. Collection, the pet's page and Settings stay reachable.
 */
@Composable
fun LockedHomeScreen(
    state: LockedHomeUiState,
    modifier: Modifier = Modifier,
    onOpenMenu: () -> Unit = {},
    onOpenPaywall: () -> Unit = {},
) {
    val petName = state.pet.displayName()
    val dusk = RoomPalette.Dusk
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(modifier.fillMaxSize().background(dusk.floor).verticalScroll(rememberScrollState())) {
        OnSurface(SurfaceColors(background = dusk.wall, edge = dusk.wallEdge)) {
            Box(Modifier.fillMaxWidth().height(H.SceneHeight + topInset)) {
                RoomScene(Modifier.fillMaxSize(), palette = dusk, topInset = topInset, showClockAndShelf = false)
                PetView(
                    energy = 1f,
                    mood = Mood.SLEEPING,
                    spec = state.pet.spec,
                    name = petName,
                    accessories = state.pet.wearing,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = H.PetAboveSceneBottom)
                        .size(H.PetWidth, H.PetHeight),
                )
                TopIconButton(
                    TopIcon.Menu,
                    onClick = onOpenMenu,
                    modifier = Modifier.align(Alignment.TopStart).padding(top = topInset + H.TopBarPadding, start = H.TopBarPadding),
                )
            }
        }
        OnSurface(SurfaceColors(background = dusk.floor, edge = dusk.skirtingShadow)) {
            NappingCard(
                state = state,
                petName = petName,
                onOpenPaywall = onOpenPaywall,
                modifier = Modifier.padding(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, top = L.CardTop, bottom = L.CardBottom),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NappingCard(state: LockedHomeUiState, petName: String, onOpenPaywall: () -> Unit, modifier: Modifier) {
    val offer = state.offer
    val trialDays = offer?.trialDays
    PillCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(L.CardRadius),
        edgeHeight = L.CardEdge,
        contentPadding = PaddingValues(start = L.CardPadding, end = L.CardPadding, top = L.CardPaddingTop, bottom = L.CardPadding),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(L.Gap)) {
            Text(
                stringResource(R.string.locked_title, petName),
                style = MaterialTheme.typography.displayMedium.copy(fontSize = L.TitleSize).balanced(),
                color = Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().semantics { heading() },
            )
            Text(
                stringResource(if (trialDays != null) R.string.locked_body_trial else R.string.locked_body, petName),
                style = MaterialTheme.typography.bodyLarge.balanced(),
                color = InkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = L.ChipsVertical),
                horizontalArrangement = Arrangement.spacedBy(L.ChipGap, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(L.ChipGap),
            ) {
                if (state.totalBreaks > 0) {
                    Chip(pluralStringResource(R.plurals.locked_breaks_saved, state.totalBreaks, state.totalBreaks), TabPetTint)
                }
                Chip(stringResource(R.string.locked_pets_stay), BreakEyesTint)
            }
            ChunkyButton(
                text = if (trialDays != null) stringResource(R.string.paywall_cta_trial, trialDays) else stringResource(R.string.locked_cta_plans),
                onClick = onOpenPaywall,
                wrap = true,
                sound = FeelSound.TapSoft,
                modifier = Modifier.fillMaxWidth(),
            )
            // The price after the trial, only next to a button that offers the trial.
            if (offer != null && trialDays != null) {
                Text(
                    stringResource(R.string.locked_then, stringResource(R.string.paywall_price_yearly, offer.price)),
                    style = MaterialTheme.typography.labelMedium,
                    color = InkMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun Chip(text: String, color: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = Ink,
        modifier = Modifier.background(color, WigglebunShapes.Pill).padding(horizontal = L.ChipPaddingHorizontal, vertical = L.ChipPaddingVertical),
    )
}

/** States for previews and screenshot tests; the price is sample data. */
internal object LockedHomePreviewStates {
    val trial = LockedHomeUiState(totalBreaks = 42, offer = LockedOffer(price = "$29.99", trialDays = 7))
    val noTrial = LockedHomeUiState(totalBreaks = 42, offer = LockedOffer(price = "$29.99", trialDays = null))

    /** Straight after onboarding with no plans loaded yet: one break, no offer. */
    val fresh = LockedHomeUiState(totalBreaks = 1)
}

@Preview(widthDp = 390, heightDp = 760)
@Composable
internal fun LockedHomeTrialPreview() {
    WigglebunTheme { LockedHomeScreen(LockedHomePreviewStates.trial) }
}

@Preview(widthDp = 390, heightDp = 760)
@Composable
internal fun LockedHomeNoTrialPreview() {
    WigglebunTheme { LockedHomeScreen(LockedHomePreviewStates.noTrial) }
}

@Preview(widthDp = 390, heightDp = 760)
@Composable
internal fun LockedHomeFreshPreview() {
    WigglebunTheme { LockedHomeScreen(LockedHomePreviewStates.fresh) }
}

@Preview(widthDp = 360, heightDp = 560, fontScale = 1.3f)
@Composable
internal fun LockedHomeSmallLargeFontPreview() {
    WigglebunTheme { LockedHomeScreen(LockedHomePreviewStates.trial) }
}
