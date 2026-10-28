package com.kartikkhandelwal.wigglebun.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.kartikkhandelwal.wigglebun.data.prefs.SettingsRepository
import com.kartikkhandelwal.wigglebun.data.repo.BreakActions
import com.kartikkhandelwal.wigglebun.data.repo.BreakRepository
import com.kartikkhandelwal.wigglebun.data.repo.PetRepository
import com.kartikkhandelwal.wigglebun.data.repo.SitRepository
import com.kartikkhandelwal.wigglebun.data.repo.WalletRepository
import com.kartikkhandelwal.wigglebun.data.time.AppClock
import com.kartikkhandelwal.wigglebun.domain.model.ActivePet
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.ui.pet.displayName
import com.kartikkhandelwal.wigglebun.ui.pet.spec
import com.kartikkhandelwal.wigglebun.ui.theme.Cream
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

private val H = Dimens.Home
private const val ENTRANCE_SETTLE_MILLIS = 400L

@HiltViewModel
class HomeViewModel @Inject constructor(
    sitRepository: SitRepository,
    settingsRepository: SettingsRepository,
    breakRepository: BreakRepository,
    walletRepository: WalletRepository,
    petRepository: PetRepository,
    private val actions: BreakActions,
    private val clock: AppClock,
) : ViewModel() {

    private val focused = MutableStateFlow<BreakType?>(null)
    private val status = sitRepository.status.filterNotNull()

    /** Today's BreakLog, re-queried when the local date changes. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val logsToday = status
        .map { it.now.atZone(clock.zone).toLocalDate() }
        .distinctUntilChanged()
        .flatMapLatest { breakRepository.observeDay(it, clock.zone) }

    private val profile = combine(
        settingsRepository.settings,
        petRepository.activePet,
        walletRepository.balance,
    ) { settings, pet, sparks -> Triple(settings, pet, sparks) }

    /** Null until the saved state has loaded (no default pet flashing on a cold start). */
    val state: StateFlow<HomeUiState?> = combine(status, profile, logsToday, focused) { st, (settings, pet, sparks), logs, focus ->
        toHomeUiState(st, settings, pet, sparks, logs, focus, clock.zone)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun openBreak(type: BreakType) {
        focused.value = type
    }

    fun closeBreak() {
        focused.value = null
    }

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)

    /** One-shot navigation requests, collected by the route. */
    val events: Flow<HomeEvent> = _events.receiveAsFlow()

    /**
     * The single place a "Done" request lands (card check button, focused card, stiff CTA): it
     * opens the guided break session, which completes the break through RecordBreakDone.
     * A type already done today can be done again: it resets the timer and celebrates, but pays
     * sparks only the first time each day (CLAUDE.md §5.4).
     */
    fun requestDone(type: BreakType) {
        focused.value = null
        _events.trySend(HomeEvent.StartSession(type))
    }

    fun skipFocused() {
        val type = focused.value ?: return
        val due = state.value?.reminderDue ?: false
        focused.value = null
        viewModelScope.launch { actions.skip(type, reminderDue = due) }
    }

    fun snoozeFocused() {
        focused.value = null
        viewModelScope.launch { actions.snooze() }
    }

    /** "Take a 2-min break" (stiff CTA) starts the 2-min walk session. */
    fun takeBreak() = requestDone(BreakType.WALK)
}

sealed interface HomeEvent {
    data class StartSession(val type: BreakType) : HomeEvent
}

/** Navigation and debug hooks Home needs from the app shell. */
class HomeNavigation(
    val onOpenMenu: () -> Unit = {},
    val onStartBreak: (BreakType) -> Unit = {},
    val debugEntry: @Composable () -> Unit = {},
)

