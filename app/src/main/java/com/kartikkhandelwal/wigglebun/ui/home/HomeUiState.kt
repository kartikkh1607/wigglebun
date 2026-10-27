package com.kartikkhandelwal.wigglebun.ui.home

import com.kartikkhandelwal.wigglebun.data.db.BreakLogEntity
import com.kartikkhandelwal.wigglebun.domain.model.BreakOutcome
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.domain.sitting.SitStatus
import com.kartikkhandelwal.wigglebun.domain.sitting.SittingSettings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Mood chip on the timer pill (copy and colours from Main.dc.html). */
enum class MoodChip { GREAT, TIRED, STIFF, WALK }

sealed interface PillState {
    /** In work hours: the timer. [progress] is the fraction of the interval, capped at 1. */
    data class Sitting(val minutes: Int, val progress: Float, val chip: MoodChip) : PillState

    /** Outside work hours: no timer, no guilt, just when work hours start next. */
    data class Asleep(val nextStart: LocalDateTime?) : PillState
}

data class BreakCardUi(
    val type: BreakType,
    val doneToday: Boolean,
    val skippedToday: Boolean,
)

/** The focused card overlay (FocusCard.dc.html). */
data class FocusUi(
    val type: BreakType,
    val doneToday: Boolean,
    /** Snooze is only offered once the reminder is due (the engine refuses earlier). */
    val canSnooze: Boolean,
)

data class HomeUiState(
    /** Null until the user names the pet (M9); screens fall back to "Bun". */
    val petName: String?,
    val energy: Float,
    val mood: Mood,
    val pill: PillState,
    val showBreakCta: Boolean,
    val sparks: Int,
    val breaks: List<BreakCardUi>,
    val intervalMinutes: Int,
    val workStartMinute: Int,
    val workEndMinute: Int,
    /** The sit-timer reminder is due right now (decides what Skip does). */
    val reminderDue: Boolean,
    val today: LocalDate,
    val focus: FocusUi?,
) {
    val doneCount: Int get() = breaks.count { it.doneToday }
    val totalCount: Int get() = breaks.size
}

/** True once this session's reminder time has passed (in work hours). */
fun SitStatus.isReminderDue(): Boolean {
    val due = reminderDue ?: return false
    return isWorkTime && !now.isBefore(due)
}

/** Pure mapping from engine/settings/BreakLog to the screen state (unit-tested). */
fun toHomeUiState(
    status: SitStatus,
    settings: SittingSettings,
    petName: String?,
    sparks: Int,
    logsToday: List<BreakLogEntity>,
    focused: BreakType?,
    zone: ZoneId,
    enabledTypes: List<BreakType> = BreakType.entries,
): HomeUiState {
    val done = logsToday.filter { it.outcome == BreakOutcome.DONE }.map { it.type }.toSet()
    val skipped = logsToday.filter { it.outcome == BreakOutcome.SKIPPED }.map { it.type }.toSet()
    val reminderDue = status.isReminderDue()

    val pill = if (!status.isWorkTime) {
        PillState.Asleep(status.nextSessionStart?.atZone(zone)?.toLocalDateTime())
    } else {
        PillState.Sitting(
            minutes = status.sitMinutes,
            progress = status.sitFraction.coerceIn(0f, 1f),
            chip = when {
                status.isOnWalk -> MoodChip.WALK
                status.mood == Mood.STIFF -> MoodChip.STIFF
                status.mood == Mood.TIRED -> MoodChip.TIRED
                else -> MoodChip.GREAT // HAPPY, CELEBRATE, STRETCH
            },
        )
    }

    return HomeUiState(
        petName = petName,
        energy = status.energy,
        mood = status.mood,
        pill = pill,
        showBreakCta = status.isWorkTime && status.mood == Mood.STIFF && !status.isOnWalk,
        sparks = sparks,
        breaks = enabledTypes.map { BreakCardUi(it, doneToday = it in done, skippedToday = it in skipped && it !in done) },
        intervalMinutes = settings.interval,
        workStartMinute = settings.schedule.startMinute,
        workEndMinute = settings.schedule.endMinute,
        reminderDue = reminderDue,
        today = status.now.atZone(zone).toLocalDate(),
        focus = focused?.let {
            FocusUi(it, doneToday = it in done, canSnooze = reminderDue && status.snoozesLeft > 0)
        },
    )
}
