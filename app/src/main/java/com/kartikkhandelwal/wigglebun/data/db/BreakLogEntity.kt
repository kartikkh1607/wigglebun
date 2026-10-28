package com.kartikkhandelwal.wigglebun.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import com.kartikkhandelwal.wigglebun.domain.model.BreakOutcome
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import kotlinx.coroutines.flow.Flow

/** One completed or skipped break. Enums are stored by name (Room's built-in enum support). */
@Entity(tableName = "break_log", indices = [Index("completedAt")])
data class BreakLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: BreakType,
    /** Epoch millis (UTC). */
    val completedAt: Long,
    /**
     * The type's listed reward; 0 for skipped. What was actually paid (repeats pay nothing)
     * lives in spark_ledger.
     */
    val reward: Int,
    val outcome: BreakOutcome,
)

@Dao
interface BreakLogDao {
    @Insert
    suspend fun insert(entry: BreakLogEntity): Long

    @Query("SELECT * FROM break_log WHERE completedAt >= :fromEpochMillis ORDER BY completedAt DESC")
    fun observeSince(fromEpochMillis: Long): Flow<List<BreakLogEntity>>

    @Query("SELECT * FROM break_log WHERE completedAt >= :fromEpochMillis AND completedAt < :untilEpochMillis ORDER BY completedAt")
    fun observeBetween(fromEpochMillis: Long, untilEpochMillis: Long): Flow<List<BreakLogEntity>>

    @Query("SELECT COUNT(*) FROM break_log WHERE outcome = 'DONE' AND completedAt >= :fromEpochMillis")
    suspend fun countDoneSince(fromEpochMillis: Long): Int

    /** Every completed break (type + time), for the streak. A few thousand rows per year at most. */
    @Query("SELECT type, completedAt FROM break_log WHERE outcome = 'DONE' ORDER BY completedAt")
    suspend fun doneBreaks(): List<DoneBreakRow>

    /** [doneBreaks], kept up to date (Pet profile: streak, total and chart). */
    @Query("SELECT type, completedAt FROM break_log WHERE outcome = 'DONE' ORDER BY completedAt")
    fun observeDoneBreaks(): Flow<List<DoneBreakRow>>
}

data class DoneBreakRow(val type: BreakType, val completedAt: Long)
