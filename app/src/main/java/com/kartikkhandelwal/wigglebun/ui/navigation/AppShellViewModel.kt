package com.kartikkhandelwal.wigglebun.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartikkhandelwal.wigglebun.data.prefs.SettingsRepository
import com.kartikkhandelwal.wigglebun.data.repo.PetRepository
import com.kartikkhandelwal.wigglebun.domain.model.ActivePet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** What the app shell itself shows: the active pet, which names the pet tab, and the first-launch gate. */
@HiltViewModel
class AppShellViewModel @Inject constructor(
    petRepository: PetRepository,
    settings: SettingsRepository,
) : ViewModel() {
    /** Null until loaded, so a cold start never names the tab after the wrong pet. */
    val activePet: StateFlow<ActivePet?> = petRepository.activePet.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Null until decided, see [SettingsRepository.onboardingDone]. */
    val onboardingDone: StateFlow<Boolean?> = settings.onboardingDone.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
