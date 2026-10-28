package com.kartikkhandelwal.wigglebun.data.repo

import com.kartikkhandelwal.wigglebun.data.db.SparkLedgerDao
import com.kartikkhandelwal.wigglebun.data.db.SparkLedgerEntity
import com.kartikkhandelwal.wigglebun.data.db.SpendOutcome
import com.kartikkhandelwal.wigglebun.domain.model.SparkReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** The sparks balance (CLAUDE.md §5.4), backed by the spark_ledger table. */
@Singleton
class WalletRepository @Inject constructor(
    private val dao: SparkLedgerDao,
) {
    val balance: Flow<Int> = dao.observeBalance().distinctUntilChanged()

    /**
     * Pays [amount] under [key] unless that key was already paid.
     * @return the sparks actually added: [amount], or 0 for a duplicate.
     */
    suspend fun award(key: String, amount: Int, reason: SparkReason, at: Instant): Int {
        val id = dao.insert(SparkLedgerEntity(amount = amount, reason = reason, key = key, createdAt = at.toEpochMilli()))
        return if (id == -1L) 0 else amount
    }

    /** Charges [amount] (> 0) once under [key], only if the balance covers it (one transaction). */
    suspend fun spend(key: String, amount: Int, reason: SparkReason, at: Instant): SpendOutcome {
        require(amount > 0) { "amount $amount" }
        return dao.spend(SparkLedgerEntity(amount = -amount, reason = reason, key = key, createdAt = at.toEpochMilli()))
    }

    /** Every paid key starting with [prefix] (the unlock rows of adopted pets). */
    fun keysStartingWith(prefix: String): Flow<List<String>> = dao.observeKeysStartingWith(prefix)
}
