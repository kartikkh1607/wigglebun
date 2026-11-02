package com.kartikkhandelwal.wigglebun.ui.onboarding

import android.text.format.DateFormat
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.domain.sitting.WorkSchedule
import com.kartikkhandelwal.wigglebun.ui.collection.PetSheet
import com.kartikkhandelwal.wigglebun.ui.components.ChunkyButton
import com.kartikkhandelwal.wigglebun.ui.components.PillCard
import com.kartikkhandelwal.wigglebun.ui.home.formatTime
import com.kartikkhandelwal.wigglebun.ui.home.minuteOfDay
import com.kartikkhandelwal.wigglebun.ui.theme.Card
import com.kartikkhandelwal.wigglebun.ui.theme.CheckIdle
import com.kartikkhandelwal.wigglebun.ui.theme.Coral
import com.kartikkhandelwal.wigglebun.ui.theme.Cream
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.InkMuted
import com.kartikkhandelwal.wigglebun.ui.theme.Sun
import com.kartikkhandelwal.wigglebun.ui.theme.Track
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunShapes
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

private val S = Dimens.Onboarding

/** Which time the picker sheet is setting. */
enum class WorkTime(@StringRes val sheetTitle: Int) {
    START(R.string.hours_sheet_start),
    END(R.string.hours_sheet_end),
}

@Composable
fun WorkHoursScreenRoute(
    draft: WorkHoursDraft,
    onToggleDay: (DayOfWeek) -> Unit,
    onTimePicked: (WorkTime, Int) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by rememberSaveable { mutableStateOf<WorkTime?>(null) }
    WorkHoursScreen(draft, onToggleDay, onPickTime = { picking = it }, onContinue, onSkip, modifier)
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

/** One question: when do you work? (CLAUDE.md §6.2). Times are picked in a bottom sheet. */
@Composable
fun WorkHoursScreen(
    draft: WorkHoursDraft,
    onToggleDay: (DayOfWeek) -> Unit,
    onPickTime: (WorkTime) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingQuestion(
        title = stringResource(R.string.hours_title),
        body = stringResource(R.string.hours_body),
        step = 3,
        continueEnabled = draft.problem == null,
        onContinue = onContinue,
        onSkip = onSkip,
        modifier = modifier,
    ) {
        DayChips(draft.days, onToggleDay)
        TimeRow(R.string.hours_start, draft.startMinute, onClick = { onPickTime(WorkTime.START) })
        TimeRow(
            label = R.string.hours_end,
            minute = draft.endMinute,
            note = if (draft.endsNextDay) stringResource(R.string.hours_next_day) else null,
            onClick = { onPickTime(WorkTime.END) },
        )
        draft.problem?.let { problem ->
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
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Shared by the two question screens: Skip, title, body, the answer, Continue, dots. */
@Composable
internal fun OnboardingQuestion(
    title: String,
    body: String,
    step: Int,
    continueEnabled: Boolean,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier.fillMaxSize().background(Cream).systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SkipRow(color = Ink, onSkip = onSkip)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = S.NameFieldHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(S.QuestionGap),
        ) {
            Spacer(Modifier.height(S.QuestionTop))
            Text(
                title,
                style = MaterialTheme.typography.displayMedium,
                color = Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(body, style = MaterialTheme.typography.bodyLarge, color = InkMuted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(S.QuestionAnswerTop))
            content()
        }
        ChunkyButton(
            text = stringResource(R.string.onboarding_continue),
            onClick = onContinue,
            enabled = continueEnabled,
            modifier = Modifier.fillMaxWidth().padding(horizontal = S.NameButtonHorizontal, vertical = S.CtaBottomShort),
        )
        StepDots(step = step, active = Sun, inactive = CheckIdle)
        Spacer(Modifier.height(S.DotsBottomShort))
    }
}

@Composable
private fun DayChips(selected: Set<DayOfWeek>, onToggle: (DayOfWeek) -> Unit) {
    val locale: Locale = LocalConfiguration.current.locales[0]
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        DayOfWeek.entries.forEach { day ->
            val isOn = day in selected
            val fullName = day.getDisplayName(TextStyle.FULL, locale)
            Box(
                // The whole cell is the touch target; the circle inside is what shows.
                Modifier
                    .weight(1f)
                    .heightIn(min = S.DayChipTouch)
                    .toggleable(value = isOn, role = Role.Checkbox, onValueChange = { onToggle(day) })
                    .semantics { contentDescription = fullName },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .defaultMinSize(minWidth = S.DayChip, minHeight = S.DayChip)
                        .clip(CircleShape)
                        .background(if (isOn) Ink else Track),
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

@Composable
private fun TimeRow(@StringRes label: Int, minute: Int, note: String? = null, onClick: () -> Unit) {
    PillCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(label), style = MaterialTheme.typography.titleMedium, color = Ink)
                if (note != null) Text(note, style = MaterialTheme.typography.labelMedium, color = InkMuted)
            }
            Text(formatTime(minuteOfDay(minute)), style = MaterialTheme.typography.titleLarge, color = Ink)
        }
    }
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
    WigglebunTheme { WorkHoursScreen(PreviewDraft, {}, {}, {}, {}) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun WorkHoursNoDaysPreview() {
    WigglebunTheme { WorkHoursScreen(PreviewDraft.copy(days = emptySet()), {}, {}, {}, {}) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun WorkHoursOvernightPreview() {
    WigglebunTheme { WorkHoursScreen(PreviewDraft.copy(startMinute = 22 * 60, endMinute = 6 * 60), {}, {}, {}, {}) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
internal fun WorkHoursTimeSheetPreview() {
    SheetStillFrame(
        screen = { WorkHoursScreen(PreviewDraft, {}, {}, {}, {}) },
        sheet = { TimeSheetContent(WorkTime.START, PreviewDraft.startMinute, onDone = {}) },
    )
}

@Preview(widthDp = 360, heightDp = 640, fontScale = 1.3f)
@Composable
internal fun WorkHoursSmallLargeFontPreview() {
    WigglebunTheme { WorkHoursScreen(PreviewDraft, {}, {}, {}, {}) }
}
