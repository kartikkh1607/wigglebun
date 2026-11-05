package com.kartikkhandelwal.wigglebun.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kartikkhandelwal.wigglebun.data.prefs.SettingsRepository
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.cleanPetName
import com.kartikkhandelwal.wigglebun.domain.model.limitPetNameInput
import com.kartikkhandelwal.wigglebun.domain.sitting.SittingSettings
import com.kartikkhandelwal.wigglebun.ui.breaksession.BreakSessionScreenRoute
import com.kartikkhandelwal.wigglebun.ui.navigation.BreakSessionRoute
import com.kartikkhandelwal.wigglebun.ui.navigation.navFadeIn
import com.kartikkhandelwal.wigglebun.ui.navigation.navFadeOut
import com.kartikkhandelwal.wigglebun.ui.paywall.PaywallPlaceholderScreen
import com.kartikkhandelwal.wigglebun.ui.pet.PetSpecs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import javax.inject.Inject

@Serializable
data object WakeUpRoute

@Serializable
data object NamePetRoute

@Serializable
data object WorkHoursRoute

@Serializable
data object IntervalRoute

@Serializable
data object FirstStretchRoute

@Serializable
data object PaywallPlaceholderRoute

/** One of the 1-minute breaks, and it ends with a big reach up. */
private val FirstBreak = BreakType.STAND_UP

@HiltViewModel
class OnboardingViewModel @Inject constructor(private val settings: SettingsRepository) : ViewModel() {
    private val _nameDraft = MutableStateFlow("")

    /** What's in the name field; starts with the saved name if onboarding was interrupted. */
    val nameDraft: StateFlow<String> = _nameDraft.asStateFlow()

    private val _workHours = MutableStateFlow(WorkHoursDraft())
    val workHours: StateFlow<WorkHoursDraft> = _workHours.asStateFlow()

    private val _hoursProblem = MutableStateFlow<WorkHoursProblem?>(null)

    /**
     * What the last Continue found (the same start and end); the next edit clears it. No work days
     * needs no Continue: the screen turns Continue off and shows why ([WorkHoursDraft.shownProblem]).
     */
    val hoursProblem: StateFlow<WorkHoursProblem?> = _hoursProblem.asStateFlow()

    private val _intervalMinutes = MutableStateFlow(SittingSettings.DEFAULT_INTERVAL_MINUTES)
    val intervalMinutes: StateFlow<Int> = _intervalMinutes.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = settings.petName.first()
            if (saved != null && _nameDraft.value.isEmpty()) _nameDraft.value = saved
            val stored = settings.current()
            _workHours.value = WorkHoursDraft.of(stored.schedule)
            _intervalMinutes.value = stored.interval
        }
    }

    fun onNameChange(input: String) {
        _nameDraft.value = limitPetNameInput(input)
    }

    fun saveName() {
        val name = cleanPetName(_nameDraft.value)
        viewModelScope.launch { settings.setPetName(name) }
    }

    fun onToggleDay(day: DayOfWeek) = editHours { it.toggled(day) }

    fun onTimePicked(time: WorkTime, minute: Int) = editHours {
        when (time) {
            WorkTime.START -> it.copy(startMinute = minute)
            WorkTime.END -> it.copy(endMinute = minute)
        }
    }

    /** The same start and end is never flagged on the way (18–18 between 9–18 and 18–02 is fine), as in Settings. */
    private fun editHours(change: (WorkHoursDraft) -> WorkHoursDraft) {
        _workHours.update(change)
        _hoursProblem.value = null
    }

    /**
     * Continue: saves the hours and returns true, or returns false and says why (the same start
     * and end). With no work days Continue is off, so nothing happens. Skipped steps never save:
     * what's stored (the defaults on a new install) stays.
     */
    fun saveWorkHours(): Boolean {
        val draft = _workHours.value
        if (!draft.canSave) return false
        _hoursProblem.value = draft.problem
        if (draft.problem != null) return false
        viewModelScope.launch { settings.setSchedule(draft.toSchedule()) }
        return true
    }

    fun onIntervalPicked(minutes: Int) {
        _intervalMinutes.value = minutes
    }

    fun saveInterval() {
        val minutes = _intervalMinutes.value
        viewModelScope.launch { settings.setInterval(minutes) }
    }

    /** Flips the [AppGate] over to the main app. */
    fun finish() {
        viewModelScope.launch { settings.finishOnboarding() }
    }
}

