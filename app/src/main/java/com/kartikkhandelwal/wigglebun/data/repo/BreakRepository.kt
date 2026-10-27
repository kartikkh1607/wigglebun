package com.kartikkhandelwal.wigglebun.data.repo

import com.kartikkhandelwal.wigglebun.data.db.BreakLogDao
import com.kartikkhandelwal.wigglebun.data.db.BreakLogEntity
import com.kartikkhandelwal.wigglebun.domain.model.BreakOutcome
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BreakRepository @Inject constructor(
    private val dao: BreakLogDao,
) {
    suspend fun logDone(type: BreakType, at: Instant) {
        dao.insert(BreakLogEntity(type = type, completedAt = at.toEpochMilli(), reward = type.defaultReward, outcome = BreakOutcome.DONE))
    }

    suspend fun logSkipped(type: BreakType, at: Instant) {
        dao.insert(BreakLogEntity(type = type, completedAt = at.toEpochMilli(), reward = 0, outcome = BreakOutcome.SKIPPED))
    }

    fun observeSince(from: Instant): Flow<List<BreakLogEntity>> = dao.observeSince(from.toEpochMilli())

    /** Everything logged on [date] in [zone] (local midnight to midnight, DST-safe). */
    fun observeDay(date: LocalDate, zone: ZoneId): Flow<List<BreakLogEntity>> = dao.observeBetween(
        date.atStartOfDay(zone).toInstant().toEpochMilli(),
        date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
    )
}
