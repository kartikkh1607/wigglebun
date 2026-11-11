package com.kartikkhandelwal.wigglebun.ui.breaksession

import com.kartikkhandelwal.wigglebun.feedback.ConsumeFeel
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.data.repo.BreakDoneResult
import com.kartikkhandelwal.wigglebun.domain.model.ActivePet
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.PetId
import com.kartikkhandelwal.wigglebun.domain.rewards.Milestone
import com.kartikkhandelwal.wigglebun.domain.rewards.Streak
import com.kartikkhandelwal.wigglebun.domain.session.SessionTimer
import com.kartikkhandelwal.wigglebun.ui.motion.LocalReducedMotion
import com.kartikkhandelwal.wigglebun.ui.pet.PetSpecs
import com.kartikkhandelwal.wigglebun.ui.pet.PetView
import com.kartikkhandelwal.wigglebun.ui.pet.displayName
import com.kartikkhandelwal.wigglebun.ui.pet.spec
import com.kartikkhandelwal.wigglebun.ui.components.SheetPanel
import com.kartikkhandelwal.wigglebun.ui.theme.Card
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.OnSurface
import com.kartikkhandelwal.wigglebun.ui.theme.Surfaces
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme
import kotlinx.coroutines.delay
import java.time.LocalDate

private val S = Dimens.BreakSession

/** The celebration lasts 3 s (CLAUDE.md §5.2), then the session returns to Home by itself. */
private const val CELEBRATE_MILLIS = 3_000L

/** Ring and countdown opacity while paused. */
private const val PAUSED_ALPHA = 0.4f
private const val PAUSE_FADE_MILLIS = 200

@Composable
fun BreakSessionScreenRoute(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    /** After the celebration. Close and Back use [onExit]; onboarding tells the two apart. */
    onCompleted: () -> Unit = onExit,
    viewModel: BreakSessionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ConsumeFeel(viewModel.feel)
    // Ignores a second exit (Continue tapped as the auto-return fires).
    val exit = dropUnlessResumed(block = onExit)
    val complete = dropUnlessResumed(block = onCompleted)
    val close = {
        viewModel.close()
        exit()
    }
    BackHandler(onBack = close)
    // Pause when the app goes to the background (the 2-min walk keeps going: see onBackground).
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onBackground() }

    val celebrating = state.result != null
    val accessibilityManager = LocalAccessibilityManager.current
    LaunchedEffect(celebrating) {
        if (!celebrating) return@LaunchedEffect
        // With TalkBack (touch exploration) on this is Long.MAX_VALUE: no time limit, Continue
        // only. It also honours the user's "time to take action" accessibility setting.
        val timeout = accessibilityManager?.calculateRecommendedTimeoutMillis(
            originalTimeoutMillis = CELEBRATE_MILLIS,
            containsIcons = true,
            containsText = true,
            containsControls = true,
        ) ?: CELEBRATE_MILLIS
        if (timeout == Long.MAX_VALUE) return@LaunchedEffect
        delay(timeout)
        complete()
    }

    BreakSessionScreen(
        state = state,
        // Keep the screen on only while the countdown runs.
        modifier = if (state.keepScreenOn) modifier.keepScreenOn() else modifier,
        onClose = close,
        onTogglePause = viewModel::togglePause,
        onNext = viewModel::nextStep,
        onDone = viewModel::done,
        onContinue = complete,
    )
}

/** Full-screen guided break (CLAUDE.md §6.4, design/ui_v2/Stretch.dc.html) and its celebration (Celebrate.dc.html). */
@Composable
fun BreakSessionScreen(
    state: BreakSessionUiState,
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
    onTogglePause: () -> Unit = {},
    onNext: () -> Unit = {},
    onDone: () -> Unit = {},
    onContinue: () -> Unit = {},
) {
    val pet = state.pet
    val petName = pet?.displayName().orEmpty()
    val result = state.result
    val surface = if (result == null) Surfaces.Session else Surfaces.HomeWall
    OnSurface(surface) {
        Box(modifier.fillMaxSize().background(surface.background)) {
            if (result == null) {
                SessionContent(state, petName, onClose, onTogglePause, onNext, onDone)
            } else {
                CelebrationContent(
                    result = result,
                    pet = pet?.spec ?: PetSpecs.Bun,
                    petName = petName,
                    nextPet = state.nextPet,
                    onContinue = onContinue,
                    accessories = pet?.wearing.orEmpty(),
                )
            }
        }
    }
}

