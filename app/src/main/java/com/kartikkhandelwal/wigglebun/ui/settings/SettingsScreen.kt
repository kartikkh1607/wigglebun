package com.kartikkhandelwal.wigglebun.ui.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kartikkhandelwal.wigglebun.BuildConfig
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.sitting.SittingSettings
import com.kartikkhandelwal.wigglebun.domain.sitting.WorkSchedule
import com.kartikkhandelwal.wigglebun.ui.collection.PetSheet
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButton
import com.kartikkhandelwal.wigglebun.ui.components.PillCard
import com.kartikkhandelwal.wigglebun.ui.home.BreakIconTile
import com.kartikkhandelwal.wigglebun.ui.labelRes
import com.kartikkhandelwal.wigglebun.ui.onboarding.DayChips
import com.kartikkhandelwal.wigglebun.ui.onboarding.IntervalSheetContent
import com.kartikkhandelwal.wigglebun.ui.onboarding.SheetStillFrame
import com.kartikkhandelwal.wigglebun.ui.onboarding.TimeRow
import com.kartikkhandelwal.wigglebun.ui.onboarding.TimeSheetContent
import com.kartikkhandelwal.wigglebun.ui.onboarding.WorkHoursDraft
import com.kartikkhandelwal.wigglebun.ui.onboarding.WorkHoursProblem
import com.kartikkhandelwal.wigglebun.ui.onboarding.WorkTime
import com.kartikkhandelwal.wigglebun.ui.setup.SetupAction
import com.kartikkhandelwal.wigglebun.ui.setup.SetupCard
import com.kartikkhandelwal.wigglebun.ui.setup.SetupCardRow
import com.kartikkhandelwal.wigglebun.ui.setup.SetupCardUi
import com.kartikkhandelwal.wigglebun.ui.setup.SetupCardsRoute
import com.kartikkhandelwal.wigglebun.ui.setup.SetupCardsUi
import com.kartikkhandelwal.wigglebun.ui.setup.SetupCardsViewModel
import com.kartikkhandelwal.wigglebun.ui.setup.SetupExplainContent
import com.kartikkhandelwal.wigglebun.ui.setup.SetupStatus
import com.kartikkhandelwal.wigglebun.ui.theme.Card
import com.kartikkhandelwal.wigglebun.ui.theme.Coral
import com.kartikkhandelwal.wigglebun.ui.theme.Cream
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.DoneGreen
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.InkMuted
import com.kartikkhandelwal.wigglebun.ui.theme.Track
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunShapes
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme
import java.time.DayOfWeek

private val S = Dimens.Settings

/**
 * Not hosted yet (CLAUDE.md §12). Until it is, the row says "Coming soon"; the Play Store requires
 * a real one before release.
 */
private val PRIVACY_POLICY_URL: String? = null

/** What previews and screenshot tests show, so a version bump doesn't change reference images. */
private const val PREVIEW_VERSION = "1.0.0"

class SettingsActions(
    val onOpenSetup: (SetupCard) -> Unit = {},
    val onToggleDay: (DayOfWeek) -> Unit = {},
    val onPickTime: (WorkTime) -> Unit = {},
    val onSaveHours: () -> Unit = {},
    val onPickInterval: () -> Unit = {},
    val onBreakToggled: (BreakType, Boolean) -> Unit = { _, _ -> },
    val onOpenPrivacy: (() -> Unit)? = null,
)

@Composable
fun SettingsScreenRoute(
    modifier: Modifier = Modifier,
    setupViewModel: SetupCardsViewModel = hiltViewModel(),
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val loaded = state
    if (loaded == null) {
        Box(modifier.fillMaxSize().background(Cream))
        return
    }
    val context = LocalContext.current
    var pickingTime by rememberSaveable { mutableStateOf<WorkTime?>(null) }
    var pickingInterval by rememberSaveable { mutableStateOf(false) }
    SetupCardsRoute(setupViewModel) { setup, openSetup ->
        SettingsScreen(
            state = loaded,
            setup = setup,
            actions = SettingsActions(
                onOpenSetup = openSetup,
                onToggleDay = viewModel::onToggleDay,
                onPickTime = { pickingTime = it },
                onSaveHours = viewModel::onSaveHours,
                onPickInterval = { pickingInterval = true },
                onBreakToggled = viewModel::onBreakToggled,
                onOpenPrivacy = PRIVACY_POLICY_URL?.let { url ->
                    {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        } catch (_: ActivityNotFoundException) {
                            // No browser on the phone: nothing else to try.
                        }
                    }
                },
            ),
            modifier = modifier,
        )
    }
    pickingTime?.let { time ->
        PetSheet(onDismiss = { pickingTime = null }) {
            TimeSheetContent(
                time = time,
                minute = if (time == WorkTime.START) loaded.hours.startMinute else loaded.hours.endMinute,
                onDone = {
                    viewModel.onTimePicked(time, it)
                    pickingTime = null
                },
            )
        }
    }
    if (pickingInterval) {
        PetSheet(onDismiss = { pickingInterval = false }) {
            IntervalSheetContent(
                initial = loaded.intervalMinutes,
                onDone = {
                    viewModel.onIntervalPicked(it)
                    pickingInterval = false
                },
            )
        }
    }
}

