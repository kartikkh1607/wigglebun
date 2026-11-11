package com.kartikkhandelwal.wigglebun.ui.onboarding

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.sizeIn
import com.kartikkhandelwal.wigglebun.feedback.LocalFeel
import android.text.format.DateFormat
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.domain.sitting.WorkSchedule
import com.kartikkhandelwal.wigglebun.ui.collection.PetSheet
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButton
import com.kartikkhandelwal.wigglebun.ui.components.SettingsRow
import com.kartikkhandelwal.wigglebun.ui.home.formatTime
import com.kartikkhandelwal.wigglebun.ui.home.minuteOfDay
import com.kartikkhandelwal.wigglebun.ui.theme.BreakEyesTint
import com.kartikkhandelwal.wigglebun.ui.theme.BreakStandTint
import com.kartikkhandelwal.wigglebun.ui.theme.Card
import com.kartikkhandelwal.wigglebun.ui.theme.CheckIdle
import com.kartikkhandelwal.wigglebun.ui.theme.Coral
import com.kartikkhandelwal.wigglebun.ui.theme.Cream
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.OnboardingCard
import com.kartikkhandelwal.wigglebun.ui.theme.OnboardingCardDivider
import com.kartikkhandelwal.wigglebun.ui.theme.InkMuted
import com.kartikkhandelwal.wigglebun.ui.theme.Sun
import com.kartikkhandelwal.wigglebun.ui.theme.Track
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunShapes
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

private val S = Dimens.Onboarding
private val P = Dimens.OnboardingPanel

/** Which time the picker sheet is setting. */
enum class WorkTime(@StringRes val sheetTitle: Int) {
    START(R.string.hours_sheet_start),
    END(R.string.hours_sheet_end),
}

@Composable
fun WorkHoursScreenRoute(
    draft: WorkHoursDraft,
    problem: WorkHoursProblem?,
    petName: String,
    onToggleDay: (DayOfWeek) -> Unit,
    onTimePicked: (WorkTime, Int) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by rememberSaveable { mutableStateOf<WorkTime?>(null) }
    WorkHoursScreen(draft, problem, petName, onToggleDay, onPickTime = { picking = it }, onBack, onContinue, onSkip, modifier)
    picking?.let { time ->
        PetSheet(onDismiss = { picking = null }) {
            TimeSheetContent(
                time = time,
                minute = if (time == WorkTime.START) draft.startMinute else draft.endMinute,
                onDone = {
                    onTimePicked(time, it)
                    picking = null
                },
            )
        }
    }
}

/**
 * One question: when do you work? (CLAUDE.md §6.2, design/ui_v2/Hours.dc.html). Times are picked in
 * a bottom sheet. No work days turns Continue off and says why at once; [problem] is what the last
 * Continue found (the same start and end), which is never flagged while editing.
 */
@Composable
fun WorkHoursScreen(
    draft: WorkHoursDraft,
    problem: WorkHoursProblem?,
    petName: String,
    onToggleDay: (DayOfWeek) -> Unit,
    onPickTime: (WorkTime) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingPanelScreen(
        step = 2,
        title = stringResource(R.string.hours_title),
        body = stringResource(R.string.hours_body),
        panelHeight = P.HoursPanelHeight,
        onBack = onBack,
        onSkip = onSkip,
        onContinue = onContinue,
        modifier = modifier,
        continueEnabled = draft.canSave,
        error = draft.shownProblem(problem)?.let {
            stringResource(
                when (it) {
                    WorkHoursProblem.NO_DAYS -> R.string.hours_no_days
                    WorkHoursProblem.SAME_TIME -> R.string.hours_same_time
                },
            )
        },
        scene = { scale -> HoursScene(draft, petName, scale) },
    ) {
        DayChips(draft.days, onToggleDay)
        StartEndRows(draft, onPickTime)
    }
}

