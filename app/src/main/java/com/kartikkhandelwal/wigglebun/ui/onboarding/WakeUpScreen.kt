package com.kartikkhandelwal.wigglebun.ui.onboarding

import com.kartikkhandelwal.wigglebun.feedback.LocalFeel
import com.kartikkhandelwal.wigglebun.feedback.FeelSound
import com.kartikkhandelwal.wigglebun.feedback.tickClickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButton
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButtonStyle
import com.kartikkhandelwal.wigglebun.ui.motion.LocalReducedMotion
import com.kartikkhandelwal.wigglebun.ui.pet.PetSpecs
import com.kartikkhandelwal.wigglebun.ui.pet.PetView
import com.kartikkhandelwal.wigglebun.ui.pet.Poses
import com.kartikkhandelwal.wigglebun.ui.theme.BreakEyes
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.NightBody
import com.kartikkhandelwal.wigglebun.ui.theme.NightLink
import com.kartikkhandelwal.wigglebun.ui.theme.NightPill
import com.kartikkhandelwal.wigglebun.ui.theme.NightPillEdge
import com.kartikkhandelwal.wigglebun.ui.theme.NightRoomFloor
import com.kartikkhandelwal.wigglebun.ui.theme.NightRoomStar
import com.kartikkhandelwal.wigglebun.ui.theme.PetBlush
import com.kartikkhandelwal.wigglebun.ui.theme.PetPink
import com.kartikkhandelwal.wigglebun.ui.theme.PetYellow
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme
import com.kartikkhandelwal.wigglebun.ui.theme.balanced
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

private val S = Dimens.Onboarding

/** How long the pet holds its big morning stretch before the naming screen takes over. */
private const val STRETCH_MILLIS = 1_600L

/** The sleeping blob's own drawing space (the mockup's viewBox is 200 x 170). */
private const val BLOB_VIEW_WIDTH = 200f

/** Height of [OnboardingTopBar] at normal font size: its top gap plus the Skip pill's touch target. */
private val SkipRowHeight = S.TopBarTop + S.TopBarButton

enum class WakePhase {
    ASLEEP,

    /** Just tapped: the big stretch, then the naming screen. */
    STRETCHING,
}

/**
 * The wake-up (design/ui_v2/WakeUp.dc.html). A tap starts the stretch; when it ends [onContinue]
 * opens the naming screen, where the pet says hello. With animations removed there is nothing to
 * watch, so the tap goes straight on. Nothing is kept: coming back from the next screen finds the
 * pet asleep again.
 */
@Composable
fun WakeUpScreenRoute(onContinue: () -> Unit, onSkip: () -> Unit, modifier: Modifier = Modifier) {
    var phase by remember { mutableStateOf(WakePhase.ASLEEP) }
    val next by rememberUpdatedState(onContinue)
    LaunchedEffect(phase) {
        if (phase == WakePhase.STRETCHING) {
            delay(STRETCH_MILLIS.milliseconds)
            next()
        }
    }
    val reducedMotion = LocalReducedMotion.current
    WakeUpScreen(
        phase = phase,
        onWake = { if (reducedMotion) onContinue() else phase = WakePhase.STRETCHING },
        onSkip = onSkip,
        modifier = modifier,
    )
}

/** The first thing a new user sees (CLAUDE.md §6.2). */
@Composable
fun WakeUpScreen(
    phase: WakePhase,
    onWake: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    // The system bars' heights; parameters so a screenshot can show a phone that has them.
    statusTop: Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
    navBottom: Dp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
) {
    val asleep = phase == WakePhase.ASLEEP
    // The room is dark, and the app forces dark status-bar icons otherwise.
    StatusBarIcons(dark = true)
    BoxWithConstraints(modifier.fillMaxSize().background(NightRoomFloor)) {
        // The mockup is 844 tall below the status bar; a shorter phone squeezes its positions.
        // The title, the line under it and the button must be whole on screen, so the scene gets what
        // they leave: everything above the title (556 units) and the button's bottom gap (44) scale
        // with it. The text is measured as it will be drawn (font size, wrapping, width), and the
        // navigation bar under the button is room the scene can't use.
        val textHeight = wakeTextHeight(maxWidth - S.WakeTextHorizontal * 2)
        // A big font wraps the button's label onto a second line, which makes the button taller too.
        val buttonHeight = wakeButtonHeight(maxWidth - S.WakeCtaHorizontal * 2)
        val room = maxHeight - statusTop - navBottom - textHeight - buttonHeight - S.WakeCtaPaddingShort
        val k = (room / (S.WakeTitleTop + S.WakeCtaBottom)).coerceIn(S.WakeMinSqueeze, 1f)
        NightRoomScene(Modifier.fillMaxSize(), squeeze = k, topInset = statusTop)
        Column(
            Modifier.fillMaxSize().padding(top = statusTop, bottom = navBottom),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OnboardingTopBar(onSkip = onSkip, skipFace = NightPill, skipText = NightLink, skipEdge = NightPillEdge)
            // Everything above the button scrolls if a large font needs it; Skip and the button stay.
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height((S.WakeBlobTop * k - SkipRowHeight).coerceAtLeast(S.TextGap)))
                Sleeper(asleep, k, onWake)
                Spacer(Modifier.height((S.WakeTitleTop - S.WakeBlobTop - S.WakeBlobHeight) * k))
                WakeText()
                // Scrolled to its end (a big font), the last line stops clear of the button.
                Spacer(Modifier.height(S.TextGap))
            }
            WakeButton(
                asleep,
                onWake,
                Modifier.padding(start = S.WakeCtaHorizontal, end = S.WakeCtaHorizontal, top = S.WakeCtaPaddingShort, bottom = S.WakeCtaBottom * k),
            )
        }
    }
}