/** The Menu tab (CLAUDE.md §6.7). The Premium card comes with the paywall in M10. */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    setup: SetupCardsUi,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
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
        setup.cards.forEach { ui -> SetupCardRow(ui, onOpen = { actions.onOpenSetup(ui.card) }) }

        SectionLabel(R.string.settings_work_hours)
        PillCard(modifier = Modifier.fillMaxWidth()) {
            DayChips(state.hours.days, actions.onToggleDay)
        }
        TimeRow(R.string.hours_start, state.hours.startMinute, onClick = { actions.onPickTime(WorkTime.START) })
        TimeRow(
            label = R.string.hours_end,
            minute = state.hours.endMinute,
            note = if (state.hours.endsNextDay) stringResource(R.string.hours_next_day) else null,
            onClick = { actions.onPickTime(WorkTime.END) },
        )
        if (state.hoursChanged) {
            // No work days shows at once and turns Save off; the same start and end only shows once
            // Save has found it. Either way the reason sits right above the button.
            state.hoursProblem?.let { problem ->
                Text(
                    stringResource(
                        when (problem) {
                            WorkHoursProblem.NO_DAYS -> R.string.hours_no_days
                            WorkHoursProblem.SAME_TIME -> R.string.hours_same_time
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Coral,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            ChunkyButton(
                text = stringResource(R.string.settings_save_hours),
                onClick = actions.onSaveHours,
                enabled = state.canSaveHours,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionLabel(R.string.settings_breaks)
        PillCard(modifier = Modifier.fillMaxWidth(), onClick = actions.onPickInterval) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.interval_sheet),
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(R.string.interval_minutes, state.intervalMinutes),
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink,
                )
            }
        }
        BreakToggles(state.enabledBreaks, actions.onBreakToggled)

        SectionLabel(R.string.settings_about)
        PillCard(modifier = Modifier.fillMaxWidth(), onClick = actions.onOpenPrivacy) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.settings_privacy),
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                    modifier = Modifier.weight(1f),
                )
                if (actions.onOpenPrivacy == null) {
                    Text(stringResource(R.string.coming_soon), style = MaterialTheme.typography.labelMedium, color = InkMuted)
                }
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(vertical = S.AboutVertical),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.settings_made_in), style = MaterialTheme.typography.bodyMedium, color = InkMuted)
            Text(
                stringResource(
                    R.string.settings_version,
                    if (LocalInspectionMode.current) PREVIEW_VERSION else BuildConfig.VERSION_NAME,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = InkMuted,
            )
        }
    }
}

@Composable
private fun SectionLabel(label: Int) {
    Text(
        stringResource(label),
        style = MaterialTheme.typography.titleLarge,
        color = Ink,
        modifier = Modifier.padding(top = S.SectionTop).semantics { heading() },
    )
}

/** Which breaks show on Home (CLAUDE.md §5.3). The last one can't be switched off. */
@Composable
private fun BreakToggles(enabled: List<BreakType>, onToggle: (BreakType, Boolean) -> Unit) {
    val onlyOneLeft = enabled.size == 1
    PillCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.settings_breaks_hint), style = MaterialTheme.typography.bodyMedium, color = InkMuted)
        BreakType.entries.forEach { type ->
            val isOn = type in enabled
            Row(
                Modifier.fillMaxWidth().padding(top = S.ToggleTop),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(S.ToggleGap),
            ) {
                BreakIconTile(type, S.ToggleTile, WigglebunShapes.IconTile, S.ToggleIcon)
                Text(
                    stringResource(type.labelRes),
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = isOn,
                    enabled = !(isOn && onlyOneLeft),
                    onCheckedChange = { onToggle(type, it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Card,
                        checkedTrackColor = DoneGreen,
                        uncheckedThumbColor = InkMuted,
                        uncheckedTrackColor = Track,
                        disabledCheckedThumbColor = Card,
                        disabledCheckedTrackColor = InkMuted,
                    ),
                )
            }
        }
        if (onlyOneLeft) {
            Text(
                stringResource(R.string.settings_keep_one),
                style = MaterialTheme.typography.bodyMedium,
                color = InkMuted,
                modifier = Modifier.padding(top = S.ToggleTop),
            )
        }
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

    val Default = SettingsUiState(
        hours = WorkHoursDraft.of(WorkSchedule.Default),
        hoursChanged = false,
        hoursProblem = null,
        intervalMinutes = SittingSettings.DEFAULT_INTERVAL_MINUTES,
        enabledBreaks = BreakType.entries,
    )
}

