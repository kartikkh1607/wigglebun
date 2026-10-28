package com.kartikkhandelwal.wigglebun.ui.pet.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartikkhandelwal.wigglebun.data.prefs.SettingsRepository
import com.kartikkhandelwal.wigglebun.data.repo.BreakRepository
import com.kartikkhandelwal.wigglebun.data.repo.PetRepository
import com.kartikkhandelwal.wigglebun.data.repo.SitRepository
import com.kartikkhandelwal.wigglebun.data.time.AppClock
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.domain.rewards.RewardRules
import com.kartikkhandelwal.wigglebun.ui.home.quantize
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class PetProfileViewModel @Inject constructor(
    sitRepository: SitRepository,
    petRepository: PetRepository,
    breakRepository: BreakRepository,
    settingsRepository: SettingsRepository,
    private val clock: AppClock,
) : ViewModel() {

    private data class Live(val mood: Mood, val energy: Float, val today: LocalDate)

    /** The status ticks every second; only mood, energy (in 0.5% steps) and the date matter here. */
    private val live = sitRepository.status.filterNotNull()
        .map { Live(it.mood, quantize(it.energy), it.now.atZone(clock.zone).toLocalDate()) }
        .distinctUntilChanged()

    /** Null until the saved state has loaded. */
    val state: StateFlow<PetProfileUiState?> = combine(
        petRepository.activePet,
        live,
        breakRepository.observeDoneBreaks(),
        settingsRepository.workDayHistory,
    ) { pet, now, done, history ->
        toPetProfileUiState(
            pet = pet,
            mood = now.mood,
            energy = now.energy,
            done = done,
            today = now.today,
            zone = clock.zone,
            isWorkDay = history::isWorkDay,
            // All types are on until the Settings toggles exist (M9), as in RecordBreakDone.
            goal = RewardRules.dailyGoal(enabledTypeCount = BreakType.entries.size),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
