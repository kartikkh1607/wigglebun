package com.kartikkhandelwal.wigglebun.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartikkhandelwal.wigglebun.data.prefs.SettingsRepository
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.ui.onboarding.WorkHoursDraft
import com.kartikkhandelwal.wigglebun.ui.onboarding.WorkHoursProblem
import com.kartikkhandelwal.wigglebun.ui.onboarding.WorkTime
import com.kartikkhandelwal.wigglebun.widget.WidgetRefresher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

data class SettingsUiState(
    val hours: WorkHoursDraft,
    /** Why the last change to the hours wasn't made; the stored hours stay as they were. */
    val hoursProblem: WorkHoursProblem?,
    val intervalMinutes: Int,
    val enabledBreaks: List<BreakType>,
)

/** Everything saves straight away. There's no Save button and nothing to lose. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val widget: WidgetRefresher,
) : ViewModel() {

    private val hoursProblem = MutableStateFlow<WorkHoursProblem?>(null)

    /** Null until the stored settings have loaded. */
    val state: StateFlow<SettingsUiState?> = combine(
        settings.settings,
        settings.enabledBreaks,
        hoursProblem,
    ) { stored, breaks, problem ->
        SettingsUiState(WorkHoursDraft.of(stored.schedule), problem, stored.interval, breaks)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    fun onToggleDay(day: DayOfWeek) = changeHours { it.toggled(day) }

    fun onTimePicked(time: WorkTime, minute: Int) = changeHours {
        when (time) {
            WorkTime.START -> it.copy(startMinute = minute)
            WorkTime.END -> it.copy(endMinute = minute)
        }
    }

    /**
     * Applied to what's stored inside one atomic write, so quick taps can't overwrite each other.
     * A change that would leave an impossible schedule isn't made, and the screen says why.
     */
    private fun changeHours(change: (WorkHoursDraft) -> WorkHoursDraft) {
        viewModelScope.launch {
            var problem: WorkHoursProblem? = null
            settings.updateSchedule { schedule ->
                val next = change(WorkHoursDraft.of(schedule))
                problem = next.problem
                if (next.problem == null) next.toSchedule() else schedule
            }
            hoursProblem.value = problem
            if (problem == null) widget.requestUpdate() // the hours decide whether the pet sleeps
        }
    }

    fun onIntervalPicked(minutes: Int) {
        viewModelScope.launch {
            settings.setInterval(minutes)
            widget.requestUpdate() // the interval decides the mood
        }
    }

    fun onBreakToggled(type: BreakType, enabled: Boolean) {
        viewModelScope.launch {
            settings.setBreakEnabled(type, enabled)
            widget.requestUpdate() // its "Take a break" button suggests one of the enabled types
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
