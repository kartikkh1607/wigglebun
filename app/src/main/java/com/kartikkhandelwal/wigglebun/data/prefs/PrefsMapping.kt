package com.kartikkhandelwal.wigglebun.data.prefs

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.kartikkhandelwal.wigglebun.domain.model.MovementActivity
import com.kartikkhandelwal.wigglebun.domain.model.PetId
import com.kartikkhandelwal.wigglebun.domain.model.ResetReason
import com.kartikkhandelwal.wigglebun.domain.sitting.SitState
import com.kartikkhandelwal.wigglebun.domain.sitting.SittingSettings
import com.kartikkhandelwal.wigglebun.domain.sitting.WorkDayHistory
import com.kartikkhandelwal.wigglebun.domain.sitting.WorkDaysChange
import com.kartikkhandelwal.wigglebun.domain.sitting.WorkSchedule
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

/** All keys in the single app DataStore (see DataStoreModule). */
internal object PrefsKeys {
    val WorkDays = stringSetPreferencesKey("work_days")
    val WorkStartMinute = intPreferencesKey("work_start_minute")
    val WorkEndMinute = intPreferencesKey("work_end_minute")
    val IntervalMinutes = intPreferencesKey("interval_minutes")
    val PetName = stringPreferencesKey("pet_name")
    /** [PetId.key] of the pet that is "with you" (M8). Missing = the starter pet. */
    val ActivePet = stringPreferencesKey("active_pet")
    /** [AccessoryId.key]s a pet wears (one per slot), one set per pet: "worn_bun", "worn_pip"… */
    fun Worn(pet: PetId) = stringSetPreferencesKey("worn_${pet.key}")
    /** "2026-10-27=MONDAY,TUESDAY;…" (see [writeWorkDayHistory]). */
    val WorkDayHistory = stringPreferencesKey("work_day_history")
    /** Missing = not decided yet; false = onboarding still to do; true = done or never needed. */
    val OnboardingDone = booleanPreferencesKey("onboarding_done")

    val SitStartedAt = longPreferencesKey("sit_started_at")
    val SnoozeCount = intPreferencesKey("snooze_count")
    val SnoozedUntil = longPreferencesKey("snoozed_until")
    val LastNotifiedDue = longPreferencesKey("last_notified_due")
    val CelebrateUntil = longPreferencesKey("celebrate_until")
    val LastBreakAt = longPreferencesKey("last_break_at")
    val LastResetReason = stringPreferencesKey("last_reset_reason")
    val MovingSince = longPreferencesKey("moving_since")
    val ActiveMoving = stringSetPreferencesKey("active_moving")
}

internal fun Preferences.toSittingSettings(): SittingSettings {
    val default = WorkSchedule.Default
    val days = this[PrefsKeys.WorkDays]
        ?.mapNotNull { name -> DayOfWeek.entries.firstOrNull { it.name == name } }
        ?.toSet()
        ?: default.days
    val start = this[PrefsKeys.WorkStartMinute]?.takeIf { it in 0 until WorkSchedule.MINUTES_PER_DAY }
    val end = this[PrefsKeys.WorkEndMinute]?.takeIf { it in 0..WorkSchedule.MINUTES_PER_DAY }
    return SittingSettings(
        schedule = WorkSchedule(days, start ?: default.startMinute, end ?: default.endMinute),
        intervalMinutes = (this[PrefsKeys.IntervalMinutes] ?: SittingSettings.DEFAULT_INTERVAL_MINUTES)
            .coerceIn(SittingSettings.MIN_INTERVAL_MINUTES, SittingSettings.MAX_INTERVAL_MINUTES),
    )
}

internal fun MutablePreferences.writeSchedule(schedule: WorkSchedule) {
    this[PrefsKeys.WorkDays] = schedule.days.map { it.name }.toSet()
    this[PrefsKeys.WorkStartMinute] = schedule.startMinute
    this[PrefsKeys.WorkEndMinute] = schedule.endMinute
}

/** Bad entries are skipped; nothing stored means "the current work days, since forever". */
internal fun Preferences.toWorkDayHistory(): WorkDayHistory {
    val changes = this[PrefsKeys.WorkDayHistory].orEmpty()
        .split(HISTORY_SEPARATOR)
        .mapNotNull { entry ->
            val from = entry.substringBefore(HISTORY_ASSIGN, missingDelimiterValue = "")
            val date = runCatching { LocalDate.parse(from) }.getOrNull() ?: return@mapNotNull null
            val days = entry.substringAfter(HISTORY_ASSIGN).split(HISTORY_DAY_SEPARATOR)
                .mapNotNull { enumOrNull<DayOfWeek>(it) }
                .toSet()
            WorkDaysChange(date, days)
        }
        .sortedBy { it.from }
    return WorkDayHistory(changes, toSittingSettings().schedule.days)
}

internal fun MutablePreferences.writeWorkDayHistory(history: WorkDayHistory) {
    if (history.changes.isEmpty()) {
        remove(PrefsKeys.WorkDayHistory)
        return
    }
    this[PrefsKeys.WorkDayHistory] = history.changes.joinToString(HISTORY_SEPARATOR) { change ->
        change.from.toString() + HISTORY_ASSIGN + change.days.sorted().joinToString(HISTORY_DAY_SEPARATOR) { it.name }
    }
}

private const val HISTORY_SEPARATOR = ";"
private const val HISTORY_ASSIGN = "="
private const val HISTORY_DAY_SEPARATOR = ","

internal fun Preferences.toSitState(): SitState = SitState(
    sitStartedAt = instant(PrefsKeys.SitStartedAt),
    snoozeCount = this[PrefsKeys.SnoozeCount] ?: 0,
    snoozedUntil = instant(PrefsKeys.SnoozedUntil),
    lastNotifiedDue = instant(PrefsKeys.LastNotifiedDue),
    celebrateUntil = instant(PrefsKeys.CelebrateUntil),
    lastBreakAt = instant(PrefsKeys.LastBreakAt),
    lastResetReason = enumOrNull<ResetReason>(this[PrefsKeys.LastResetReason]),
    movingSince = instant(PrefsKeys.MovingSince),
    activeMoving = this[PrefsKeys.ActiveMoving].orEmpty().mapNotNull { enumOrNull<MovementActivity>(it) }.toSet(),
)

internal fun MutablePreferences.writeSitState(state: SitState) {
    putInstant(PrefsKeys.SitStartedAt, state.sitStartedAt)
    this[PrefsKeys.SnoozeCount] = state.snoozeCount
    putInstant(PrefsKeys.SnoozedUntil, state.snoozedUntil)
    putInstant(PrefsKeys.LastNotifiedDue, state.lastNotifiedDue)
    putInstant(PrefsKeys.CelebrateUntil, state.celebrateUntil)
    putInstant(PrefsKeys.LastBreakAt, state.lastBreakAt)
    val reason = state.lastResetReason
    if (reason == null) remove(PrefsKeys.LastResetReason) else this[PrefsKeys.LastResetReason] = reason.name
    putInstant(PrefsKeys.MovingSince, state.movingSince)
    this[PrefsKeys.ActiveMoving] = state.activeMoving.map { it.name }.toSet()
}

private inline fun <reified E : Enum<E>> enumOrNull(name: String?): E? =
    name?.let { n -> enumValues<E>().firstOrNull { it.name == n } }

private fun Preferences.instant(key: Preferences.Key<Long>): Instant? = this[key]?.let(Instant::ofEpochMilli)

private fun MutablePreferences.putInstant(key: Preferences.Key<Long>, value: Instant?) {
    if (value == null) remove(key) else this[key] = value.toEpochMilli()
}
