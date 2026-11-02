package com.kartikkhandelwal.wigglebun.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.kartikkhandelwal.wigglebun.data.time.AppClock
import com.kartikkhandelwal.wigglebun.domain.model.AccessoryId
import com.kartikkhandelwal.wigglebun.domain.model.Outfit
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.PetId
import com.kartikkhandelwal.wigglebun.domain.rewards.DailyGoalHistory
import com.kartikkhandelwal.wigglebun.domain.rewards.RewardRules
import com.kartikkhandelwal.wigglebun.domain.sitting.SittingSettings
import com.kartikkhandelwal.wigglebun.domain.sitting.WorkDayHistory
import com.kartikkhandelwal.wigglebun.domain.sitting.WorkSchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Work hours, break interval, pet name, the active pet and what each pet wears (CLAUDE.md §5.1). Backed up. */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val clock: AppClock,
) {
    private val prefs: Flow<Preferences> = dataStore.data.catch { e ->
        // A corrupt/unreadable file must not crash the app: fall back to defaults.
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val settings: Flow<SittingSettings> = prefs.map { it.toSittingSettings() }.distinctUntilChanged()

    /** Null until the user names the pet in onboarding (M9). */
    val petName: Flow<String?> = prefs.map { it[PrefsKeys.PetName] }.distinctUntilChanged()

    /** The stored active pet; PetRepository checks it is owned. Unknown or missing = the starter pet. */
    val activePetId: Flow<PetId> =
        prefs.map { PetId.fromKey(it[PrefsKeys.ActivePet]) ?: PetId.Starter }.distinctUntilChanged()

    /** What each pet wears, as stored (PetRepository keeps only owned accessories). */
    val outfits: Flow<Map<PetId, Outfit>> = prefs.map { p ->
        PetId.entries.associateWith { pet ->
            Outfit(p[PrefsKeys.Worn(pet)].orEmpty().mapNotNullTo(mutableSetOf(), AccessoryId::fromKey))
        }
    }.distinctUntilChanged()

    /** Work days over time, so the streak judges past days by the days in force back then. */
    val workDayHistory: Flow<WorkDayHistory> = prefs.map { it.toWorkDayHistory() }.distinctUntilChanged()

    /** Null until [decideOnboarding] has run (CLAUDE.md §6.2); the app shows nothing until then. */
    val onboardingDone: Flow<Boolean?> = prefs.map { it[PrefsKeys.OnboardingDone] }.distinctUntilChanged()

    /** The break types switched on, in display order; never empty (CLAUDE.md §5.3). */
    val enabledBreaks: Flow<List<BreakType>> = prefs.map { it.toEnabledBreaks() }.distinctUntilChanged()

    /** The streak's goal over time, so past days keep the goal they had (see [DailyGoalHistory]). */
    val dailyGoalHistory: Flow<DailyGoalHistory> = prefs.map { it.toDailyGoalHistory() }.distinctUntilChanged()

    suspend fun current(): SittingSettings = settings.first()

    /**
     * Run on every app start, before the first start saves the timer. A saved timer on the very
     * first decision means this install predates onboarding, so it never sees it. The answer is
     * stored, so a new install killed halfway still gets onboarding (its timer is saved by then).
     */
    suspend fun decideOnboarding() {
        dataStore.edit {
            if (PrefsKeys.OnboardingDone !in it) it[PrefsKeys.OnboardingDone] = PrefsKeys.SitStartedAt in it
        }
    }

    suspend fun finishOnboarding() {
        dataStore.edit { it[PrefsKeys.OnboardingDone] = true }
    }

    /** Changing the work days takes effect today and is recorded in [workDayHistory]. */
    suspend fun setSchedule(schedule: WorkSchedule) = updateSchedule { schedule }

    /** [setSchedule] from the stored schedule, in one atomic write, so quick changes can't overwrite each other. */
    suspend fun updateSchedule(transform: (WorkSchedule) -> WorkSchedule) {
        val today = today()
        dataStore.edit {
            val schedule = transform(it.toSittingSettings().schedule)
            val history = it.toWorkDayHistory().withChange(schedule.days, today)
            it.writeSchedule(schedule)
            it.writeWorkDayHistory(history)
        }
    }

    /**
     * Switches a break type on or off. Refuses (returns false) to switch off the last one. The
     * streak's goal follows the number switched on, from today (see [DailyGoalHistory]).
     */
    suspend fun setBreakEnabled(type: BreakType, enabled: Boolean): Boolean {
        val today = today()
        var changed = false
        dataStore.edit {
            val current = it.toEnabledBreaks()
            val next = if (enabled) current + type else current - type
            if (next.isEmpty()) return@edit
            changed = true
            it[PrefsKeys.EnabledBreaks] = next.map { break_ -> break_.name }.toSet()
            it.writeDailyGoalHistory(it.toDailyGoalHistory().withChange(RewardRules.dailyGoal(next.toSet().size), today))
        }
        return changed
    }

    private fun today() = clock.instant().atZone(clock.zone).toLocalDate()

    suspend fun setOutfit(pet: PetId, outfit: Outfit) {
        dataStore.edit { it[PrefsKeys.Worn(pet)] = outfit.items.mapTo(mutableSetOf()) { item -> item.key } }
    }

    /** Null goes back to the pet's own name. */
    suspend fun setPetName(name: String?) {
        dataStore.edit { if (name == null) it.remove(PrefsKeys.PetName) else it[PrefsKeys.PetName] = name }
    }

    suspend fun setActivePet(pet: PetId) {
        dataStore.edit { it[PrefsKeys.ActivePet] = pet.key }
    }

    suspend fun setInterval(minutes: Int) {
        dataStore.edit {
            it[PrefsKeys.IntervalMinutes] =
                minutes.coerceIn(SittingSettings.MIN_INTERVAL_MINUTES, SittingSettings.MAX_INTERVAL_MINUTES)
        }
    }
}