@Preview(widthDp = 390, heightDp = 760)
@Composable
internal fun SettingsAllOffPreview() {
    WigglebunTheme { SettingsScreen(SettingsPreviewStates.Default, SettingsPreviewStates.AllOff, SettingsActions()) }
}

@Preview(widthDp = 390, heightDp = 760)
@Composable
internal fun SettingsMixedPreview() {
    WigglebunTheme { SettingsScreen(SettingsPreviewStates.Default, SettingsPreviewStates.Mixed, SettingsActions()) }
}

@Preview(widthDp = 360, heightDp = 640, fontScale = 1.3f)
@Composable
internal fun SettingsSmallLargeFontPreview() {
    WigglebunTheme { SettingsScreen(SettingsPreviewStates.Default, SettingsPreviewStates.AllOff, SettingsActions()) }
}

/** The whole page, for reviewing what's below the fold. */
@Preview(widthDp = 390, heightDp = 1500)
@Composable
internal fun SettingsFullPreview() {
    WigglebunTheme { SettingsScreen(SettingsPreviewStates.Default, SettingsPreviewStates.Mixed, SettingsActions()) }
}

/** Overnight shift, and only one break left on, which can't be switched off. */
@Preview(widthDp = 390, heightDp = 1500)
@Composable
internal fun SettingsOvernightOneBreakPreview() {
    val state = SettingsPreviewStates.Default.copy(
        hours = WorkHoursDraft.of(WorkSchedule.Default).copy(startMinute = 22 * 60, endMinute = 6 * 60),
        enabledBreaks = listOf(BreakType.WATER),
    )
    WigglebunTheme { SettingsScreen(state, SettingsPreviewStates.Mixed, SettingsActions()) }
}

/** Halfway from 9–18 to 18–02: start and end match for now, which is fine until Save. */
@Preview(widthDp = 390, heightDp = 1500)
@Composable
internal fun SettingsHoursEditingPreview() {
    val state = SettingsPreviewStates.Default.copy(
        hours = WorkHoursDraft.of(WorkSchedule.Default).copy(startMinute = 18 * 60),
        hoursChanged = true,
    )
    WigglebunTheme { SettingsScreen(state, SettingsPreviewStates.Mixed, SettingsActions()) }
}

/** Every work day switched off: Save is off at once, and the reason shows above it. */
@Preview(widthDp = 390, heightDp = 1500)
@Composable
internal fun SettingsHoursNoDaysPreview() {
    val hours = WorkHoursDraft.of(WorkSchedule.Default).copy(days = emptySet())
    val state = SettingsPreviewStates.Default.copy(
        hours = hours,
        hoursChanged = true,
        hoursProblem = hours.shownProblem(null),
    )
    WigglebunTheme { SettingsScreen(state, SettingsPreviewStates.Mixed, SettingsActions()) }
}

/** Saved while start and end match: nothing is saved, and the reason shows by Save. */
@Preview(widthDp = 390, heightDp = 1500)
@Composable
internal fun SettingsHoursProblemPreview() {
    val state = SettingsPreviewStates.Default.copy(
        hours = WorkHoursDraft.of(WorkSchedule.Default).copy(startMinute = 18 * 60),
        hoursChanged = true,
        hoursProblem = WorkHoursProblem.SAME_TIME,
    )
    WigglebunTheme { SettingsScreen(state, SettingsPreviewStates.Mixed, SettingsActions()) }
}

@Composable
internal fun SettingsExplainPreview(ui: SetupCardUi) {
    SheetStillFrame(
        screen = { SettingsScreen(SettingsPreviewStates.Default, SettingsPreviewStates.AllOff, SettingsActions()) },
        sheet = { SetupExplainContent(ui, {}, {}) },
    )
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun SettingsTimeSheetPreview() {
    SheetStillFrame(
        screen = { SettingsScreen(SettingsPreviewStates.Default, SettingsPreviewStates.Mixed, SettingsActions()) },
        sheet = { TimeSheetContent(WorkTime.END, SettingsPreviewStates.Default.hours.endMinute, onDone = {}) },
    )
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun SettingsIntervalSheetPreview() {
    SheetStillFrame(
        screen = { SettingsScreen(SettingsPreviewStates.Default, SettingsPreviewStates.Mixed, SettingsActions()) },
        sheet = { IntervalSheetContent(SettingsPreviewStates.Default.intervalMinutes, onDone = {}) },
    )
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun SettingsExplainNotificationsPreview() = SettingsExplainPreview(SettingsPreviewStates.Notifications)
