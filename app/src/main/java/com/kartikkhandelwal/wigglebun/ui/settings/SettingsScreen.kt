package com.kartikkhandelwal.wigglebun.ui.settings

import androidx.compose.ui.graphics.Color
import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kartikkhandelwal.wigglebun.BuildConfig
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.domain.model.ActivePet
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.domain.model.PetId
import com.kartikkhandelwal.wigglebun.domain.sitting.SittingSettings
import com.kartikkhandelwal.wigglebun.domain.sitting.WorkSchedule
import com.kartikkhandelwal.wigglebun.feedback.tickClickable
import com.kartikkhandelwal.wigglebun.ui.collection.PetSheet
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButton
import androidx.annotation.DrawableRes
import com.kartikkhandelwal.wigglebun.feedback.ConsumeFeel
import com.kartikkhandelwal.wigglebun.feedback.LocalFeel
import com.kartikkhandelwal.wigglebun.ui.components.ChunkySurface
import com.kartikkhandelwal.wigglebun.ui.components.GroupedCard
import com.kartikkhandelwal.wigglebun.ui.components.LegalLinks
import com.kartikkhandelwal.wigglebun.ui.components.SettingsRow
import com.kartikkhandelwal.wigglebun.ui.components.StatusChip
import com.kartikkhandelwal.wigglebun.ui.home.formatTime
import com.kartikkhandelwal.wigglebun.ui.home.minuteOfDay
import com.kartikkhandelwal.wigglebun.ui.iconRes
import com.kartikkhandelwal.wigglebun.ui.labelRes
import com.kartikkhandelwal.wigglebun.ui.onboarding.DayChips
import com.kartikkhandelwal.wigglebun.ui.onboarding.IntervalSheetContent
import com.kartikkhandelwal.wigglebun.ui.onboarding.SheetStillFrame
import com.kartikkhandelwal.wigglebun.ui.onboarding.TimeSheetContent
import com.kartikkhandelwal.wigglebun.ui.onboarding.WorkHoursDraft
import com.kartikkhandelwal.wigglebun.ui.onboarding.WorkHoursProblem
import com.kartikkhandelwal.wigglebun.ui.onboarding.WorkTime
import com.kartikkhandelwal.wigglebun.ui.pet.PetNameSentence
import com.kartikkhandelwal.wigglebun.ui.pet.PetView
import com.kartikkhandelwal.wigglebun.ui.pet.displayName
import com.kartikkhandelwal.wigglebun.ui.pet.spec
import com.kartikkhandelwal.wigglebun.ui.setup.SetupAction
import com.kartikkhandelwal.wigglebun.ui.setup.SetupCard
import com.kartikkhandelwal.wigglebun.ui.setup.SetupCardUi
import com.kartikkhandelwal.wigglebun.ui.setup.SetupCardsRoute
import com.kartikkhandelwal.wigglebun.ui.setup.SetupCardsUi
import com.kartikkhandelwal.wigglebun.ui.setup.SetupCardsViewModel
import com.kartikkhandelwal.wigglebun.ui.setup.SetupExplainContent
import com.kartikkhandelwal.wigglebun.ui.setup.SetupSettingsRow
import com.kartikkhandelwal.wigglebun.ui.setup.SetupStatus
import com.kartikkhandelwal.wigglebun.ui.theme.Sun
import com.kartikkhandelwal.wigglebun.ui.tint
import com.kartikkhandelwal.wigglebun.ui.theme.BreakEyesTint
import com.kartikkhandelwal.wigglebun.ui.theme.BreakNeckTint
import com.kartikkhandelwal.wigglebun.ui.theme.BreakStandTint
import com.kartikkhandelwal.wigglebun.ui.theme.BreakWalkTint
import com.kartikkhandelwal.wigglebun.ui.theme.BreakWristTint
import com.kartikkhandelwal.wigglebun.ui.theme.Card
import com.kartikkhandelwal.wigglebun.ui.theme.Coral
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.DoneGreen
import com.kartikkhandelwal.wigglebun.ui.theme.GroupDivider
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.InkMuted
import com.kartikkhandelwal.wigglebun.ui.theme.Night
import com.kartikkhandelwal.wigglebun.ui.theme.NightEdge
import com.kartikkhandelwal.wigglebun.ui.theme.NightSubtle
import com.kartikkhandelwal.wigglebun.ui.theme.NightTile
import com.kartikkhandelwal.wigglebun.ui.theme.OnSurface
import com.kartikkhandelwal.wigglebun.ui.theme.Surfaces
import com.kartikkhandelwal.wigglebun.ui.theme.Track
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunShapes
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTextStyles
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme
import java.time.DayOfWeek