@Composable
fun OnboardingFlowRoute(viewModel: OnboardingViewModel = hiltViewModel()) {
    val nameDraft by viewModel.nameDraft.collectAsStateWithLifecycle()
    val workHours by viewModel.workHours.collectAsStateWithLifecycle()
    val hoursProblem by viewModel.hoursProblem.collectAsStateWithLifecycle()
    val intervalMinutes by viewModel.intervalMinutes.collectAsStateWithLifecycle()
    OnboardingFlow(
        nameDraft = nameDraft,
        workHours = workHours,
        hoursProblem = hoursProblem,
        intervalMinutes = intervalMinutes,
        actions = OnboardingActions(
            onNameChange = viewModel::onNameChange,
            onSaveName = viewModel::saveName,
            onToggleDay = viewModel::onToggleDay,
            onTimePicked = viewModel::onTimePicked,
            onSaveWorkHours = viewModel::saveWorkHours,
            onIntervalPicked = viewModel::onIntervalPicked,
            onSaveInterval = viewModel::saveInterval,
            onFinish = viewModel::finish,
        ),
    )
}

/** Everything the steps can ask for; each is a no-op by default so tests set only what they check. */
class OnboardingActions(
    val onNameChange: (String) -> Unit = {},
    val onSaveName: () -> Unit = {},
    val onToggleDay: (DayOfWeek) -> Unit = {},
    val onTimePicked: (WorkTime, Int) -> Unit = { _, _ -> },
    /** True when the hours were fine and saved, so the flow moves on. */
    val onSaveWorkHours: () -> Boolean = { true },
    val onIntervalPicked: (Int) -> Unit = {},
    val onSaveInterval: () -> Unit = {},
    val onFinish: () -> Unit = {},
)

/**
 * The steps in order (CLAUDE.md §6.2). Skip moves on one step and keeps that step's default, so
 * nobody misses the first stretch and the paywall that follow it (§5.5).
 */
@Composable
fun OnboardingFlow(
    nameDraft: String,
    workHours: WorkHoursDraft,
    intervalMinutes: Int,
    actions: OnboardingActions,
    /** Why the last Continue on the hours didn't go through, shown by Continue. */
    hoursProblem: WorkHoursProblem? = null,
    navController: NavHostController = rememberNavController(),
    /** Overridable so tests can stand in for the Hilt-backed break session. */
    sessionContent: @Composable (onExit: () -> Unit, onCompleted: () -> Unit) -> Unit = { onExit, onCompleted ->
        BreakSessionScreenRoute(onExit = onExit, onCompleted = onCompleted)
    },
) {
    val defaultName = stringResource(PetSpecs.Bun.defaultName)
    val petName = cleanPetName(nameDraft) ?: defaultName
    NavHost(navController = navController, startDestination = WakeUpRoute, enterTransition = navFadeIn, exitTransition = navFadeOut) {
        composable<WakeUpRoute> {
            val next = dropUnlessResumed { navController.navigate(NamePetRoute) { launchSingleTop = true } }
            WakeUpScreenRoute(onContinue = next, onSkip = next)
        }
        composable<NamePetRoute> {
            val next = dropUnlessResumed { navController.navigate(WorkHoursRoute) { launchSingleTop = true } }
            NamePetScreen(
                name = nameDraft,
                onNameChange = actions.onNameChange,
                onContinue = {
                    actions.onSaveName()
                    next()
                },
                onSkip = next,
            )
        }
        composable<WorkHoursRoute> {
            val next = dropUnlessResumed { navController.navigate(IntervalRoute) { launchSingleTop = true } }
            WorkHoursScreenRoute(
                draft = workHours,
                problem = hoursProblem,
                onToggleDay = actions.onToggleDay,
                onTimePicked = actions.onTimePicked,
                onContinue = { if (actions.onSaveWorkHours()) next() },
                onSkip = next,
            )
        }
        composable<IntervalRoute> {
            val next = dropUnlessResumed { navController.navigate(FirstStretchRoute) { launchSingleTop = true } }
            IntervalScreenRoute(
                minutes = intervalMinutes,
                petName = petName,
                onPicked = actions.onIntervalPicked,
                onContinue = {
                    actions.onSaveInterval()
                    next()
                },
                onSkip = next,
            )
        }
        composable<FirstStretchRoute> {
            val start = dropUnlessResumed { navController.navigate(BreakSessionRoute(FirstBreak.name)) { launchSingleTop = true } }
            FirstStretchScreen(petName = petName, onStart = start, onSkip = { navController.openPaywall() })
        }
        composable<BreakSessionRoute> {
            sessionContent({ navController.popBackStack() }, { navController.openPaywall() })
        }
        composable<PaywallPlaceholderRoute> {
            PaywallPlaceholderScreen(petName = petName, onContinue = actions.onFinish)
        }
    }
}

/** There's nothing to go back to from the paywall: the steps are behind us. */
private fun NavHostController.openPaywall() = navigate(PaywallPlaceholderRoute) {
    popUpTo(FirstStretchRoute) { inclusive = true }
    launchSingleTop = true
}