@Composable
private fun SessionContent(
    state: BreakSessionUiState,
    petName: String,
    onClose: () -> Unit,
    onTogglePause: () -> Unit,
    onNext: () -> Unit,
    onDone: () -> Unit,
) {
    val stepLabel = stringResource(R.string.session_step, state.stepIndex + 1, state.stepCount)
    // Paused reads at a glance: ring and countdown dim and the chip says "Paused".
    val timerAlpha by animateFloatAsState(
        targetValue = if (state.isPaused) PAUSED_ALPHA else 1f,
        animationSpec = tween(PAUSE_FADE_MILLIS, easing = FastOutSlowInEasing),
        label = "timerAlpha",
    )
    val top: @Composable () -> Unit = {
        Spacer(Modifier.height(S.TopPadding))
        SessionHeader(state.type, onClose)
        StepBars(
            count = state.stepCount,
            currentIndex = state.stepIndex,
            currentProgress = state.progress,
            modifier = Modifier.padding(top = S.SegmentsTop, start = S.SegmentsHorizontal, end = S.SegmentsHorizontal),
        )
    }
    val ring: @Composable (Modifier) -> Unit = { ringModifier ->
        CountdownRing(progress = state.progress, ringAlpha = timerAlpha, modifier = ringModifier) {
            state.pet?.let { pet ->
                PetView(
                    // The colour fills over the whole session, so each step sees it come back.
                    energy = state.energy,
                    mood = state.mood,
                    spec = pet.spec,
                    name = petName,
                    // With animations removed each step holds one pose instead of cycling.
                    pose = if (LocalReducedMotion.current) state.stillPose else state.pose,
                    accessories = pet.wearing,
                    modifier = Modifier
                        .fillMaxSize(S.PetInRing)
                        // The pet's box has its feet low; lift it so the pet sits in the middle of the ring.
                        .graphicsLayer { translationY = -size.height * S.PetLift },
                )
            }
        }
    }
    val timer: @Composable (Modifier) -> Unit = { timerModifier ->
        TimerRow(
            remainingMillis = state.remainingMillis,
            isPaused = state.isPaused,
            colourIsBack = state.colourIsBack,
            showColourChip = state.showColourChip,
            timerAlpha = timerAlpha,
            modifier = timerModifier,
        )
    }
    val text: @Composable () -> Unit = {
        InstructionText(
            stepLabel = stepLabel,
            title = stringResource(state.title),
            instruction = stringResource(state.instruction),
            showSafetyNote = state.showSafetyNote,
        )
    }
    val buttons: @Composable (Modifier) -> Unit = { buttonsModifier ->
        SessionButtons(
            isPaused = state.isPaused,
            isLastStep = state.isLastStep,
            isFinished = state.isFinished,
            onTogglePause = onTogglePause,
            onNext = onNext,
            onDone = onDone,
            modifier = buttonsModifier,
        )
    }
    if (LocalDensity.current.fontScale > S.LargeFontScale) {
        // A big font leaves no room for the ring and the text together: they scroll as one, above the
        // pinned buttons, so the whole instruction can be read.
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Column { top() }
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(S.RingTop))
                val ringScale = if (LocalDensity.current.fontScale >= S.HugeFontScale) S.HugeFontRingScale else S.LargeFontRingScale
                ring(Modifier.size(S.RingSize * ringScale))
                timer(Modifier.padding(top = S.TimerGap))
                Spacer(Modifier.height(S.TimerGap))
                SheetPanel(Modifier.fillMaxWidth().heightIn(min = S.TextMinHeight)) { text() }
            }
            Column(Modifier.fillMaxWidth().background(Card).navigationBarsPadding()) {
                buttons(Modifier.padding(start = Dimens.Shared.PanelPaddingHorizontal, end = Dimens.Shared.PanelPaddingHorizontal, top = S.ButtonsTop, bottom = S.ButtonsBottom))
            }
        }
        return
    }
    SessionLayout(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.sessionSlot(SessionSlot.Top)) { top() }
        ring(Modifier.sessionSlot(SessionSlot.Ring))
        timer(Modifier.sessionSlot(SessionSlot.Timer))
        SheetPanel(Modifier.sessionSlot(SessionSlot.Panel).fillMaxWidth()) {
            // Scrolls when a big font leaves less room than the text needs; the buttons stay put.
            Column(Modifier.weight(1f, fill = false).heightIn(min = S.TextMinHeight).verticalScroll(rememberScrollState())) { text() }
            // White to the bottom of the screen, under the system bar.
            buttons(Modifier.padding(top = S.ButtonsTop, bottom = S.ButtonsBottom).navigationBarsPadding())
        }
    }
}

/** Sample states for previews and screenshot tests, built through the real mapper. */
internal object BreakSessionPreviewStates {
    private const val MILLIS_PER_SECOND = 1_000L
    private const val MILLIS_PER_MINUTE = 60_000L
    private val today: LocalDate = LocalDate.of(2026, 10, 27) // Tuesday

    /** [step] (0-based) with [remainingSeconds] left; frozen at "now" = 0. */
    fun midStep(
        type: BreakType,
        step: Int,
        remainingSeconds: Int,
        paused: Boolean = false,
        result: BreakDoneResult? = null,
        nextPet: NextPetHint? = null,
    ): BreakSessionUiState {
        val durations = SessionTimer.evenSplit(type.durationMinutes * MILLIS_PER_MINUTE, type.sessionSteps().size)
        val timer = SessionTimer(
            stepDurationsMillis = durations,
            stepIndex = step,
            countedMillis = durations[step] - remainingSeconds * MILLIS_PER_SECOND,
            runningSinceMillis = if (paused) null else 0L,
        )
        return sessionUiState(type, pet = ActivePet.Default, timer = timer, now = 0L, result = result, nextPet = nextPet)
    }

