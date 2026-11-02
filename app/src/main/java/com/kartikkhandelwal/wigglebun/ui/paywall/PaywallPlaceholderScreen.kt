package com.kartikkhandelwal.wigglebun.ui.paywall

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButton
import com.kartikkhandelwal.wigglebun.ui.pet.PetSpecs
import com.kartikkhandelwal.wigglebun.ui.pet.PetView
import com.kartikkhandelwal.wigglebun.ui.theme.Cream
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.InkMuted
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme

private val S = Dimens.Onboarding

/**
 * Where the paywall will go (CLAUDE.md §5.5): M10 replaces this with the real one and the locked
 * Home. It offers nothing to buy. Back does the same as Continue, as closing the paywall will.
 */
@Composable
fun PaywallPlaceholderScreen(petName: String, onContinue: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler(onBack = onContinue)
    Column(
        modifier.fillMaxSize().background(Cream).systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = S.TextHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(S.TextGap, Alignment.CenterVertically),
        ) {
            PetView(energy = 1f, mood = Mood.HAPPY, spec = PetSpecs.Bun, name = petName, modifier = Modifier.size(S.StretchPetSize))
            Text(
                stringResource(R.string.paywall_placeholder_title),
                style = MaterialTheme.typography.displayMedium,
                color = Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.paywall_placeholder_body),
                style = MaterialTheme.typography.bodyLarge,
                color = InkMuted,
                textAlign = TextAlign.Center,
            )
        }
        ChunkyButton(
            text = stringResource(R.string.onboarding_continue),
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().padding(horizontal = S.NameButtonHorizontal, vertical = S.StretchButtonBottom),
        )
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun PaywallPlaceholderPreview() {
    WigglebunTheme { PaywallPlaceholderScreen("Bun", {}) }
}