private val S = Dimens.Settings

/** What previews and screenshot tests show, so a version bump doesn't change reference images. */
private const val PREVIEW_VERSION = "1.0.0"

class SettingsActions(
    val onOpenSetup: (SetupCard) -> Unit = {},
    val onToggleDay: (DayOfWeek) -> Unit = {},
    val onPickTime: (WorkTime) -> Unit = {},
    val onSaveHours: () -> Unit = {},
    val onPickInterval: () -> Unit = {},
    val onBreakToggled: (BreakType, Boolean) -> Unit = { _, _ -> },
    val onSoundsToggled: (Boolean) -> Unit = {},
    val onHapticsToggled: (Boolean) -> Unit = {},
    val onOpenPrivacy: () -> Unit = {},
    val onOpenTerms: () -> Unit = {},
    /** The "Bun & you" card opens the Pet tab. */
    val onOpenPet: () -> Unit = {},
    /** Its "Unlock" chip, shown while the app is locked, opens the plans. */
    val onOpenPaywall: () -> Unit = {},
)

@Composable
fun SettingsScreenRoute(
    modifier: Modifier = Modifier,
    onOpenPet: () -> Unit = {},
    onOpenPaywall: () -> Unit = {},
    setupViewModel: SetupCardsViewModel = hiltViewModel(),
    viewModel: SettingsViewModel = hiltViewModel(),
    badgeViewModel: PremiumBadgeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val loaded = state
    if (loaded == null) {
        Box(modifier.fillMaxSize().background(Surfaces.Settings.background))
        return
    }
    ConsumeFeel(viewModel.feel)
    val badge by badgeViewModel.badge.collectAsStateWithLifecycle()
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
                onSoundsToggled = viewModel::onSoundsToggled,
                onHapticsToggled = viewModel::onHapticsToggled,
                onPickInterval = { pickingInterval = true },
                onBreakToggled = viewModel::onBreakToggled,
                onOpenPet = onOpenPet,
                onOpenPaywall = onOpenPaywall,
                onOpenPrivacy = { LegalLinks.open(context, LegalLinks.PRIVACY_URL) },
                onOpenTerms = { LegalLinks.open(context, LegalLinks.TERMS_URL) },
            ),
            modifier = modifier,
            badge = badge,
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

