package com.kartikkhandelwal.wigglebun.ui.breaksession

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.kartikkhandelwal.wigglebun.domain.rewards.Milestone
import com.kartikkhandelwal.wigglebun.domain.rewards.Streak
import com.kartikkhandelwal.wigglebun.domain.session.SessionTimer
import com.kartikkhandelwal.wigglebun.ui.labelRes
import com.kartikkhandelwal.wigglebun.ui.pet.PetSpecs
import com.kartikkhandelwal.wigglebun.ui.pet.PetView
import com.kartikkhandelwal.wigglebun.ui.pet.displayName
import com.kartikkhandelwal.wigglebun.ui.pet.spec
import com.kartikkhandelwal.wigglebun.ui.theme.BreakSky
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme
import kotlinx.coroutines.delay
import java.time.LocalDate

private val S = Dimens.BreakSession

/** The celebration lasts 3 s (CLAUDE.md §5.2), then the session returns to Home by itself. */
private const val CELEBRATE_MILLIS = 3_000L

/** Mockup: the 210 dp pet inside the 280 dp ring. */
private const val PET_IN_RING = 210f / 280f

/** Ring and countdown opacity while paused. */
private const val PAUSED_ALPHA = 0.4f
private const val PAUSE_FADE_MILLIS = 200

@Composable
fun BreakSessionScreenRoute(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BreakSessionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Ignores a second exit (Continue tapped as the auto-return fires).
    val exit = dropUnlessResumed(block = onExit)
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
        exit()
    }

    BreakSessionScreen(
        state = state,
        // Keep the screen on only while the countdown runs.
        modifier = if (state.keepScreenOn) modifier.keepScreenOn() else modifier,
        onClose = close,
        onTogglePause = viewModel::togglePause,
        onNext = viewModel::nextStep,
        onDone = viewModel::done,
        onContinue = exit,
    )
}

/** Full-screen guided break (CLAUDE.md §6.4, design/mockups_source/BreakSession.dc.html). */
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
    Box(
        modifier
            .fillMaxSize()
            .background(BreakSky)
            .systemBarsPadding(),
    ) {
        val result = state.result
        if (result == null) {
            SessionContent(state, petName, onClose, onTogglePause, onNext, onDone)
        } else {
            CelebrationContent(result, pet?.spec ?: PetSpecs.Bun, petName, onContinue, accessories = pet?.wearing.orEmpty())
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
    // Paused reads at a glance: ring and countdown dim, "Paused" appears under the time.
    val timerAlpha by animateFloatAsState(
        targetValue = if (state.isPaused) PAUSED_ALPHA else 1f,
        animationSpec = tween(PAUSE_FADE_MILLIS, easing = FastOutSlowInEasing),
        label = "timerAlpha",
    )
    Column(Modifier.fillMaxSize()) {
        // Everything above the buttons; the free space collects under the card, as in the mockup.
        Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(S.TopPadding))
            SessionHeader(title = stringResource(state.type.labelRes), stepLabel = stepLabel, onClose = onClose)
            Segments(
                count = state.stepCount,
                filled = state.stepIndex + 1,
                modifier = Modifier.padding(top = S.SegmentsTop, start = S.SegmentsHorizontal, end = S.SegmentsHorizontal),
            )
            // The ring shrinks on short screens / large fonts so the instruction stays visible.
            BoxWithConstraints(Modifier.weight(1f, fill = false).fillMaxWidth()) {
                val ring = minOf(S.RingSize, maxHeight - S.RingTop).coerceAtLeast(0.dp)
                CountdownRing(
                    progress = state.progress,
                    ringAlpha = timerAlpha,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = S.RingTop).size(ring),
                ) {
                    state.pet?.let { pet ->
                        PetView(
                            energy = 1f,
                            mood = state.mood,
                            spec = pet.spec,
                            name = petName,
                            pose = state.pose,
                            accessories = pet.wearing,
                            modifier = Modifier.size(ring * PET_IN_RING),
                        )
                    }
                }
            }
            CountdownText(state.remainingMillis, Modifier.padding(top = S.TimerTop).alpha(timerAlpha))
            AnimatedVisibility(visible = state.isPaused) { PausedLabel() }
            InstructionCard(
                stepLabel = stepLabel,
                instruction = stringResource(state.instruction),
                showSafetyNote = state.showSafetyNote,
                modifier = Modifier.padding(top = S.CardTop, start = S.CardHorizontal, end = S.CardHorizontal),
            )
        }
        SessionButtons(
            isPaused = state.isPaused,
            isLastStep = state.isLastStep,
            isFinished = state.isFinished,
            onTogglePause = onTogglePause,
            onNext = onNext,
            onDone = onDone,
            modifier = Modifier.padding(start = S.ButtonsHorizontal, end = S.ButtonsHorizontal, top = S.ButtonsTop, bottom = S.ButtonsBottom),
        )
    }
}

/** Sample states for previews and screenshot tests, built through the real mapper. */
internal object BreakSessionPreviewStates {
    private const val MILLIS_PER_SECOND = 1_000L
    private const val MILLIS_PER_MINUTE = 60_000L
    private val today: LocalDate = LocalDate.of(2026, 10, 27) // Tuesday

    /** [step] (0-based) with [remainingSeconds] left; frozen at "now" = 0. */
    fun midStep(type: BreakType, step: Int, remainingSeconds: Int, paused: Boolean = false, result: BreakDoneResult? = null): BreakSessionUiState {
        val durations = SessionTimer.evenSplit(type.durationMinutes * MILLIS_PER_MINUTE, type.sessionSteps().size)
        val timer = SessionTimer(
            stepDurationsMillis = durations,
            stepIndex = step,
            countedMillis = durations[step] - remainingSeconds * MILLIS_PER_SECOND,
            runningSinceMillis = if (paused) null else 0L,
        )
        return sessionUiState(type, pet = ActivePet.Default, timer = timer, now = 0L, result = result)
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
            streak = Streak(days = 3, startDate = today.minusDays(2), goal = 4, todayDistinct = 4, todayIsWorkDay = true),
        ),
    )

    /** A repeat neck stretch: celebrates, but no sparks; 3 of 4 types done today. */
    val CelebrateNoBadge = midStep(
        BreakType.NECK,
        step = 3,
        remainingSeconds = 0,
        result = BreakDoneResult(
            type = BreakType.NECK,
            sparksEarned = 0,
            bonuses = emptyList(),
            streak = Streak(days = 2, startDate = today.minusDays(2), goal = 4, todayDistinct = 3, todayIsWorkDay = true),
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

@Preview(widthDp = 360, heightDp = 640, fontScale = 1.3f)
@Composable
private fun BreakSessionSmallLargeFontPreview() {
    WigglebunTheme { BreakSessionScreen(BreakSessionPreviewStates.Neck) }
}