@Composable
internal fun DayChips(
    selected: Set<DayOfWeek>,
    onToggle: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier,
    /** An unticked day's circle: Track on onboarding's panel, the grouped card's divider lilac in Settings. */
    offColor: Color = Track,
) {
    val locale: Locale = LocalConfiguration.current.locales[0]
    val feel = LocalFeel.current
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        DayOfWeek.entries.forEach { day ->
            val isOn = day in selected
            val fullName = day.getDisplayName(TextStyle.FULL, locale)
            Box(
                // The whole cell is the touch target; the circle inside is what shows.
                Modifier
                    .weight(1f)
                    .heightIn(min = S.DayChipTouch)
                    .toggleable(value = isOn, role = Role.Checkbox, onValueChange = { feel.segment(); onToggle(day) })
                    .semantics { contentDescription = fullName },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    // Never wider than its cell less a gap: on a 360 dp phone seven 40 dp circles
                    // would all but touch, so there they shrink a little instead. The letter grows
                    // with the font; the circle doesn't.
                    Modifier
                        .padding(horizontal = S.DayChipGap / 2)
                        .sizeIn(maxWidth = S.DayChip, maxHeight = S.DayChip)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(if (isOn) Ink else offColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        day.getDisplayName(TextStyle.NARROW, locale),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isOn) Card else InkMuted,
                    )
                }
            }
        }
    }
}

/** Starts and Ends on one soft card (Hours.dc.html); a tap opens the clock sheet. */
@Composable
private fun StartEndRows(draft: WorkHoursDraft, onPickTime: (WorkTime) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(P.RowsRadius))
            .background(OnboardingCard),
    ) {
        SettingsRow(
            icon = R.drawable.ic_settings_sunrise,
            tile = BreakStandTint,
            title = stringResource(R.string.hours_start),
            onClick = { onPickTime(WorkTime.START) },
        ) { TimeValue(draft.startMinute) }
        HorizontalDivider(thickness = P.RowsDividerThickness, color = OnboardingCardDivider)
        SettingsRow(
            icon = R.drawable.ic_settings_moon,
            tile = BreakEyesTint,
            title = stringResource(R.string.hours_end),
            subtitle = if (draft.endsNextDay) stringResource(R.string.hours_next_day) else null,
            onClick = { onPickTime(WorkTime.END) },
        ) { TimeValue(draft.endMinute) }
    }
}

@Composable
private fun TimeValue(minute: Int) {
    Text(formatTime(minuteOfDay(minute)), style = MaterialTheme.typography.titleLarge, color = Ink)
}

