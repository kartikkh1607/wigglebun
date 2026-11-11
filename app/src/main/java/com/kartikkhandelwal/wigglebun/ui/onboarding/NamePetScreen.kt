package com.kartikkhandelwal.wigglebun.ui.onboarding

import com.kartikkhandelwal.wigglebun.feedback.FeelSound
import com.kartikkhandelwal.wigglebun.feedback.tickClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
import com.kartikkhandelwal.wigglebun.ui.components.SheetPanel
import com.kartikkhandelwal.wigglebun.ui.components.SkipPill
import com.kartikkhandelwal.wigglebun.ui.components.SpeechBubble
import com.kartikkhandelwal.wigglebun.ui.components.StepDots
import com.kartikkhandelwal.wigglebun.ui.home.RoomScene
import com.kartikkhandelwal.wigglebun.ui.pet.PetSpecs
import com.kartikkhandelwal.wigglebun.ui.pet.PetView
import com.kartikkhandelwal.wigglebun.ui.theme.Card
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.InkMuted
import com.kartikkhandelwal.wigglebun.ui.theme.NameChipLilac
import com.kartikkhandelwal.wigglebun.ui.theme.NameChipMint
import com.kartikkhandelwal.wigglebun.ui.theme.NameChipPink
import com.kartikkhandelwal.wigglebun.ui.theme.NameChipYellow
import com.kartikkhandelwal.wigglebun.ui.theme.NameFieldEdge
import com.kartikkhandelwal.wigglebun.ui.theme.OnSurface
import com.kartikkhandelwal.wigglebun.ui.theme.Surfaces
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunShapes
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTextStyles
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme

private val S = Dimens.Onboarding

/** The names offered under the field, each on its own tint (Name.dc.html). */
private data class Suggestion(val label: Int, val tint: Color)

private val Suggestions = listOf(
    Suggestion(R.string.name_suggestion_mochi, NameChipYellow),
    Suggestion(R.string.name_suggestion_peanut, NameChipMint),
    Suggestion(R.string.name_suggestion_biscuit, NameChipLilac),
    Suggestion(R.string.name_suggestion_noodle, NameChipPink),
)

/**
 * The pet says hello and asks to be named (CLAUDE.md §6.2, design/ui_v2/Name.dc.html): the room
 * and the pet with its speech bubble on top, the question and the field on a white panel. An
 * empty field keeps the pet's own name, the same as Skip.
 */
@Composable
fun NamePetScreen(
    name: String,
    onNameChange: (String) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val defaultName = stringResource(PetSpecs.Bun.defaultName)
    // A big font needs the question more than the room: the room shrinks, the bubble keeps one line
    // and the button scrolls with the panel, so the title and field are on screen without scrolling.
    val fontScale = LocalDensity.current.fontScale
    val shrink = (S.NameLargeFontScale / fontScale).coerceAtMost(1f)
    val roomScale = (shrink * shrink).coerceAtLeast(S.NameMinRoomScale)
    val buttonInPanel = fontScale > S.NameLargeFontScale
    val cta: @Composable () -> Unit = {
        ChunkyButton(
            text = stringResource(R.string.name_cta),
            onClick = onContinue,
            sound = FeelSound.TapSoft,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    OnSurface(Surfaces.Home) {
        Box(modifier.fillMaxSize().background(Card)) {
            Column(Modifier.fillMaxSize().imePadding()) {
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val viewport = maxHeight
                    // Room and panel scroll together when a big font or the keyboard leaves little room.
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        Room(petName = name.ifBlank { defaultName }, roomScale = roomScale, hello = fontScale < S.NameHugeFontScale)
                        SheetPanel(Modifier.fillMaxWidth().heightIn(min = viewport - S.NamePanelTop * roomScale)) {
                            StepDots(step = 1, count = ONBOARDING_STEPS)
                            Column(verticalArrangement = Arrangement.spacedBy(S.NameTextGap)) {
                                Text(
                                    stringResource(R.string.name_title),
                                    style = MaterialTheme.typography.displayMedium.copy(fontSize = S.NameTitleSize),
                                    color = Ink,
                                    modifier = Modifier.semantics { heading() },
                                )
                                Text(
                                    stringResource(R.string.name_body, MAX_PET_NAME_LENGTH),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = InkMuted,
                                )
                            }
                            NameField(name, defaultName, onNameChange, onContinue)
                            Suggestions(onNameChange)
                            if (buttonInPanel) Box(Modifier.padding(bottom = S.NameCtaBottom)) { cta() }
                        }
                    }
                }
                // Part of the panel: white to the bottom of the screen, under the system bar.
                if (!buttonInPanel) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(Card)
                            .navigationBarsPadding()
                            .padding(start = S.NameCtaHorizontal, end = S.NameCtaHorizontal, top = S.NameCtaTop, bottom = S.NameCtaBottom),
                    ) { cta() }
                }
            }
            Box(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = S.SkipRowTop, end = S.SkipRowHorizontal)) {
                SkipPill(onClick = onSkip)
            }
        }
    }
}

