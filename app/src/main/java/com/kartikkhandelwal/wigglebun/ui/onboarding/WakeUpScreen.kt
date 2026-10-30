package com.kartikkhandelwal.wigglebun.ui.onboarding

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButton
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButtonStyle
import com.kartikkhandelwal.wigglebun.ui.pet.PetSpecs
import com.kartikkhandelwal.wigglebun.ui.pet.PetView
import com.kartikkhandelwal.wigglebun.ui.pet.Poses
import com.kartikkhandelwal.wigglebun.ui.theme.CardEdge
import com.kartikkhandelwal.wigglebun.ui.theme.CheckIdle
import com.kartikkhandelwal.wigglebun.ui.theme.Cream
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.InkMuted
import com.kartikkhandelwal.wigglebun.ui.theme.Night
import com.kartikkhandelwal.wigglebun.ui.theme.NightBody
import com.kartikkhandelwal.wigglebun.ui.theme.NightFloor
import com.kartikkhandelwal.wigglebun.ui.theme.NightLine
import com.kartikkhandelwal.wigglebun.ui.theme.NightLink
import com.kartikkhandelwal.wigglebun.ui.theme.NightMoon
import com.kartikkhandelwal.wigglebun.ui.theme.NightStar
import com.kartikkhandelwal.wigglebun.ui.theme.Sun
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme
import kotlinx.coroutines.delay

private val S = Dimens.Onboarding

/** Night turns to day as the pet wakes. */
private const val DAYBREAK_MILLIS = 800

/** How long the pet holds its big morning stretch before settling. */
private const val STRETCH_MILLIS = 1_600L

/** The mockup's artboard, which the moon and stars are positioned in. */
private const val ARTBOARD_WIDTH = 390f

/** x, y, radius. As in WakeUp.dc.html, moved clear of the text. */
private val Stars = listOf(
    Triple(60f, 96f, 2f),
    Triple(210f, 62f, 2.5f),
    Triple(26f, 190f, 1.8f),
    Triple(366f, 236f, 1.6f),
    Triple(44f, 306f, 2f),
)

/** The mockup's moon, 32 units higher so it clears the title. */
private const val MOON_PATH = "M300 118 A34 34 0 1 1 330 78 A26 26 0 1 0 300 118 Z"

enum class WakePhase {
    ASLEEP,

    /** Just tapped: the big stretch. */
    STRETCHING,

    /** Settled, and has said hello. */
    AWAKE,
}

@Composable
fun WakeUpScreenRoute(petName: String, onContinue: () -> Unit, onSkip: () -> Unit, modifier: Modifier = Modifier) {
    // The phase is only this screen's animation, and Back from naming should find the pet awake.
    var phase by rememberSaveable { mutableStateOf(WakePhase.ASLEEP) }
    LaunchedEffect(phase) {
        if (phase == WakePhase.STRETCHING) {
            delay(STRETCH_MILLIS)
            phase = WakePhase.AWAKE
        }
    }
    WakeUpScreen(
        phase = phase,
        petName = petName,
        onWake = { phase = WakePhase.STRETCHING },
        onContinue = onContinue,
        onSkip = onSkip,
        modifier = modifier,
    )
}

/** The first thing a new user sees (CLAUDE.md §6.2, design/mockups_source/WakeUp.dc.html). */
@Composable
fun WakeUpScreen(
    phase: WakePhase,
    petName: String,
    onWake: () -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val asleep = phase == WakePhase.ASLEEP
    StatusBarIcons(dark = asleep)
    val background by animatedColor(asleep, Night, Cream)
    val nightAlpha by animateFloatAsState(if (asleep) 1f else 0f, tween(DAYBREAK_MILLIS, easing = FastOutSlowInEasing), label = "nightAlpha")
    val moonPath = remember { PathParser().parsePathString(MOON_PATH).toPath() }

    BoxWithConstraints(modifier.fillMaxSize().background(background)) {
        val tall = maxHeight >= S.TallScreen
        val ringBox = if (tall) S.RingBox else minOf(S.RingBox, maxHeight * S.RingBoxShortFraction)
        // On a short screen the text moves up to where the sky is, so there's no room for it.
        if (tall && nightAlpha > 0f) {
            Canvas(Modifier.fillMaxSize()) {
                scale(size.width / ARTBOARD_WIDTH, pivot = Offset.Zero) {
                    Stars.forEach { (x, y, r) -> drawCircle(NightStar, r, Offset(x, y), alpha = nightAlpha) }
                    drawPath(moonPath, NightMoon, alpha = nightAlpha)
                }
            }
        }
        Column(Modifier.fillMaxSize().systemBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            SkipRow(color = animatedColor(asleep, NightLink, Ink).value, onSkip = onSkip)
            // Everything above the button scrolls if a large font needs it; Skip and the button stay.
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(if (tall) S.TitleTop else S.TitleTopShort))
                WakeText(phase)
                Spacer(Modifier.height(if (tall) S.RingTop else S.RingTopShort))
                PetRing(phase, petName, ringBox, nightAlpha, onWake)
            }
            WakeButton(asleep, onWake, onContinue)
            Spacer(Modifier.height(if (tall) S.CtaBottom else S.CtaBottomShort))
            StepDots(step = 1, active = Sun, inactive = animatedColor(asleep, NightLine, CheckIdle).value)
            Spacer(Modifier.height(if (tall) S.DotsBottom else S.DotsBottomShort))
        }
    }
}

