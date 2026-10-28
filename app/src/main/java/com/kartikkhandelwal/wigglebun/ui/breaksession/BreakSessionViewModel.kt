package com.kartikkhandelwal.wigglebun.ui.breaksession

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kartikkhandelwal.wigglebun.data.repo.BreakDoneResult
import com.kartikkhandelwal.wigglebun.data.repo.PetRepository
import com.kartikkhandelwal.wigglebun.data.repo.RecordBreakDone
import com.kartikkhandelwal.wigglebun.data.time.ElapsedClock
import com.kartikkhandelwal.wigglebun.domain.model.ActivePet
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.session.SessionTimer
import com.kartikkhandelwal.wigglebun.ui.navigation.BreakSessionRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The countdown only redraws this often; the time itself always comes from [ElapsedClock]. */
private const val TICK_MILLIS = 100L
private const val MILLIS_PER_MINUTE = 60_000L

@HiltViewModel
class BreakSessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recordBreakDone: RecordBreakDone,
    private val elapsed: ElapsedClock,
    petRepository: PetRepository,
) : ViewModel() {

    val type: BreakType = savedStateHandle.toRoute<BreakSessionRoute>().breakType

    private var timer = SessionTimer.start(
        totalMillis = type.durationMinutes * MILLIS_PER_MINUTE,
        stepCount = type.sessionSteps().size,
        now = elapsed.nowMillis(),
    )
    private var pet: ActivePet? = null
    private var result: BreakDoneResult? = null
    private var completing = false

    private val _state = MutableStateFlow(sessionUiState(type, pet, timer, elapsed.nowMillis(), result))
    val state: StateFlow<BreakSessionUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            petRepository.activePet.collect {
                pet = it
                publish()
            }
        }
        viewModelScope.launch {
            // Tick only while the screen is collecting (collectAsStateWithLifecycle stops at
            // ON_STOP), so a pocketed 2-min walk doesn't redraw 10× a second in the background.
            // The timer is elapsed-time based, so the first tick on return shows the real time.
            _state.subscriptionCount.collectLatest { subscribers ->
                while (subscribers > 0) {
                    delay(TICK_MILLIS)
                    if (timer.isRunning && result == null) publish()
                }
            }
        }
    }

    fun togglePause() {
        val now = elapsed.nowMillis()
        timer = if (timer.isRunning) timer.pause(now) else timer.resume(now)
        publish(now)
    }

    fun nextStep() {
        val now = elapsed.nowMillis()
        timer = timer.next(now)
        publish(now)
    }

    /**
     * The app went to the background (ON_STOP). Sessions pause, except the 2-min walk: people
     * pocket the phone and lock the screen to walk. Its timer is elapsed-time based, so on return
     * it shows the real remaining time (or the last step at 0:00 with Done).
     */
    fun onBackground() {
        if (type == BreakType.WALK || result != null) return
        val now = elapsed.nowMillis()
        timer = timer.pause(now)
        publish(now)
    }

    /** Close (X) or Back mid-session: nothing is logged or paid and the sit timer is untouched. */
    fun close() {
        val now = elapsed.nowMillis()
        timer = timer.pause(now)
        publish(now)
    }

    /** The one place a session completes: log, reset the sit timer, pay (once), then celebrate. */
    fun done() {
        if (completing) return
        completing = true
        val now = elapsed.nowMillis()
        timer = timer.pause(now)
        publish(now)
        viewModelScope.launch {
            result = recordBreakDone(type)
            publish()
        }
    }

    private fun publish(now: Long = elapsed.nowMillis()) {
        timer = timer.advance(now)
        _state.value = sessionUiState(type, pet, timer, now, result)
    }
}