/**
 * The Settings tab (CLAUDE.md §6.7, design/ui_v2/Settings.dc.html): the "Bun & you" card, then
 * grouped cards for the setup, work hours, breaks and about. [badge] is the chip on that card.
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    setup: SetupCardsUi,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
    badge: PremiumBadge? = null,
) {
    OnSurface(Surfaces.Settings) { SettingsContent(state, setup, actions, modifier) }
}

@Composable
private fun SettingsContent(
    state: SettingsUiState,
    setup: SetupCardsUi,
    actions: SettingsActions,
    modifier: Modifier,
) {
    val petName = state.pet.displayName()
    Column(
        modifier
            .fillMaxSize()
            .background(Surfaces.Settings.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.ScreenPadding),
    ) {
        Text(
            stringResource(R.string.tab_settings),
            style = WigglebunTextStyles.TabScreenTitle,
            color = Ink,
            modifier = Modifier.padding(start = S.TextInset, end = S.TextInset, bottom = S.TitleBottom).semantics { heading() },
        )
        PetAndYouCard(state, petName, badge, actions)

        SectionLabel(R.string.settings_your_setup)
        GroupedCard {
            setup.cards.forEach { ui ->
                row { SetupSettingsRow(ui, petName, onOpen = { actions.onOpenSetup(ui.card) }) }
            }
        }

        SectionLabel(R.string.settings_work_hours)
        WorkHoursCard(state, actions)
        if (state.hoursChanged) HoursSave(state, actions.onSaveHours)

        SectionLabel(R.string.settings_breaks)
        BreaksCard(state, actions)

        SectionLabel(R.string.settings_feel)
        FeelCard(state, actions)

        SectionLabel(R.string.settings_about)
        GroupedCard {
            row {
                SettingsRow(
                    icon = R.drawable.ic_settings_shield,
                    tile = BreakWalkTint,
                    title = stringResource(R.string.settings_privacy),
                    onClick = actions.onOpenPrivacy,
                )
            }
            row {
                SettingsRow(
                    icon = R.drawable.ic_settings_terms,
                    tile = BreakNeckTint,
                    title = stringResource(R.string.settings_terms),
                    onClick = actions.onOpenTerms,
                )
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

/**
 * "Bun & you" on Night: the pet with you (still, wearing its outfit), its streak and how many
 * breaks so far. Tapping it opens the Pet tab. The name never adds a line ([PetNameSentence]).
 */
@Composable
private fun PetAndYouCard(state: SettingsUiState, petName: String, badge: PremiumBadge?, actions: SettingsActions) {
    val summary = petSummary(state.streakDays, state.totalBreaks)
    // With a big font the chip goes under the text, like a row's value (SettingsRow), so the name keeps its room.
    val badgeBelow = LocalDensity.current.fontScale >= S.BadgeBelowFontScale
    ChunkySurface(
        shape = WigglebunShapes.Card,
        faceColor = Night,
        edgeColor = NightEdge,
        edgeHeight = S.HeaderEdge,
        onClick = actions.onOpenPet,
        onClickLabel = stringResource(R.string.settings_open_profile),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = S.HeaderPaddingStart, end = S.HeaderPaddingEnd, top = S.HeaderPaddingVertical, bottom = S.HeaderPaddingVertical),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(S.HeaderGap),
        ) {
            Box(
                Modifier.size(S.HeaderTile).clip(RoundedCornerShape(S.HeaderTileRadius)).background(NightTile),
                contentAlignment = Alignment.Center,
            ) {
                PetView(
                    energy = 1f,
                    mood = Mood.HAPPY,
                    spec = state.pet.spec,
                    name = petName,
                    accessories = state.pet.wearing,
                    animated = false,
                    modifier = Modifier.size(S.HeaderPetWidth, S.HeaderPetHeight).clearAndSetSemantics {},
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(S.HeaderTextGap)) {
                PetNameSentence(
                    R.string.settings_pet_and_you,
                    petName,
                    style = WigglebunTextStyles.HeaderCardTitle,
                    color = Card,
                )
                Text(summary, style = WigglebunTextStyles.HeaderCardSub, color = NightSubtle)
                if (badge != null && badgeBelow) PremiumChip(badge, actions.onOpenPaywall)
            }
            if (badge != null && !badgeBelow) PremiumChip(badge, actions.onOpenPaywall)
        }
    }
}

/** "Premium" / "Free trial" just say so; "Unlock" (the app is locked) opens the plans. */
@Composable
private fun PremiumChip(badge: PremiumBadge, onOpenPaywall: () -> Unit) {
    val text = stringResource(
        when (badge) {
            PremiumBadge.Premium -> R.string.settings_badge_premium
            PremiumBadge.Trial -> R.string.settings_badge_trial
            PremiumBadge.Unlock -> R.string.settings_badge_unlock
        },
    )
    val tap = if (badge == PremiumBadge.Unlock) {
        Modifier.tickClickable(onClickLabel = stringResource(R.string.settings_badge_unlock_label), onClick = onOpenPaywall)
    } else {
        Modifier
    }
    StatusChip(text, modifier = Modifier.clip(WigglebunShapes.Pill).then(tap), background = Sun, content = Ink)
}