    val StandUp = midStep(BreakType.STAND_UP, step = 0, remainingSeconds = 12)
    val Walk = midStep(BreakType.WALK, step = 0, remainingSeconds = 36)
    val Neck = midStep(BreakType.NECK, step = 1, remainingSeconds = 11)
    val Shoulders = midStep(BreakType.SHOULDERS, step = 0, remainingSeconds = 19)
    val Wrist = midStep(BreakType.WRIST, step = 0, remainingSeconds = 18)
    val Eyes = midStep(BreakType.EYES, step = 2, remainingSeconds = 8)
    val Water = midStep(BreakType.WATER, step = 0, remainingSeconds = 21)
    val Paused = midStep(BreakType.NECK, step = 1, remainingSeconds = 11, paused = true)
    val LastStepDone = midStep(BreakType.STAND_UP, step = 3, remainingSeconds = 0)

    /** First neck stretch of the day on the third work day in a row: +5 and the 3-day bonus. */
    val CelebrateWithBadge = midStep(
        BreakType.NECK,
        step = 3,
        remainingSeconds = 0,
        result = BreakDoneResult(
            type = BreakType.NECK,
            sparksEarned = BreakType.NECK.defaultReward,
            bonuses = listOf(Milestone(days = 3, bonus = 20)),
            streak = Streak(days = 3, startDate = today.minusDays(2), goal = 3, todayDone = 3, todayIsWorkDay = true),
        ),
        nextPet = NextPetHint(PetId.PIP, price = 75, affordable = false),
    )

    /** The first stretch on install day: +5 sparks, 1 of 3 breaks for the streak (design/ui_v2/Celebrate.dc.html). */
    val CelebrateFirst = midStep(
        BreakType.STAND_UP,
        step = 3,
        remainingSeconds = 0,
        result = BreakDoneResult(
            type = BreakType.STAND_UP,
            sparksEarned = BreakType.STAND_UP.defaultReward,
            bonuses = emptyList(),
            streak = Streak(days = 0, startDate = null, goal = 3, todayDone = 1, todayIsWorkDay = true),
        ),
        nextPet = NextPetHint(PetId.PIP, price = 75, affordable = false),
    )

    /** A repeat neck stretch: celebrates, but no sparks; 2 of 3 breaks done today. */
    val CelebrateNoBadge = midStep(
        BreakType.NECK,
        step = 3,
        remainingSeconds = 0,
        result = BreakDoneResult(
            type = BreakType.NECK,
            sparksEarned = 0,
            bonuses = emptyList(),
            streak = Streak(days = 2, startDate = today.minusDays(2), goal = 3, todayDone = 2, todayIsWorkDay = true),
        ),
    )
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun BreakSessionNeckPreview() {
    WigglebunTheme { BreakSessionScreen(BreakSessionPreviewStates.Neck) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun BreakSessionStandUpPreview() {
    WigglebunTheme { BreakSessionScreen(BreakSessionPreviewStates.StandUp) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun BreakSessionPausedPreview() {
    WigglebunTheme { BreakSessionScreen(BreakSessionPreviewStates.Paused) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun BreakSessionLastStepPreview() {
    WigglebunTheme { BreakSessionScreen(BreakSessionPreviewStates.LastStepDone) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun BreakSessionCelebratePreview() {
    WigglebunTheme { BreakSessionScreen(BreakSessionPreviewStates.CelebrateWithBadge) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun BreakSessionCelebrateNoBadgePreview() {
    WigglebunTheme { BreakSessionScreen(BreakSessionPreviewStates.CelebrateNoBadge) }
}

@Preview(widthDp = 360, heightDp = 640)
@Composable
private fun BreakSessionSmallPhonePreview() {
    WigglebunTheme { BreakSessionScreen(BreakSessionPreviewStates.StandUp) }
}

@Preview(widthDp = 360, heightDp = 640, fontScale = 1.3f)
@Composable
private fun BreakSessionSmallLargeFontPreview() {
    WigglebunTheme { BreakSessionScreen(BreakSessionPreviewStates.Neck) }
}

@Preview(widthDp = 360, heightDp = 640, fontScale = 2f)
@Composable
private fun BreakSessionSmallHugeFontPreview() {
    WigglebunTheme { BreakSessionScreen(BreakSessionPreviewStates.Neck) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun BreakSessionCelebrateFirstPreview() {
    WigglebunTheme { BreakSessionScreen(BreakSessionPreviewStates.CelebrateFirst) }
}

@Preview(widthDp = 360, heightDp = 640)
@Composable
private fun BreakSessionCelebrateSmallPhonePreview() {
    WigglebunTheme { BreakSessionScreen(BreakSessionPreviewStates.CelebrateFirst) }
}
