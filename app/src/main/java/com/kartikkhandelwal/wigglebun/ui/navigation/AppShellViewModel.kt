package com.kartikkhandelwal.wigglebun.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartikkhandelwal.wigglebun.billing.AccessGate
import com.kartikkhandelwal.wigglebun.billing.AppAccess
import com.kartikkhandelwal.wigglebun.data.prefs.SettingsRepository
import com.kartikkhandelwal.wigglebun.data.repo.PetRepository
import com.kartikkhandelwal.wigglebun.domain.model.ActivePet
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest

/** What the app shell itself shows: the active pet, which names the pet tab, and the first-launch gate. */
@HiltViewModel
class AppShellViewModel @Inject constructor(
    petRepository: PetRepository,
    settings: SettingsRepository,
    accessGate: AccessGate,
) : ViewModel() {
    /** Null until loaded, so a cold start never names the tab after the wrong pet. */
    val activePet: StateFlow<ActivePet?> = petRepository.activePet.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Null until decided, see [SettingsRepository.onboardingDone]. */
    val onboardingDone: StateFlow<Boolean?> = settings.onboardingDone.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * Null until known. A paying user's answer is cached on the phone, so it is there at once;
     * with nothing cached and no reply yet (first launch offline, a reinstall) the wait ends after
     * [CHECKING_PATIENCE_MILLIS] as locked, where the paywall can say what's wrong and try again.
     * The real answer still replaces it whenever it arrives.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val access: StateFlow<AppAccess?> = accessGate.access
        .transformLatest { access ->
            if (access == AppAccess.Checking) {
                emit(null)
                delay(CHECKING_PATIENCE_MILLIS)
                emit(AppAccess.Locked)
            } else {
                emit(access)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private companion object {
        const val CHECKING_PATIENCE_MILLIS = 3_000L
    }
}
