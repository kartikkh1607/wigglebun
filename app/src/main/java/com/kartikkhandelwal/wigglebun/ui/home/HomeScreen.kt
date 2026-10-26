package com.kartikkhandelwal.wigglebun.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.data.prefs.SettingsRepository
import com.kartikkhandelwal.wigglebun.data.repo.SitRepository
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.ui.components.PillCard
import com.kartikkhandelwal.wigglebun.ui.pet.PetView
import com.kartikkhandelwal.wigglebun.ui.theme.Cream
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.InkMuted
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Placeholder Home (final design is M5): the engine-driven pet and minutes since the last break. */
data class HomeUiState(
    val energy: Float = 1f,
    val mood: Mood = Mood.HAPPY,
    val minutesSinceBreak: Int = 0,
    val isWorkTime: Boolean = true,
    /** Null until the user names the pet (M9); the screen falls back to "Bun". */
    val petName: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    sitRepository: SitRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<HomeUiState> = combine(sitRepository.status, settingsRepository.petName) { status, name ->
        HomeUiState(
            energy = status.energy,
            mood = status.mood,
            minutesSinceBreak = status.sitMinutes,
            isWorkTime = status.isWorkTime,
            petName = name,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}

@Composable
fun HomeScreenRoute(
    modifier: Modifier = Modifier,
    debugEntry: @Composable () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(state = state, modifier = modifier, debugEntry = debugEntry)
}

/**
 * @param debugEntry debug builds put a "Pet lab" button here; empty in release.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    modifier: Modifier = Modifier,
    debugEntry: @Composable () -> Unit = {},
) {
    val name = state.petName ?: stringResource(R.string.pet_default_name)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Cream)
            .padding(Dimens.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.ListGap, Alignment.CenterVertically),
    ) {
        PetView(
            energy = state.energy,
            mood = state.mood,
            name = name,
            modifier = Modifier.size(220.dp, 231.dp),
        )
        PillCard(modifier = Modifier.fillMaxWidth()) {
            if (state.isWorkTime) {
                Text(
                    text = stringResource(R.string.home_time_since_break),
                    style = MaterialTheme.typography.labelMedium,
                    color = InkMuted,
                )
                Text(
                    text = pluralStringResource(R.plurals.home_minutes, state.minutesSinceBreak, state.minutesSinceBreak),
                    style = MaterialTheme.typography.displayMedium,
                )
            } else {
                Text(
                    text = stringResource(R.string.home_asleep, name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        debugEntry()
    }
}

/** Sample states for previews and screenshot tests. */
internal object HomePreviewStates {
    val Happy = HomeUiState(energy = 0.85f, mood = Mood.HAPPY, minutesSinceBreak = 12)
    val Tired = HomeUiState(energy = 0.41f, mood = Mood.TIRED, minutesSinceBreak = 32)
    val Stiff = HomeUiState(energy = 0.15f, mood = Mood.STIFF, minutesSinceBreak = 52)
    val Asleep = HomeUiState(energy = 1f, mood = Mood.SLEEPING, isWorkTime = false)
    val All = listOf("happy" to Happy, "tired" to Tired, "stiff" to Stiff, "asleep" to Asleep)
}

@Preview(widthDp = 390, heightDp = 760)
@Composable
private fun HomeTiredPreview() {
    WigglebunTheme { HomeScreen(HomePreviewStates.Tired) }
}

@Preview(widthDp = 390, heightDp = 760)
@Composable
private fun HomeAsleepPreview() {
    WigglebunTheme { HomeScreen(HomePreviewStates.Asleep) }
}