/**
 * The day room with the pet on its rug and the greeting above. The room grows with the greeting
 * (a big font), so the pet always stands on the rug.
 */
@Composable
private fun Room(petName: String, roomScale: Float, hello: Boolean) {
    Box(Modifier.fillMaxWidth().background(Surfaces.Home.background)) {
        Column {
            Box(Modifier.fillMaxWidth().heightIn(min = S.NameRoomHeight * roomScale)) {
                RoomScene(Modifier.matchParentSize(), showClockAndShelf = false, roomScale = roomScale)
                Column(
                    Modifier.fillMaxWidth().padding(bottom = S.NamePetFloorGap * roomScale),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(if (roomScale < 1f) S.NameBubbleTopBelowSkip else S.NameBubbleTop))
                    SpeechBubble(Modifier.widthIn(max = S.NameBubbleWidth)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(R.string.intro_title),
                                style = WigglebunTextStyles.Bubble.copy(fontSize = S.NameBubbleTitleSize),
                                color = Ink,
                                textAlign = TextAlign.Center,
                            )
                            // Left out at the biggest fonts, where it would crowd the pet and the question.
                            if (hello) {
                                Text(
                                    stringResource(R.string.intro_body),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = InkMuted,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(S.NameBubbleToPet * roomScale))
                    PetView(energy = 1f, mood = Mood.HAPPY, spec = PetSpecs.Bun, name = petName, modifier = Modifier.size(S.NamePetSize * roomScale))
                }
            }
            // The floor carries on to where the panel starts.
            Spacer(Modifier.height((S.NamePanelTop - S.NameRoomHeight) * roomScale))
        }
    }
}

@Composable
private fun NameField(name: String, placeholder: String, onNameChange: (String) -> Unit, onDone: () -> Unit) {
    val label = stringResource(R.string.name_field_label)
    val textStyle = MaterialTheme.typography.titleLarge.copy(color = Ink)
    Column(verticalArrangement = Arrangement.spacedBy(S.NameFieldGap)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = InkMuted)
        Row(
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    val outline = WigglebunShapes.SmallButton.createOutline(size, layoutDirection, this)
                    translate(top = S.NameFieldEdge.toPx()) { drawOutline(outline, NameFieldEdge) }
                }
                .background(Card, WigglebunShapes.SmallButton)
                .border(S.NameFieldBorder, Ink, WigglebunShapes.SmallButton)
                .heightIn(min = S.NameFieldHeight)
                .padding(horizontal = S.NameFieldPaddingHorizontal),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(S.NameCounterGap),
        ) {
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
            Text(
                stringResource(R.string.name_counter, name.length, MAX_PET_NAME_LENGTH),
                style = MaterialTheme.typography.labelMedium,
                color = InkMuted,
            )
        }
    }
}

/** Tapping one fills the field; the user can still change it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Suggestions(onPick: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(S.NameChipGap)) {
        Suggestions.forEach { suggestion ->
            val text = stringResource(suggestion.label)
            // The chip is smaller than a finger: the touch target around it is 48 dp.
            Box(
                Modifier.tickClickable { onPick(text) }.minimumInteractiveComponentSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text,
                    style = MaterialTheme.typography.titleSmall,
                    color = Ink,
                    modifier = Modifier
                        .clip(WigglebunShapes.Pill)
                        .background(suggestion.tint)
                        .padding(horizontal = S.NameChipPaddingHorizontal, vertical = S.NameChipPaddingVertical),
                )
            }
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun NamePetEmptyPreview() {
    WigglebunTheme { NamePetScreen("", {}, {}, {}) }
}

/** The state Name.dc.html draws: "Bun" typed, 3/12. */
@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun NamePetMockupPreview() {
    WigglebunTheme { NamePetScreen("Bun", {}, {}, {}) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun NamePetTypedPreview() {
    WigglebunTheme { NamePetScreen("Biscuit", {}, {}, {}) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun NamePetLongNamePreview() {
    WigglebunTheme { NamePetScreen("WWWWWWWWWWWW", {}, {}, {}) }
}

@Preview(widthDp = 360, heightDp = 640, fontScale = 1.3f)
@Composable
internal fun NamePetSmallLargeFontPreview() {
    WigglebunTheme { NamePetScreen("WWWWWWWWWWWW", {}, {}, {}) }
}

@Preview(widthDp = 360, heightDp = 640, fontScale = 2f)
@Composable
internal fun NamePetSmallHugeFontPreview() {
    WigglebunTheme { NamePetScreen("WWWWWWWWWWWW", {}, {}, {}) }
}
