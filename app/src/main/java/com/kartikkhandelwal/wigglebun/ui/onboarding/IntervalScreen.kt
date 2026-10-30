package com.kartikkhandelwal.wigglebun.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.domain.sitting.SittingSettings
import com.kartikkhandelwal.wigglebun.ui.collection.PetSheet
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButton
import com.kartikkhandelwal.wigglebun.ui.components.PillCard
import com.kartikkhandelwal.wigglebun.ui.pet.PetSpecs
import com.kartikkhandelwal.wigglebun.ui.pet.PetView
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.InkEdge
import com.kartikkhandelwal.wigglebun.ui.theme.Track
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme
import kotlin.math.roundToInt

private val S = Dimens.Onboarding

private const val INTERVAL_STEP = 5

/** Slider ticks between the ends: 20, 25, … 90 minutes. */
private val SliderSteps = (SittingSettings.MAX_INTERVAL_MINUTES - SittingSettings.MIN_INTERVAL_MINUTES) / INTERVAL_STEP - 1

@Composable
fun IntervalScreenRoute(
    minutes: Int,
    petName: String,
    onPicked: (Int) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    IntervalScreen(minutes, petName, onPick = { picking = true }, onContinue, onSkip, modifier)
    if (picking) {
        PetSheet(onDismiss = { picking = false }) {
            IntervalSheetContent(
                initial = minutes,
                onDone = {
                    onPicked(it)
                    picking = false
                },
            )
        }
    }
}

/** One question: how long before a break? (CLAUDE.md §5.1, §6.2). Picked in a bottom sheet. */
@Composable
fun IntervalScreen(
    minutes: Int,
    petName: String,
    onPick: () -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingQuestion(
        title = stringResource(R.string.interval_title),
        body = stringResource(R.string.interval_body),
        step = 4,
        continueEnabled = true,
        onContinue = onContinue,
        onSkip = onSkip,
        modifier = modifier,
    ) {
        PetView(energy = 1f, mood = Mood.HAPPY, spec = PetSpecs.Bun, name = petName, modifier = Modifier.size(S.IntervalPetSize))
        PillCard(modifier = Modifier.fillMaxWidth(), onClick = onPick) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.interval_every),
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                    modifier = Modifier.weight(1f),
                )
                Text(stringResource(R.string.interval_minutes, minutes), style = MaterialTheme.typography.titleLarge, color = Ink)
            }
        }
    }
}

@Composable
internal fun IntervalSheetContent(initial: Int, onDone: (Int) -> Unit, modifier: Modifier = Modifier) {
    var minutes by rememberSaveable { mutableIntStateOf(initial) }
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = S.SheetHorizontal, end = S.SheetHorizontal, bottom = S.SheetBottom),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(S.SheetGap),
    ) {
        Text(stringResource(R.string.interval_sheet), style = MaterialTheme.typography.titleLarge, color = Ink, textAlign = TextAlign.Center)
        Text(stringResource(R.string.interval_minutes, minutes), style = MaterialTheme.typography.displayMedium, color = Ink)
        Slider(
            value = minutes.toFloat(),
            onValueChange = { minutes = it.roundToInt() },
            valueRange = SittingSettings.MIN_INTERVAL_MINUTES.toFloat()..SittingSettings.MAX_INTERVAL_MINUTES.toFloat(),
            steps = SliderSteps,
            colors = SliderDefaults.colors(
                thumbColor = Ink,
                activeTrackColor = Ink,
                activeTickColor = Track,
                inactiveTrackColor = Track,
                inactiveTickColor = InkEdge,
            ),
        )
        ChunkyButton(text = stringResource(R.string.sheet_done), onClick = { onDone(minutes) }, modifier = Modifier.fillMaxWidth())
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun IntervalPreview() {
    WigglebunTheme { IntervalScreen(SittingSettings.DEFAULT_INTERVAL_MINUTES, "Bun", {}, {}, {}) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun IntervalSheetPreview() {
    SheetStillFrame(
        screen = { IntervalScreen(SittingSettings.DEFAULT_INTERVAL_MINUTES, "Bun", {}, {}, {}) },
        sheet = { IntervalSheetContent(SittingSettings.DEFAULT_INTERVAL_MINUTES, onDone = {}) },
    )
}

@Preview(widthDp = 360, heightDp = 640, fontScale = 1.3f)
@Composable
internal fun IntervalSmallLargeFontPreview() {
    WigglebunTheme { IntervalScreen(SittingSettings.DEFAULT_INTERVAL_MINUTES, "Bun", {}, {}, {}) }
}