@Composable
private fun wakeTitleStyle(): TextStyle = MaterialTheme.typography.displayMedium.copy(fontSize = S.WakeTitleSize).balanced()

@Composable
private fun wakeBodyStyle(): TextStyle =
    MaterialTheme.typography.bodyLarge.copy(fontSize = S.WakeBodySize, lineHeight = S.WakeBodyLineHeight).balanced()

/**
 * The height [WakeText] takes in [width], measured with the same styles, text and font scale, plus
 * the gap that keeps its last line clear of the button. Estimating it (as a share of a fixed
 * height) let the second line of the body slip behind the button on a tall phone.
 */
@Composable
private fun wakeTextHeight(width: Dp): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val title = stringResource(R.string.wake_title)
    val body = stringResource(R.string.wake_body)
    val titleStyle = wakeTitleStyle()
    val bodyStyle = wakeBodyStyle()
    return remember(measurer, density, width, title, body, titleStyle, bodyStyle) {
        val constraints = Constraints(maxWidth = with(density) { width.roundToPx() })
        val titleHeight = measurer.measure(title, titleStyle, constraints = constraints, density = density).size.height
        val bodyHeight = measurer.measure(body, bodyStyle, constraints = constraints, density = density).size.height
        with(density) { (titleHeight + bodyHeight).toDp() } + S.WakeTextGap + S.TextGap
    }
}

/** The button's height in [width] (its full width): the usual one, or taller when the label wraps. */
@Composable
private fun wakeButtonHeight(width: Dp): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val label = stringResource(R.string.wake_cta)
    val style = MaterialTheme.typography.labelLarge
    return remember(measurer, density, width, label, style) {
        // The label's room: the button's side padding, the hand icon and the gap after it.
        val labelWidth = width - Dimens.ButtonHorizontalPadding * 2 - S.WakeCtaIcon - S.WakeCtaIconGap
        val constraints = Constraints(maxWidth = with(density) { labelWidth.roundToPx() })
        val labelHeight = measurer.measure(label, style, constraints = constraints, density = density).size.height
        maxOf(S.WakeCtaHeight, with(density) { labelHeight.toDp() } + ChunkyButtonStyle.Highlight.edgeHeight)
    }
}

@Composable
private fun WakeText() {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = S.WakeTextHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(S.WakeTextGap),
    ) {
        Text(
            stringResource(R.string.wake_title),
            style = wakeTitleStyle(),
            color = NightRoomStar,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(R.string.wake_body),
            style = wakeBodyStyle(),
            color = NightBody,
            textAlign = TextAlign.Center,
        )
    }
}

