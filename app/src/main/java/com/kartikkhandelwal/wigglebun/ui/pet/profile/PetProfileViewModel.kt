package com.kartikkhandelwal.wigglebun.ui.pet.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartikkhandelwal.wigglebun.data.prefs.SettingsRepository
import com.kartikkhandelwal.wigglebun.data.repo.BreakRepository
import com.kartikkhandelwal.wigglebun.data.repo.PetRepository
import com.kartikkhandelwal.wigglebun.data.repo.SitRepository
import com.kartikkhandelwal.wigglebun.data.time.AppClock
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.domain.sitting.BreakDays
import com.kartikkhandelwal.wigglebun.domain.sitting.DayBoundaryHistory
import com.kartikkhandelwal.wigglebun.ui.home.quantize
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
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

    private data class Live(val mood: Mood, val energy: Float, val today: LocalDate, val boundaries: DayBoundaryHistory)

    /** The status ticks every second; only mood, energy (in 0.5% steps) and the break day matter here. */
    private val live = combine(sitRepository.status.filterNotNull(), settingsRepository.dayBoundaryHistory) { status, history ->
        Live(status.mood, quantize(status.energy), BreakDays(history, clock.zone).dayOf(status.now), history)
    }
        .distinctUntilChanged()

    /** Null until the saved state has loaded. */
    val state: StateFlow<PetProfileUiState?> = combine(
        petRepository.activePet,
        live,
        breakRepository.observeDoneBreaks(),
        settingsRepository.workDayHistory,
        settingsRepository.dailyGoalHistory,
    ) { pet, now, done, history, goals ->
        val days = BreakDays(now.boundaries, clock.zone)
        toPetProfileUiState(
            pet = pet,
            mood = now.mood,
            energy = now.energy,
            done = done,
            today = now.today,
            zone = clock.zone,
            isWorkDay = { days.exists(it) && history.isWorkDay(it) },
            goalOn = goals::goalOn,
            dayOf = days::dayOf,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
