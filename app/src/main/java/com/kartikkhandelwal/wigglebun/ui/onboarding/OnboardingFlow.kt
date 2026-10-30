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
import com.kartikkhandelwal.wigglebun.ui.pet.PetSpecs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data object WakeUpRoute

@Serializable
data object NamePetRoute

@HiltViewModel
class OnboardingViewModel @Inject constructor(private val settings: SettingsRepository) : ViewModel() {
    private val _nameDraft = MutableStateFlow("")

    /** What's in the name field; starts with the saved name if onboarding was interrupted. */
    val nameDraft: StateFlow<String> = _nameDraft.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = settings.petName.first()
            if (saved != null && _nameDraft.value.isEmpty()) _nameDraft.value = saved
        }
    }

    fun onNameChange(input: String) {
        _nameDraft.value = limitPetNameInput(input)
    }

    fun saveName() {
        val name = cleanPetName(_nameDraft.value)
        viewModelScope.launch { settings.setPetName(name) }
    }

    /** Flips the [AppGate] over to the main app. */
    fun finish() {
        viewModelScope.launch { settings.finishOnboarding() }
    }
}

@Composable
fun OnboardingFlowRoute(viewModel: OnboardingViewModel = hiltViewModel()) {
    val nameDraft by viewModel.nameDraft.collectAsStateWithLifecycle()
    OnboardingFlow(
        nameDraft = nameDraft,
        onNameChange = viewModel::onNameChange,
        onSaveName = viewModel::saveName,
        onFinish = viewModel::finish,
    )
}

/**
 * The steps in order (CLAUDE.md §6.2). Skip moves on one step and keeps that step's default, so
 * nobody misses the first stretch and the paywall that follow it (§5.5).
 */
@Composable
fun OnboardingFlow(
    nameDraft: String,
    onNameChange: (String) -> Unit,
    onSaveName: () -> Unit,
    onFinish: () -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    val defaultName = stringResource(PetSpecs.Bun.defaultName)
    NavHost(navController = navController, startDestination = WakeUpRoute) {
        composable<WakeUpRoute> {
            val next = dropUnlessResumed { navController.navigate(NamePetRoute) { launchSingleTop = true } }
            WakeUpScreenRoute(petName = cleanPetName(nameDraft) ?: defaultName, onContinue = next, onSkip = next)
        }
        composable<NamePetRoute> {
            NamePetScreen(
                name = nameDraft,
                onNameChange = onNameChange,
                onContinue = {
                    onSaveName()
                    onFinish()
                },
                onSkip = onFinish,
            )
        }
    }
}