@Composable
fun HomeScreenRoute(
    navigation: HomeNavigation,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        // Only while visible, so a navigation request is never handled behind another screen.
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    is HomeEvent.StartSession -> navigation.onStartBreak(event.type)
                }
            }
        }
    }
    val loaded = state
    if (loaded == null) {
        // Only the background until the real state arrives (a few ms from DataStore).
        Box(modifier.fillMaxSize().background(Cream))
        return
    }
    HomeScreen(
        state = loaded,
        onOpenMenu = navigation.onOpenMenu,
        onOpenBreak = viewModel::openBreak,
        onCheck = viewModel::requestDone,
        onTakeBreak = viewModel::takeBreak,
        modifier = modifier,
        debugEntry = navigation.debugEntry,
    )
    loaded.focus?.let { focus ->
        FocusCardDialog(onDismissRequest = viewModel::closeBreak) {
            FocusCardOverlay(
                focus = focus,
                intervalMinutes = loaded.intervalMinutes,
                workStartMinute = loaded.workStartMinute,
                workEndMinute = loaded.workEndMinute,
                onSkip = viewModel::skipFocused,
                onDone = { viewModel.requestDone(focus.type) },
                onSnooze = viewModel::snoozeFocused,
                onClose = viewModel::closeBreak,
            )
        }
    }
}

/**
 * Home (CLAUDE.md §6.3, design/mockups_source/Main.dc.html). One scrolling list: desk scene with
 * the timer pill overlapping it, "Breaks for today" header, break cards.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    modifier: Modifier = Modifier,
    onOpenMenu: () -> Unit = {},
    onOpenBreak: (BreakType) -> Unit = {},
    onCheck: (BreakType) -> Unit = {},
    onTakeBreak: () -> Unit = {},
    debugEntry: @Composable () -> Unit = {},
    animateEntrance: Boolean = !LocalInspectionMode.current,
) {
    val petName = state.pet.displayName()
    // Cards stagger in once per visit, not every time they scroll back into view.
    var entered by rememberSaveable { mutableStateOf(!animateEntrance) }
    LaunchedEffect(Unit) {
        if (!entered) {
            delay(state.breaks.size * STAGGER_MILLIS + ENTRANCE_SETTLE_MILLIS)
            entered = true
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Cream),
        contentPadding = PaddingValues(bottom = H.ListBottomPadding),
    ) {
        item(key = "top") {
            Column {
                DeskScene(
                    energy = state.energy,
                    mood = state.mood,
                    pet = state.pet.spec,
                    petName = petName,
                    sparks = state.sparks,
                    onOpenMenu = onOpenMenu,
                )
                TimerPill(
                    pill = state.pill,
                    petName = petName,
                    today = state.today,
                    showBreakCta = state.showBreakCta,
                    onTakeBreak = onTakeBreak,
                    modifier = Modifier
                        .padding(horizontal = Dimens.ScreenPadding)
                        .overlapUp(H.PillOverlap),
                )
            }
        }
        item(key = "header") { BreaksHeader(state.doneCount, state.totalCount) }
        itemsIndexed(state.breaks, key = { _, card -> card.type }) { index, card ->
            BreakCard(
                card = card,
                onOpen = { onOpenBreak(card.type) },
                onCheck = { onCheck(card.type) },
                modifier = Modifier
                    .padding(horizontal = Dimens.ScreenPadding)
                    // Mockup gap is 10 dp with the 3 dp edge drawn inside it.
                    .padding(bottom = Dimens.ListGap - H.CardEdge)
                    .staggerIn(index, enabled = !entered),
            )
        }
        item(key = "debug") {
            Box(Modifier.fillMaxWidth().padding(top = Dimens.ListGap), contentAlignment = Alignment.Center) { debugEntry() }
        }
    }
}

/** Pulls the element up by [overlap] and gives the space back (timer pill over the scene). */
private fun Modifier.overlapUp(overlap: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val shift = overlap.roundToPx()
    layout(placeable.width, (placeable.height - shift).coerceAtLeast(0)) { placeable.place(0, -shift) }
}

/** Sample states for previews and screenshot tests. */
internal object HomePreviewStates {
    private val today: LocalDate = LocalDate.of(2026, 10, 26) // Monday
    private val doneTwo = setOf(BreakType.STAND_UP, BreakType.NECK)

    private fun state(
        mood: Mood,
        energy: Float,
        pill: PillState,
        cta: Boolean = false,
        done: Set<BreakType> = doneTwo,
        skipped: Set<BreakType> = emptySet(),
    ) = HomeUiState(
        pet = ActivePet.Default,
        energy = energy,
        mood = mood,
        pill = pill,
        showBreakCta = cta,
        sparks = 120,
        breaks = BreakType.entries.map { BreakCardUi(it, doneToday = it in done, skippedToday = it in skipped) },
        intervalMinutes = 45,
        workStartMinute = 9 * 60,
        workEndMinute = 18 * 60,
        reminderDue = false,
        today = today,
        focus = null,
    )

