package com.kartikkhandelwal.wigglebun.data.repo

import com.kartikkhandelwal.wigglebun.data.db.BreakLogDao
import com.kartikkhandelwal.wigglebun.data.db.BreakLogEntity
import com.kartikkhandelwal.wigglebun.data.db.DoneBreakRow
import com.kartikkhandelwal.wigglebun.domain.model.BreakOutcome
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.DoneBreak
import com.kartikkhandelwal.wigglebun.domain.sitting.BreakDays
import com.kartikkhandelwal.wigglebun.domain.sitting.DaySpan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Instant
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

    suspend fun doneBreaks(): List<DoneBreak> = dao.doneBreaks().map { it.toDoneBreak() }

    fun observeDoneBreaks(): Flow<List<DoneBreak>> = dao.observeDoneBreaks().map { rows -> rows.map { it.toDoneBreak() } }

    private fun DoneBreakRow.toDoneBreak() = DoneBreak(type, Instant.ofEpochMilli(completedAt))

    /**
     * Everything logged during one break day (see [BreakDays.span]): midnight to midnight, or
     * shift start to shift start when work hours cross midnight. Nothing for a skipped day.
     */
    fun observeDay(span: DaySpan?): Flow<List<BreakLogEntity>> =
        if (span == null) flowOf(emptyList()) else dao.observeBetween(span.start.toEpochMilli(), span.end.toEpochMilli())
}
