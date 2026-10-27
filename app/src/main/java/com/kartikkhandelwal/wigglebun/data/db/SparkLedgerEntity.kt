package com.kartikkhandelwal.wigglebun.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.kartikkhandelwal.wigglebun.domain.model.SparkReason
import kotlinx.coroutines.flow.Flow

/**
 * One movement of sparks. [key] is unique, so every reward is paid at most once however often
 * it is attempted (notification Done and in-app Done racing, repeat Done, app restarts).
 * The balance is the sum of [amount]; spending (M8) will add negative rows.
 */
@Entity(tableName = "spark_ledger", indices = [Index(value = ["key"], unique = true)])
data class SparkLedgerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Int,
    val reason: SparkReason,
    val key: String,
    /** Epoch millis (UTC). */
    val createdAt: Long,
)

@Dao
interface SparkLedgerDao {
    /** Returns the new row id, or -1 if a row with the same key already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: SparkLedgerEntity): Long

    @Query("SELECT COALESCE(SUM(amount), 0) FROM spark_ledger")
    fun observeBalance(): Flow<Int>

    @Query("SELECT * FROM spark_ledger ORDER BY id")
    suspend fun all(): List<SparkLedgerEntity>
}