/** "5-day streak · 42 breaks so far"; no streak part at 0, "No breaks yet" before the first. */
@Composable
private fun petSummary(streakDays: Int, totalBreaks: Int): String {
    val breaks = if (totalBreaks == 0) {
        stringResource(R.string.settings_no_breaks_yet)
    } else {
        pluralStringResource(R.plurals.settings_breaks_so_far, totalBreaks, totalBreaks)
    }
    if (streakDays == 0) return breaks
    return stringResource(R.string.settings_pet_summary, pluralStringResource(R.plurals.settings_streak_days, streakDays, streakDays), breaks)
}

@Composable
private fun WorkHoursCard(state: SettingsUiState, actions: SettingsActions) {
    GroupedCard {
        row {
            DayChips(
                state.hours.days,
                actions.onToggleDay,
                modifier = Modifier.padding(horizontal = S.DaysPaddingHorizontal, vertical = S.DaysPaddingVertical),
                offColor = GroupDivider,
            )
        }
        row {
            SettingsRow(
                icon = R.drawable.ic_settings_sunrise,
                tile = BreakStandTint,
                title = stringResource(R.string.hours_start),
                onClick = { actions.onPickTime(WorkTime.START) },
            ) { TimeValue(state.hours.startMinute) }
        }
        row {
            SettingsRow(
                icon = R.drawable.ic_settings_moon,
                tile = BreakEyesTint,
                title = stringResource(R.string.hours_end),
                subtitle = if (state.hours.endsNextDay) stringResource(R.string.hours_next_day) else null,
                onClick = { actions.onPickTime(WorkTime.END) },
            ) { TimeValue(state.hours.endMinute) }
        }
    }
}

@Composable
private fun TimeValue(minute: Int) {
    Text(formatTime(minuteOfDay(minute)), style = MaterialTheme.typography.titleLarge, color = Ink)
}

/**
 * Under the work hours card while they differ from what's stored. No work days shows at once and
 * turns Save off; the same start and end only shows once Save has found it. Either way the
 * reason sits right above the button.
 */
