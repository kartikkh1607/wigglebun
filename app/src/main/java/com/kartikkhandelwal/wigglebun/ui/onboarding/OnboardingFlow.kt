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
import com.kartikkhandelwal.wigglebun.domain.model.cleanPetName
import com.kartikkhandelwal.wigglebun.domain.model.limitPetNameInput
import com.kartikkhandelwal.wigglebun.domain.sitting.SittingSettings
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

@HiltViewModel
class OnboardingViewModel @Inject constructor(private val settings: SettingsRepository) : ViewModel() {
    private val _nameDraft = MutableStateFlow("")

    /** What's in the name field; starts with the saved name if onboarding was interrupted. */
    val nameDraft: StateFlow<String> = _nameDraft.asStateFlow()

    private val _workHours = MutableStateFlow(WorkHoursDraft())
    val workHours: StateFlow<WorkHoursDraft> = _workHours.asStateFlow()

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

    fun onToggleDay(day: DayOfWeek) = _workHours.update { it.toggled(day) }

    fun onTimePicked(time: WorkTime, minute: Int) = _workHours.update {
        when (time) {
            WorkTime.START -> it.copy(startMinute = minute)
            WorkTime.END -> it.copy(endMinute = minute)
        }
    }

    /** Skipped steps never save: what's stored (the defaults on a new install) stays. */
    fun saveWorkHours() {
        val draft = _workHours.value
        if (draft.problem != null) return
        viewModelScope.launch { settings.setSchedule(draft.toSchedule()) }
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
    val intervalMinutes by viewModel.intervalMinutes.collectAsStateWithLifecycle()
    OnboardingFlow(
        nameDraft = nameDraft,
        workHours = workHours,
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
    val onSaveWorkHours: () -> Unit = {},
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
    navController: NavHostController = rememberNavController(),
) {
    val defaultName = stringResource(PetSpecs.Bun.defaultName)
    val petName = cleanPetName(nameDraft) ?: defaultName
    NavHost(navController = navController, startDestination = WakeUpRoute) {
        composable<WakeUpRoute> {
            val next = dropUnlessResumed { navController.navigate(NamePetRoute) { launchSingleTop = true } }
            WakeUpScreenRoute(petName = petName, onContinue = next, onSkip = next)
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
                onToggleDay = actions.onToggleDay,
                onTimePicked = actions.onTimePicked,
                onContinue = {
                    actions.onSaveWorkHours()
                    next()
                },
                onSkip = next,
            )
        }
        composable<IntervalRoute> {
            IntervalScreenRoute(
                minutes = intervalMinutes,
                petName = petName,
                onPicked = actions.onIntervalPicked,
                onContinue = {
                    actions.onSaveInterval()
                    actions.onFinish()
                },
                onSkip = actions.onFinish,
            )
        }
    }
}
