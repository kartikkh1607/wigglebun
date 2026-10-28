package com.kartikkhandelwal.wigglebun.data.repo

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kartikkhandelwal.wigglebun.data.db.AppDatabase
import com.kartikkhandelwal.wigglebun.data.db.SpendOutcome
import com.kartikkhandelwal.wigglebun.domain.model.SparkReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class)
class WalletRepositoryTest {

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java).build()
    private val wallet = WalletRepository(db.sparkLedgerDao())
    private val at = Instant.parse("2026-10-27T05:00:00Z")

    @After
    fun tearDown() = db.close()

    @Test
    fun newWalletStartsAtZero() = runBlocking {
        assertEquals(0, wallet.balance.first())
    }

    @Test
    fun eachKeyPaysOnce() = runBlocking {
        assertEquals(10, wallet.award("break:2026-10-27:WALK", 10, SparkReason.BREAK, at))
        assertEquals(0, wallet.award("break:2026-10-27:WALK", 10, SparkReason.BREAK, at.plusSeconds(60)))
        assertEquals(10, wallet.balance.first())
    }

    @Test
    fun balanceIsTheSumOfTheLedger() = runBlocking {
        wallet.award("break:2026-10-27:WALK", 10, SparkReason.BREAK, at)
        wallet.award("break:2026-10-27:NECK", 5, SparkReason.BREAK, at)
        wallet.award("streak:3:2026-10-23", 20, SparkReason.STREAK, at)
        assertEquals(35, wallet.balance.first())
    }

    // ---- Spending (M8: adopting a pet) ----

    private fun earn(amount: Int) = runBlocking { wallet.award("break:seed:$amount", amount, SparkReason.BREAK, at) }

    @Test
    fun spendChargesOnceAndRepeatsAreFree() = runBlocking {
        earn(200)
        assertEquals(SpendOutcome.Spent, wallet.spend("unlock:pip", 150, SparkReason.UNLOCK, at))
        assertEquals(SpendOutcome.AlreadyPaid, wallet.spend("unlock:pip", 150, SparkReason.UNLOCK, at.plusSeconds(1)))
        assertEquals(50, wallet.balance.first())
        assertEquals(1, db.sparkLedgerDao().all().count { it.key == "unlock:pip" && it.amount == -150 })
    }

    @Test
    fun spendRefusesWhenTheBalanceIsShortAndWritesNothing() = runBlocking {
        earn(120)
        assertEquals(SpendOutcome.Insufficient(missing = 30), wallet.spend("unlock:pip", 150, SparkReason.UNLOCK, at))
        assertEquals(120, wallet.balance.first())
        assertEquals(1, db.sparkLedgerDao().all().size)
    }

    @Test
    fun spendingExactlyTheBalanceLeavesZero() = runBlocking {
        earn(150)
        assertEquals(SpendOutcome.Spent, wallet.spend("unlock:pip", 150, SparkReason.UNLOCK, at))
        assertEquals(0, wallet.balance.first())
    }

    /** A double (or 20-fold) tap on Adopt: one charge, whoever wins the race. */
    @Test
    fun concurrentSpendsOfOneKeyChargeOnce() = runBlocking {
        earn(1_000)
        val outcomes = (1..20).map {
            async(Dispatchers.IO) { wallet.spend("unlock:nimbus", 300, SparkReason.UNLOCK, at) }
        }.awaitAll()
        assertEquals(1, outcomes.count { it == SpendOutcome.Spent })
        assertEquals(19, outcomes.count { it == SpendOutcome.AlreadyPaid })
        assertEquals(700, wallet.balance.first())
    }

    /** Two different pets at once on a balance that covers only one: one wins, never overdrawn. */
    @Test
    fun concurrentSpendsNeverOverdraw() = runBlocking {
        repeat(25) { round ->
            val start = at.plusSeconds(round * 60L)
            // Back to exactly 200 sparks: enough for Pip (150) or Toast (200), not both.
            wallet.award("break:round:$round", 200 - wallet.balance.first(), SparkReason.BREAK, start)
            val outcomes = listOf("unlock:pip:$round" to 150, "unlock:toast:$round" to 200).map { (key, price) ->
                async(Dispatchers.IO) { wallet.spend(key, price, SparkReason.UNLOCK, start) }
            }.awaitAll()
            assertEquals("round $round: exactly one adoption fits", 1, outcomes.count { it == SpendOutcome.Spent })
            val balance = wallet.balance.first()
            assertTrue("round $round went below zero: $balance", balance >= 0)
        }
    }
}
