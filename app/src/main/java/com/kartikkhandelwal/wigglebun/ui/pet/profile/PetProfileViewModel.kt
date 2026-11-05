package com.kartikkhandelwal.wigglebun.ui.pet.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartikkhandelwal.wigglebun.data.repo.PetRepository
import com.kartikkhandelwal.wigglebun.data.repo.SitRepository
import com.kartikkhandelwal.wigglebun.data.repo.StreakRepository
import com.kartikkhandelwal.wigglebun.ui.home.quantize
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PetProfileViewModel @Inject constructor(
    sitRepository: SitRepository,
    petRepository: PetRepository,
    streakRepository: StreakRepository,
) : ViewModel() {

    /** The status ticks every second; only mood and energy (in 0.5% steps) matter here. */
    private val live = sitRepository.status.filterNotNull()
        .map { it.mood to quantize(it.energy) }
        .distinctUntilChanged()

    /** Null until the saved state has loaded. */
    val state: StateFlow<PetProfileUiState?> = combine(
        petRepository.activePet,
        live,
        streakRepository.history,
    ) { pet, (mood, energy), history ->
        toPetProfileUiState(
            pet = pet,
            mood = mood,
            energy = energy,
            done = history.done,
            today = history.today,
            zone = history.zone,
            isWorkDay = history::isWorkDay,
            goalOn = history.goals::goalOn,
            dayOf = history.days::dayOf,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
