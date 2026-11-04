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
    /** The work hours on screen: what's stored, or the unsaved edit. */
    val hours: WorkHoursDraft,
    /** [hours] differs from what's stored, so Save shows. */
    val hoursChanged: Boolean,
    /** No work days (at once), or the same start and end (found by the last Save). */
    val hoursProblem: WorkHoursProblem?,
    val intervalMinutes: Int,
    val enabledBreaks: List<BreakType>,
) {
    /** Save is off while no work day is ticked. */
    val canSaveHours: Boolean get() = hours.canSave
}

/**
 * Work hours: no work days turns Save off and says why at once. The same start and end is only
 * checked on Save, so going from 9–18 to 18–02 works in either order (18–18 on the way is fine).
 * The interval and the break switches save straight away.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val widget: WidgetRefresher,
) : ViewModel() {

    /** Null while there's no edit, so the screen follows what's stored. */
    private val hoursEdit = MutableStateFlow<WorkHoursDraft?>(null)
    private val hoursProblem = MutableStateFlow<WorkHoursProblem?>(null)

    /** Null until the stored settings have loaded. */
    val state: StateFlow<SettingsUiState?> = combine(
        settings.settings,
        settings.enabledBreaks,
        hoursEdit,
        hoursProblem,
    ) { stored, breaks, edit, problem ->
        val saved = WorkHoursDraft.of(stored.schedule)
        val hours = edit ?: saved
        SettingsUiState(hours, hoursChanged = hours != saved, hours.shownProblem(problem), stored.interval, breaks)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    fun onToggleDay(day: DayOfWeek) = editHours { it.toggled(day) }

    fun onTimePicked(time: WorkTime, minute: Int) = editHours {
        when (time) {
            WorkTime.START -> it.copy(startMinute = minute)
            WorkTime.END -> it.copy(endMinute = minute)
        }
    }

    private fun editHours(change: (WorkHoursDraft) -> WorkHoursDraft) {
        val current = hoursEdit.value ?: state.value?.hours ?: return
        hoursEdit.value = change(current)
        hoursProblem.value = null
    }

    /** Saves the edited hours, or says why it can't (the same start and end). Off with no work days. */
    fun onSaveHours() {
        val hours = hoursEdit.value ?: return
        if (!hours.canSave) return
        hoursProblem.value = hours.problem
        if (hours.problem != null) return
        viewModelScope.launch {
            settings.setSchedule(hours.toSchedule())
            // Only if nothing was edited while saving: a newer edit stays on screen, unsaved.
            hoursEdit.compareAndSet(hours, null)
            widget.requestUpdate() // the hours decide whether the pet sleeps
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
