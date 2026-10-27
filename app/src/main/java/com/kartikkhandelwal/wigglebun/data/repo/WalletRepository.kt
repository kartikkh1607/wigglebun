package com.kartikkhandelwal.wigglebun.data.repo

import com.kartikkhandelwal.wigglebun.data.db.SparkLedgerDao
import com.kartikkhandelwal.wigglebun.data.db.SparkLedgerEntity
import com.kartikkhandelwal.wigglebun.domain.model.SparkReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** The sparks balance (CLAUDE.md §5.4), backed by the spark_ledger table. Spending arrives in M8. */
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
}
