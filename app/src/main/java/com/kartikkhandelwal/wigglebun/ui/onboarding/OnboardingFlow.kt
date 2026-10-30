package com.kartikkhandelwal.wigglebun.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.data.prefs.SettingsRepository
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButton
import com.kartikkhandelwal.wigglebun.ui.components.EmptyScreen
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(private val settings: SettingsRepository) : ViewModel() {
    /** Flips the [AppGate] over to the main app. */
    fun finish() {
        viewModelScope.launch { settings.finishOnboarding() }
    }
}

@Composable
fun OnboardingFlowRoute(viewModel: OnboardingViewModel = hiltViewModel()) {
    OnboardingPlaceholder(onContinue = viewModel::finish)
}

/** Stands in until the wake-up screens exist. */
@Composable
private fun OnboardingPlaceholder(onContinue: () -> Unit, modifier: Modifier = Modifier) {
    EmptyScreen(title = stringResource(R.string.app_name), modifier = modifier) {
        ChunkyButton(text = stringResource(R.string.onboarding_continue), onClick = onContinue)
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun OnboardingPlaceholderPreview() {
    WigglebunTheme { OnboardingPlaceholder(onContinue = {}) }
}
