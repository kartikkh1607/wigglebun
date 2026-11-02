package com.kartikkhandelwal.wigglebun.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.domain.model.MAX_PET_NAME_LENGTH
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButton
import com.kartikkhandelwal.wigglebun.ui.components.PillCard
import com.kartikkhandelwal.wigglebun.ui.pet.PetSpecs
import com.kartikkhandelwal.wigglebun.ui.pet.PetView
import com.kartikkhandelwal.wigglebun.ui.theme.CheckIdle
import com.kartikkhandelwal.wigglebun.ui.theme.Cream
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.InkMuted
import com.kartikkhandelwal.wigglebun.ui.theme.Sun
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme

private val S = Dimens.Onboarding

/** One question: what's the pet called? An empty field keeps its own name, the same as Skip. */
@Composable
fun NamePetScreen(
    name: String,
    onNameChange: (String) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val defaultName = stringResource(PetSpecs.Bun.defaultName)
    Column(
        modifier
            .fillMaxSize()
            .background(Cream)
            .systemBarsPadding()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SkipRow(color = Ink, onSkip = onSkip)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(S.NamePetTop))
            PetView(
                energy = 1f,
                mood = Mood.HAPPY,
                spec = PetSpecs.Bun,
                name = name.ifBlank { defaultName },
                modifier = Modifier.size(S.NamePetSize),
            )
            Spacer(Modifier.height(S.NameTextTop))
            Column(
                Modifier.fillMaxWidth().padding(horizontal = S.TextHorizontal),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(S.TextGap),
            ) {
                Text(
                    stringResource(R.string.name_title),
                    style = MaterialTheme.typography.displayMedium,
                    color = Ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    stringResource(R.string.name_body, MAX_PET_NAME_LENGTH),
                    style = MaterialTheme.typography.bodyLarge,
                    color = InkMuted,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(S.NameFieldTop))
            NameField(name, defaultName, onNameChange, onContinue, Modifier.padding(horizontal = S.NameFieldHorizontal))
        }
        ChunkyButton(
            text = stringResource(R.string.onboarding_continue),
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().padding(horizontal = S.NameButtonHorizontal, vertical = S.CtaBottomShort),
        )
        StepDots(step = 2, active = Sun, inactive = CheckIdle)
        Spacer(Modifier.height(S.DotsBottomShort))
    }
}

@Composable
private fun NameField(
    name: String,
    placeholder: String,
    onNameChange: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.name_field_label)
    val textStyle = MaterialTheme.typography.titleLarge.copy(color = Ink)
    PillCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(S.NameFieldPadding)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = name,
                onValueChange = onNameChange,
                singleLine = true,
                textStyle = textStyle,
                cursorBrush = SolidColor(Ink),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                modifier = Modifier.weight(1f).semantics { contentDescription = label },
                decorationBox = { field ->
                    if (name.isEmpty()) Text(placeholder, style = textStyle, color = InkMuted)
                    field()
                },
            )
            Spacer(Modifier.width(S.NameCounterGap))
            Text(
                stringResource(R.string.name_counter, name.length, MAX_PET_NAME_LENGTH),
                style = MaterialTheme.typography.labelMedium,
                color = InkMuted,
            )
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun NamePetEmptyPreview() {
    WigglebunTheme { NamePetScreen("", {}, {}, {}) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun NamePetTypedPreview() {
    WigglebunTheme { NamePetScreen("Biscuit", {}, {}, {}) }
}

@Preview(widthDp = 360, heightDp = 640, fontScale = 1.3f)
@Composable
internal fun NamePetSmallLargeFontPreview() {
    WigglebunTheme { NamePetScreen("WWWWWWWWWWWW", {}, {}, {}) }
}
