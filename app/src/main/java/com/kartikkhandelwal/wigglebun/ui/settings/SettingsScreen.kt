package com.kartikkhandelwal.wigglebun.ui.settings

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.ui.setup.SetupAction
import com.kartikkhandelwal.wigglebun.ui.setup.SetupCard
import com.kartikkhandelwal.wigglebun.ui.setup.SetupCardRow
import com.kartikkhandelwal.wigglebun.ui.setup.SetupCardUi
import com.kartikkhandelwal.wigglebun.ui.setup.SetupCardsRoute
import com.kartikkhandelwal.wigglebun.ui.setup.SetupCardsUi
import com.kartikkhandelwal.wigglebun.ui.setup.SetupCardsViewModel
import com.kartikkhandelwal.wigglebun.ui.setup.SetupExplainContent
import com.kartikkhandelwal.wigglebun.ui.setup.SetupStatus
import com.kartikkhandelwal.wigglebun.ui.onboarding.SheetStillFrame
import com.kartikkhandelwal.wigglebun.ui.theme.Cream
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme

@Composable
fun SettingsScreenRoute(modifier: Modifier = Modifier, viewModel: SetupCardsViewModel = hiltViewModel()) {
    SetupCardsRoute(viewModel) { state, open -> SettingsScreen(state, open, modifier) }
}

/** The Menu tab (CLAUDE.md §6.7). So far only the just-in-time setup cards. */
@Composable
fun SettingsScreen(state: SetupCardsUi, onOpen: (SetupCard) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .background(Cream)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.ListGap),
    ) {
        Text(
            stringResource(R.string.tab_settings),
            style = MaterialTheme.typography.displayMedium,
            color = Ink,
            modifier = Modifier.semantics { heading() },
        )
        state.cards.forEach { ui -> SetupCardRow(ui, onOpen = { onOpen(ui.card) }) }
    }
}

internal object SettingsPreviewStates {
    private val allow = SetupAction.RequestPermission(Manifest.permission.POST_NOTIFICATIONS)
    private val notifications = SetupCardUi(SetupCard.NOTIFICATIONS, SetupStatus.OFF, allow)
    private val movement = SetupCardUi(SetupCard.MOVEMENT, SetupStatus.OFF, SetupAction.RequestPermission(Manifest.permission.ACTIVITY_RECOGNITION))
    private val widget = SetupCardUi(SetupCard.WIDGET, SetupStatus.OFF, SetupAction.PinWidget)

    val AllOff = SetupCardsUi(listOf(notifications, movement, widget), homePrompt = notifications)
    val Mixed = SetupCardsUi(
        listOf(
            notifications.copy(status = SetupStatus.ON, action = SetupAction.None),
            movement.copy(status = SetupStatus.BLOCKED, action = SetupAction.OpenAppSettings),
            widget.copy(status = SetupStatus.ON, action = SetupAction.None),
        ),
        homePrompt = null,
    )
    val ManualWidget = widget.copy(status = SetupStatus.MANUAL, action = SetupAction.None)
    val BlockedMovement = movement.copy(status = SetupStatus.BLOCKED, action = SetupAction.OpenAppSettings)
    val Notifications = notifications
    val Movement = movement
    val Widget = widget
}

@Preview(widthDp = 390, heightDp = 760)
@Composable
internal fun SettingsAllOffPreview() {
    WigglebunTheme { SettingsScreen(SettingsPreviewStates.AllOff, {}) }
}

@Preview(widthDp = 390, heightDp = 760)
@Composable
internal fun SettingsMixedPreview() {
    WigglebunTheme { SettingsScreen(SettingsPreviewStates.Mixed, {}) }
}

@Preview(widthDp = 360, heightDp = 640, fontScale = 1.3f)
@Composable
internal fun SettingsSmallLargeFontPreview() {
    WigglebunTheme { SettingsScreen(SettingsPreviewStates.AllOff, {}) }
}

@Composable
internal fun SettingsExplainPreview(ui: SetupCardUi) {
    SheetStillFrame(
        screen = { SettingsScreen(SettingsPreviewStates.AllOff, {}) },
        sheet = { SetupExplainContent(ui, {}, {}) },
    )
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun SettingsExplainNotificationsPreview() = SettingsExplainPreview(SettingsPreviewStates.Notifications)