/** The pet curled up as a round blob with its "z z"; a tap wakes it. Once awake it stretches. */
@Composable
private fun Sleeper(asleep: Boolean, k: Float, onWake: () -> Unit) {
    val tapToWake = stringResource(R.string.wake_cta)
    val description = stringResource(R.string.wake_blob_description)
    val feel = LocalFeel.current
    Box(
        Modifier
            .size(S.WakeBlobWidth * k, S.WakeBlobHeight * k)
            .then(
                if (asleep) {
                    Modifier.tickClickable(onClickLabel = tapToWake, onClick = { feel.play(FeelSound.BunChirpBright); onWake() }).semantics { contentDescription = description }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        if (asleep) {
            SleepingBlob(Modifier.fillMaxSize())
            // The sleeper's "z z": a big one and a smaller one a little higher (decoration, not read out).
            Row(
                Modifier
                    .align(Alignment.TopStart)
                    .offset((S.WakeZLeft - S.WakeBlobLeft) * k, (S.WakeZTop - S.WakeBlobTop) * k)
                    .clearAndSetSemantics {},
            ) {
                val z = stringResource(R.string.wake_z)
                Text(z, style = MaterialTheme.typography.displayMedium.copy(fontSize = S.WakeZSize), color = BreakEyes)
                Text(
                    z,
                    style = MaterialTheme.typography.displayMedium.copy(fontSize = S.WakeZSmallSize),
                    color = BreakEyes,
                    modifier = Modifier.offset(S.WakeZSmallOffsetX, S.WakeZSmallOffsetY),
                )
            }
        } else {
            // A full pet is taller than the blob, so it stands on the blob's floor line.
            PetView(
                energy = 1f,
                mood = Mood.STRETCH,
                spec = PetSpecs.Bun,
                name = stringResource(PetSpecs.Bun.defaultName),
                pose = Poses.ReachUp,
                modifier = Modifier.wrapContentSize(Alignment.BottomCenter, unbounded = true).size(S.WakeBlobWidth * k),
            )
        }
    }
}

/** Port of the mockup's sleeping blob (viewBox 200 x 170), in pet-body colours. */
@Composable
private fun SleepingBlob(modifier: Modifier = Modifier) {
    val smile = remember { PathParser().parsePathString("M98 116 Q100 118 102 116").toPath() }
    val eyes = remember { PathParser().parsePathString("M78 104 Q84 110 90 104 M110 104 Q116 110 122 104").toPath() }
    Canvas(modifier.clearAndSetSemantics {}) {
        scale(size.width / BLOB_VIEW_WIDTH, pivot = Offset.Zero) {
            fun oval(c: Color, cx: Float, cy: Float, rx: Float, ry: Float) =
                drawOval(c, Offset(cx - rx, cy - ry), Size(rx * 2, ry * 2))
            oval(PetYellow, 100f, 112f, 72f, 50f)
            drawCircle(PetYellow, 16f, Offset(58f, 72f))
            drawCircle(PetYellow, 16f, Offset(142f, 72f))
            oval(PetYellow, 54f, 150f, 14f, 9f)
            oval(PetYellow, 146f, 150f, 14f, 9f)
            oval(PetYellow, 100f, 72f, 56f, 20f)
            drawCircle(PetPink, 8f, Offset(58f, 73f))
            drawCircle(PetPink, 8f, Offset(142f, 73f))
            drawPath(eyes, Ink, style = Stroke(width = 3.6f, cap = StrokeCap.Round))
            oval(PetBlush, 70f, 116f, 9f, 5.5f)
            oval(PetBlush, 130f, 116f, 9f, 5.5f)
            drawPath(smile, Ink, style = Stroke(width = 3f, cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun WakeButton(asleep: Boolean, onWake: () -> Unit, modifier: Modifier = Modifier) {
    // Hidden, not gone, while the pet stretches: nothing moves and nothing can be tapped twice.
    ChunkyButton(
        onClick = onWake,
        sound = FeelSound.BunChirpBright, // the wake-up moment: Bun's first chirp
        enabled = asleep,
        style = ChunkyButtonStyle.Highlight,
        modifier = modifier.fillMaxWidth().heightIn(min = S.WakeCtaHeight).alpha(if (asleep) 1f else 0f),
    ) {
        Icon(painterResource(R.drawable.ic_tap), contentDescription = null, modifier = Modifier.size(S.WakeCtaIcon))
        Spacer(Modifier.size(S.WakeCtaIconGap))
        Text(stringResource(R.string.wake_cta), textAlign = TextAlign.Center)
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun WakeUpAsleepPreview() {
    WigglebunTheme { WakeUpScreen(WakePhase.ASLEEP, {}, {}) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun WakeUpStretchingPreview() {
    WigglebunTheme { WakeUpScreen(WakePhase.STRETCHING, {}, {}) }
}

/** A tall phone with a status bar and a 3-button navigation bar (about 411 x 914 dp). */
@Preview(widthDp = 411, heightDp = 914)
@Composable
internal fun WakeUpTallPhonePreview() {
    WigglebunTheme { WakeUpScreen(WakePhase.ASLEEP, {}, {}, statusTop = 32.dp, navBottom = 48.dp) }
}

@Preview(widthDp = 411, heightDp = 914, fontScale = 2f)
@Composable
internal fun WakeUpTallPhoneHugeFontPreview() {
    WigglebunTheme { WakeUpScreen(WakePhase.ASLEEP, {}, {}, statusTop = 32.dp, navBottom = 48.dp) }
}

@Preview(widthDp = 360, heightDp = 640)
@Composable
internal fun WakeUpSmallPhonePreview() {
    WigglebunTheme { WakeUpScreen(WakePhase.ASLEEP, {}, {}) }
}

@Preview(widthDp = 360, heightDp = 640, fontScale = 2f)
@Composable
internal fun WakeUpSmallHugeFontPreview() {
    WigglebunTheme { WakeUpScreen(WakePhase.ASLEEP, {}, {}) }
}