@Composable
private fun animatedColor(asleep: Boolean, night: Color, day: Color) =
    animateColorAsState(if (asleep) night else day, tween(DAYBREAK_MILLIS, easing = FastOutSlowInEasing), label = "dayColor")

@Composable
private fun WakeText(phase: WakePhase) {
    val titleStyle = MaterialTheme.typography.displayMedium.copy(fontSize = S.TitleSize)
    val bodyStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = S.BodySize, lineHeight = S.BodyLineHeight)
    Crossfade(phase == WakePhase.ASLEEP, label = "wakeText") { asleep ->
        Column(
            Modifier.fillMaxWidth().padding(horizontal = S.TextHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(S.TextGap),
        ) {
            if (asleep) {
                Text(
                    stringResource(R.string.wake_title),
                    style = titleStyle,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Text(stringResource(R.string.wake_body), style = bodyStyle, color = NightBody, textAlign = TextAlign.Center)
            } else {
                // Two lines, like the sleeping title, so the pet doesn't jump when it wakes. No name
                // here: the next screen is where the user chooses it.
                Text(
                    stringResource(R.string.intro_title),
                    style = titleStyle,
                    color = Ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Text(stringResource(R.string.intro_body), style = bodyStyle, color = InkMuted, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun PetRing(phase: WakePhase, petName: String, box: Dp, nightAlpha: Float, onWake: () -> Unit) {
    val floor by animatedColor(phase == WakePhase.ASLEEP, NightFloor, CardEdge)
    val tapToWake = stringResource(R.string.wake_cta)
    Box(
        Modifier
            .size(box)
            .drawBehind {
                drawOval(
                    color = floor,
                    topLeft = Offset(
                        center.x - size.width * S.FloorRadiusX,
                        size.height * (S.FloorCentreY - S.FloorRadiusY),
                    ),
                    size = Size(size.width * S.FloorRadiusX * 2, size.height * S.FloorRadiusY * 2),
                )
            }
            .then(if (phase == WakePhase.ASLEEP) Modifier.clickable(onClickLabel = tapToWake, onClick = onWake) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(box * S.DashedRingInBox)) {
            val dash = S.RingDash.toPx()
            drawCircle(
                color = NightLine,
                alpha = nightAlpha,
                style = Stroke(width = S.RingStroke.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))),
            )
        }
        PetView(
            energy = 1f,
            mood = when (phase) {
                WakePhase.ASLEEP -> Mood.SLEEPING
                WakePhase.STRETCHING -> Mood.STRETCH
                WakePhase.AWAKE -> Mood.HAPPY
            },
            spec = PetSpecs.Bun,
            name = petName,
            pose = if (phase == WakePhase.STRETCHING) Poses.ReachUp else null,
            modifier = Modifier.size(box * S.PetInRing),
        )
    }
}

@Composable
private fun WakeButton(asleep: Boolean, onWake: () -> Unit, onContinue: () -> Unit) {
    ChunkyButton(
        onClick = if (asleep) onWake else onContinue,
        style = if (asleep) ChunkyButtonStyle.Highlight else ChunkyButtonStyle.Primary,
        modifier = Modifier.padding(horizontal = S.TextHorizontal).widthIn(min = S.CtaMinWidth).heightIn(min = S.CtaMinHeight),
    ) {
        if (asleep) {
            Icon(painterResource(R.drawable.ic_tap), contentDescription = null, modifier = Modifier.size(S.CtaIcon))
            Spacer(Modifier.size(S.CtaIconGap))
        }
        Text(stringResource(if (asleep) R.string.wake_cta else R.string.intro_cta), textAlign = TextAlign.Center)
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun WakeUpAsleepPreview() {
    WigglebunTheme { WakeUpScreen(WakePhase.ASLEEP, "Bun", {}, {}, {}) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun WakeUpStretchingPreview() {
    WigglebunTheme { WakeUpScreen(WakePhase.STRETCHING, "Bun", {}, {}, {}) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun WakeUpAwakePreview() {
    WigglebunTheme { WakeUpScreen(WakePhase.AWAKE, "Bun", {}, {}, {}) }
}

@Preview(widthDp = 360, heightDp = 640, fontScale = 1.3f)
@Composable
internal fun WakeUpSmallLargeFontPreview() {
    WigglebunTheme { WakeUpScreen(WakePhase.ASLEEP, "Bun", {}, {}, {}) }
}
