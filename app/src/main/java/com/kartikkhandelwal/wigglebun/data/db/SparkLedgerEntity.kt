package com.kartikkhandelwal.wigglebun.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import com.kartikkhandelwal.wigglebun.domain.model.SparkReason
import kotlinx.coroutines.flow.Flow

/**
 * One movement of sparks. [key] is unique, so every reward is paid at most once however often
 * it is attempted (notification Done and in-app Done racing, repeat Done, app restarts).
 * The balance is the sum of [amount]; spending (adopting a pet) adds negative rows.
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

    @Query("SELECT COALESCE(SUM(amount), 0) FROM spark_ledger")
    suspend fun balance(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM spark_ledger WHERE `key` = :key)")
    suspend fun hasKey(key: String): Boolean

    /** Every ledger key starting with [prefix] (e.g. the pets paid for). */
    @Query("SELECT `key` FROM spark_ledger WHERE `key` LIKE :prefix || '%'")
    fun observeKeysStartingWith(prefix: String): Flow<List<String>>

    @Query("SELECT * FROM spark_ledger ORDER BY id")
    suspend fun all(): List<SparkLedgerEntity>

    /**
     * Pays for something once: the key check, the balance check and the negative row happen in one
     * transaction, so two taps (or two screens) at once can neither charge twice nor overdraw.
     */
    @Transaction
    suspend fun spend(entry: SparkLedgerEntity): SpendOutcome {
        require(entry.amount < 0) { "Spending is a negative amount" }
        if (hasKey(entry.key)) return SpendOutcome.AlreadyPaid
        val balance = balance()
        if (balance + entry.amount < 0) return SpendOutcome.Insufficient(missing = -(balance + entry.amount))
        insert(entry)
        return SpendOutcome.Spent
    }
}

/** What [SparkLedgerDao.spend] did. */
sealed interface SpendOutcome {
    data object Spent : SpendOutcome

    /** This key was paid before (a double tap): nothing charged. */
    data object AlreadyPaid : SpendOutcome

    /** Not enough sparks: nothing charged; [missing] more are needed. */
    data class Insufficient(val missing: Int) : SpendOutcome
}