@Composable
private fun HoursSave(state: SettingsUiState, onSave: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = S.HoursSaveTop),
        verticalArrangement = Arrangement.spacedBy(Dimens.ListGap),
    ) {
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
            onClick = onSave,
            enabled = state.canSaveHours,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** The interval, then which breaks show on Home (CLAUDE.md §5.3); the last one can't be switched off. */
@Composable
private fun BreaksCard(state: SettingsUiState, actions: SettingsActions) {
    val onlyOneLeft = state.enabledBreaks.size == 1
    val inset = Dimens.Shared.RowPaddingHorizontal
    GroupedCard {
        row {
            SettingsRow(
                icon = R.drawable.ic_settings_timer,
                tile = BreakWristTint,
                title = stringResource(R.string.interval_sheet),
                onClick = actions.onPickInterval,
            ) {
                Text(stringResource(R.string.interval_minutes, state.intervalMinutes), style = MaterialTheme.typography.titleLarge, color = Ink)
            }
        }
        row {
            Column {
                Text(
                    stringResource(R.string.settings_breaks_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                    modifier = Modifier.padding(start = inset, end = inset, top = S.HintPaddingTop),
                )
                BreakType.entries.forEach { type -> BreakToggle(type, type in state.enabledBreaks, onlyOneLeft, actions.onBreakToggled) }
                if (onlyOneLeft) {
                    Text(
                        stringResource(R.string.settings_keep_one),
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkMuted,
                        modifier = Modifier.padding(start = inset, end = inset, bottom = S.HintPaddingTop),
                    )
                }
            }
        }
    }
}

/** One break type: the whole row toggles; the switch only shows the state. */
@Composable
private fun BreakToggle(type: BreakType, isOn: Boolean, onlyOneLeft: Boolean, onToggle: (BreakType, Boolean) -> Unit) {
    SwitchRow(
        icon = type.iconRes,
        tile = type.tint,
        title = stringResource(type.labelRes),
        isOn = isOn,
        enabled = !(isOn && onlyOneLeft),
        onToggle = { onToggle(type, it) },
    )
}

/** "Sound and touch": Sounds and Haptics, both on by default and applied at once. */
@Composable
private fun FeelCard(state: SettingsUiState, actions: SettingsActions) {
    GroupedCard {
        row {
            SwitchRow(
                icon = R.drawable.ic_settings_sound,
                tile = BreakEyesTint,
                title = stringResource(R.string.settings_sounds),
                subtitle = stringResource(R.string.settings_sounds_sub),
                isOn = state.soundsOn,
                // Switching Sounds off must not chirp on the way out.
                soundOnToggle = false,
                onToggle = actions.onSoundsToggled,
            )
        }
        row {
            SwitchRow(
                icon = R.drawable.ic_settings_haptics,
                tile = BreakNeckTint,
                title = stringResource(R.string.settings_haptics),
                subtitle = stringResource(R.string.settings_haptics_sub),
                isOn = state.hapticsOn,
                onToggle = actions.onHapticsToggled,
            )
        }
    }
}

/** A row that toggles as a whole; the [Switch] only shows the state. Ticks and sounds go through [LocalFeel]. */
@Composable
private fun SwitchRow(
    @DrawableRes icon: Int,
    tile: Color,
    title: String,
    isOn: Boolean,
    onToggle: (Boolean) -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true,
    soundOnToggle: Boolean = true,
) {
    val feel = LocalFeel.current
    SettingsRow(
        icon = icon,
        tile = tile,
        title = title,
        subtitle = subtitle,
        modifier = Modifier.toggleable(
            value = isOn,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = {
                feel.toggle(it, withSound = soundOnToggle)
                onToggle(it)
            },
        ),
    ) {
        Switch(
            checked = isOn,
            onCheckedChange = null,
            enabled = enabled,
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

@Composable
private fun SectionLabel(label: Int) {
    Text(
        stringResource(label),
        style = MaterialTheme.typography.titleLarge,
        color = Ink,
        modifier = Modifier
            .padding(start = S.TextInset, end = S.TextInset, top = S.SectionTop, bottom = S.SectionBottom)
            .semantics { heading() },
    )
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
    /** As design/ui_v2/Settings.dc.html: notifications on, movement off, widget added. */
    val Mockup = SetupCardsUi(
        listOf(
            notifications.copy(status = SetupStatus.ON, action = SetupAction.None),
            movement,
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

    /** The mockup's "5-day streak · 42 breaks so far". */
    val WithHistory = Default.copy(streakDays = 5, totalBreaks = 42)

    fun named(name: String) = WithHistory.copy(pet = ActivePet(PetId.BUN, chosenName = name))
}

/** The top of the page as design/ui_v2/Settings.dc.html shows it. */
@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun SettingsMockupPreview() {
    WigglebunTheme { SettingsScreen(SettingsPreviewStates.WithHistory, SettingsPreviewStates.Mockup, SettingsActions()) }
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

@Preview(widthDp = 390, heightDp = 300)
@Composable
internal fun SettingsBadgePreview(badge: PremiumBadge = PremiumBadge.Premium, name: String? = null) {
    WigglebunTheme {
        SettingsScreen(
            name?.let { SettingsPreviewStates.named(it) } ?: SettingsPreviewStates.Default,
            SettingsPreviewStates.Mockup,
            SettingsActions(),
            badge = badge,
        )
    }
}

@Preview(widthDp = 360, heightDp = 640, fontScale = 1.3f)
@Composable
internal fun SettingsSmallLargeFontPreview() {
    WigglebunTheme { SettingsScreen(SettingsPreviewStates.Default, SettingsPreviewStates.AllOff, SettingsActions()) }
}

/** The whole page, for reviewing what's below the fold. */
@Preview(widthDp = 390, heightDp = 1700)
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
