package com.kartikkhandelwal.wigglebun.data.repo

import com.kartikkhandelwal.wigglebun.data.time.AppClock
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.sitting.SnoozeResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What the Home focused card's Skip and Snooze do (kept out of the ViewModel so it's testable).
 * Done opens the break session, which completes through [RecordBreakDone].
 */
@Singleton
class BreakActions @Inject constructor(
    private val sitRepository: SitRepository,
    private val breakRepository: BreakRepository,
    private val clock: AppClock,
) {
    /**
     * Skip from the focused card. When the reminder is due it is the same as the notification's
     * Skip (timer reset, no reward). Before that, only the break is marked skipped for today: the
     * sit timer is NOT reset, otherwise Skip would be a way to make the pet happy without moving.
     */
    suspend fun skip(type: BreakType, reminderDue: Boolean) {
        if (reminderDue) sitRepository.skip(type) else breakRepository.logSkipped(type, clock.instant())
    }

    suspend fun snooze(): SnoozeResult = sitRepository.snooze()
}