    val Happy = state(Mood.HAPPY, 0.78f, PillState.Sitting(12, 12 / 45f, MoodChip.GREAT))
    val Tired = state(Mood.TIRED, 0.41f, PillState.Sitting(32, 32 / 45f, MoodChip.TIRED))
    val Stiff = state(Mood.STIFF, 0.15f, PillState.Sitting(52, 1f, MoodChip.STIFF), cta = true)
    val OnWalk = state(Mood.HAPPY, 1f, PillState.Sitting(0, 0f, MoodChip.WALK))
    val Asleep = state(
        Mood.SLEEPING,
        1f,
        PillState.Asleep(LocalDateTime.of(2026, 10, 27, 9, 0)),
        skipped = setOf(BreakType.WATER),
    )
    val AllDone = state(Mood.HAPPY, 0.9f, PillState.Sitting(5, 5 / 45f, MoodChip.GREAT), done = BreakType.entries.toSet())

    /** Just after Done on the 2-min walk: timer back to 0, card green, pet celebrating. */
    val Celebrate = state(
        Mood.CELEBRATE,
        1f,
        PillState.Sitting(0, 0f, MoodChip.GREAT),
        done = doneTwo + BreakType.WALK,
    )
    val FocusDue = Stiff.copy(focus = FocusUi(BreakType.WALK, doneToday = false, canSnooze = true), reminderDue = true)
    val FocusNotDue = Tired.copy(focus = FocusUi(BreakType.WALK, doneToday = false, canSnooze = false))

    /** Neck stretch was done earlier today: Done still works, but no reward is promised. */
    val FocusDoneToday = Tired.copy(focus = FocusUi(BreakType.NECK, doneToday = true, canSnooze = false))
}

/**
 * Home with the focused card on top, for previews/screenshots. [inDialog] uses the real
 * [FocusCardDialog] window (screenshot tests only: previews can't show dialogs).
 */
@Composable
internal fun HomeWithFocusPreview(state: HomeUiState, inDialog: Boolean = false) {
    WigglebunTheme {
        Box {
            HomeScreen(state)
            state.focus?.let { focus ->
                val overlay = @Composable {
                    FocusCardOverlay(focus, state.intervalMinutes, state.workStartMinute, state.workEndMinute, {}, {}, {}, {})
                }
                if (inDialog) FocusCardDialog(onDismissRequest = {}, content = overlay) else overlay()
            }
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun HomeHappyPreview() {
    WigglebunTheme { HomeScreen(HomePreviewStates.Happy) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun HomeTiredPreview() {
    WigglebunTheme { HomeScreen(HomePreviewStates.Tired) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun HomeStiffPreview() {
    WigglebunTheme { HomeScreen(HomePreviewStates.Stiff) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun HomeOnWalkPreview() {
    WigglebunTheme { HomeScreen(HomePreviewStates.OnWalk) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun HomeAsleepPreview() {
    WigglebunTheme { HomeScreen(HomePreviewStates.Asleep) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun HomeCelebratePreview() {
    WigglebunTheme { HomeScreen(HomePreviewStates.Celebrate) }
}

@Preview(widthDp = 390, heightDp = 1400)
@Composable
private fun HomeAllDonePreview() {
    WigglebunTheme { HomeScreen(HomePreviewStates.AllDone) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun HomeFocusDuePreview() = HomeWithFocusPreview(HomePreviewStates.FocusDue)

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun HomeFocusNotDuePreview() = HomeWithFocusPreview(HomePreviewStates.FocusNotDue)

@Preview(widthDp = 360, heightDp = 640, fontScale = 1.3f)
@Composable
private fun HomeSmallLargeFontPreview() {
    WigglebunTheme { HomeScreen(HomePreviewStates.Stiff) }
}

@Preview(widthDp = 360, heightDp = 640, fontScale = 1.3f)
@Composable
private fun HomeSmallLargeFontFocusPreview() = HomeWithFocusPreview(HomePreviewStates.FocusDue)