/** A clock in a bottom sheet (CLAUDE.md §6.2). Nothing changes until Done. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimeSheetContent(time: WorkTime, minute: Int, onDone: (Int) -> Unit, modifier: Modifier = Modifier) {
    val hourOfDay = minuteOfDay(minute)
    val state = rememberTimePickerState(
        initialHour = hourOfDay.hour,
        initialMinute = hourOfDay.minute,
        is24Hour = DateFormat.is24HourFormat(LocalContext.current),
    )
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = S.SheetHorizontal, end = S.SheetHorizontal, bottom = S.SheetBottom),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(S.SheetGap),
    ) {
        Text(stringResource(time.sheetTitle), style = MaterialTheme.typography.titleLarge, color = Ink, textAlign = TextAlign.Center)
        TimePicker(
            state = state,
            colors = TimePickerDefaults.colors(
                clockDialColor = Track,
                clockDialSelectedContentColor = Ink,
                clockDialUnselectedContentColor = Ink,
                selectorColor = Sun,
                containerColor = Card,
                periodSelectorBorderColor = CheckIdle,
                periodSelectorSelectedContainerColor = Sun,
                periodSelectorUnselectedContainerColor = Card,
                periodSelectorSelectedContentColor = Ink,
                periodSelectorUnselectedContentColor = InkMuted,
                timeSelectorSelectedContainerColor = Sun,
                timeSelectorUnselectedContainerColor = Track,
                timeSelectorSelectedContentColor = Ink,
                timeSelectorUnselectedContentColor = Ink,
            ),
        )
        ChunkyButton(
            text = stringResource(R.string.sheet_done),
            onClick = { onDone(state.hour * MINUTES_PER_HOUR + state.minute) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private const val MINUTES_PER_HOUR = 60

/**
 * A screen with its sheet open, as a still frame for previews and screenshot tests (the real
 * ModalBottomSheet animates in from its own window).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SheetStillFrame(screen: @Composable () -> Unit, sheet: @Composable () -> Unit) {
    WigglebunTheme {
        Box(Modifier.fillMaxSize()) {
            screen()
            Box(Modifier.fillMaxSize().background(BottomSheetDefaults.ScrimColor))
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clip(WigglebunShapes.SheetTop)
                    .background(Card),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BottomSheetDefaults.DragHandle(color = CheckIdle)
                sheet()
            }
        }
    }
}

private val PreviewDraft = WorkHoursDraft.of(WorkSchedule.Default)

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun WorkHoursPreview() {
    WigglebunTheme { WorkHoursScreen(PreviewDraft, null, "Bun", {}, {}, {}, {}, {}) }
}

/** Every day unticked: Continue is off straight away, and the reason shows above it. */
@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun WorkHoursNoDaysPreview() {
    WigglebunTheme { WorkHoursScreen(PreviewDraft.copy(days = emptySet()), null, "Bun", {}, {}, {}, {}, {}) }
}

/** Continue was tapped with the same start and end: the step stays, and the reason shows above Continue. */
@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun WorkHoursSameTimePreview() {
    WigglebunTheme { WorkHoursScreen(PreviewDraft.copy(startMinute = 18 * 60), WorkHoursProblem.SAME_TIME, "Bun", {}, {}, {}, {}, {}) }
}

/** Halfway from 9–18 to 18–02: start and end match for now, and nothing complains until Continue. */
@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun WorkHoursSameTimeEditingPreview() {
    WigglebunTheme { WorkHoursScreen(PreviewDraft.copy(startMinute = 18 * 60), null, "Bun", {}, {}, {}, {}, {}) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun WorkHoursOvernightPreview() {
    WigglebunTheme { WorkHoursScreen(PreviewDraft.copy(startMinute = 22 * 60, endMinute = 6 * 60), null, "Bun", {}, {}, {}, {}, {}) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun WorkHoursTimeSheetPreview() {
    SheetStillFrame(
        screen = { WorkHoursScreen(PreviewDraft, null, "Bun", {}, {}, {}, {}, {}) },
        sheet = { TimeSheetContent(WorkTime.START, PreviewDraft.startMinute, onDone = {}) },
    )
}

/** An hour's work: start and end labels would touch, so one is left out. */
@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun WorkHoursShortPreview() {
    WigglebunTheme { WorkHoursScreen(PreviewDraft.copy(startMinute = 9 * 60, endMinute = 10 * 60), null, "Bun", {}, {}, {}, {}, {}) }
}

@Preview(widthDp = 360, heightDp = 640)
@Composable
internal fun WorkHoursSmallPhonePreview() {
    WigglebunTheme { WorkHoursScreen(PreviewDraft, null, "WWWWWWWWWWWW", {}, {}, {}, {}, {}) }
}

@Preview(widthDp = 360, heightDp = 640, fontScale = 2f)
@Composable
internal fun WorkHoursSmallHugeFontPreview() {
    WigglebunTheme { WorkHoursScreen(PreviewDraft, null, "Bun", {}, {}, {}, {}, {}) }
}

@Preview(widthDp = 360, heightDp = 640, fontScale = 1.3f)
@Composable
internal fun WorkHoursSmallLargeFontPreview() {
    WigglebunTheme { WorkHoursScreen(PreviewDraft, null, "Bun", {}, {}, {}, {}, {}) }
}
