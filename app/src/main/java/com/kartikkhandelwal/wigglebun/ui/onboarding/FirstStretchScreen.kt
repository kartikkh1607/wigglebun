package com.kartikkhandelwal.wigglebun.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButtonStyle
import com.kartikkhandelwal.wigglebun.ui.pet.PetNameSentence
import com.kartikkhandelwal.wigglebun.ui.pet.PetSpecs
import com.kartikkhandelwal.wigglebun.ui.pet.PetView
import com.kartikkhandelwal.wigglebun.ui.theme.Cream
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.InkMuted
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme

private val S = Dimens.Onboarding

/** The offer of the first 1-minute stretch, the "value moment" before the paywall (CLAUDE.md §5.5). */
@Composable
fun FirstStretchScreen(petName: String, onStart: () -> Unit, onSkip: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().background(Cream).systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SkipRow(color = Ink, onSkip = onSkip)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = S.TextHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(S.TextGap),
        ) {
            Spacer(Modifier.height(S.QuestionTop))
            PetView(energy = 1f, mood = Mood.TIRED, spec = PetSpecs.Bun, name = petName, modifier = Modifier.size(S.StretchPetSize))
            Spacer(Modifier.height(S.NameTextTop))
            PetNameSentence(
                template = R.string.stretch_title,
                name = petName,
                style = MaterialTheme.typography.displayMedium,
                color = Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.stretch_body),
                style = MaterialTheme.typography.bodyLarge,
                color = InkMuted,
                textAlign = TextAlign.Center,
            )
        }
        ChunkyButton(
            text = stringResource(R.string.stretch_start),
            onClick = onStart,
            style = ChunkyButtonStyle.Highlight,
            modifier = Modifier.fillMaxWidth().padding(horizontal = S.NameButtonHorizontal, vertical = S.StretchButtonBottom),
        )
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun FirstStretchPreview() {
    WigglebunTheme { FirstStretchScreen("Bun", {}, {}) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun FirstStretchLongNamePreview() {
    WigglebunTheme { FirstStretchScreen("WWWWWWWWWWWW", {}, {}) }
}

@Preview(widthDp = 360, heightDp = 640, fontScale = 1.3f)
@Composable
internal fun FirstStretchSmallLargeFontPreview() {
    WigglebunTheme { FirstStretchScreen("Bun", {}, {}) }
}
