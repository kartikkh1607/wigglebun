package com.kartikkhandelwal.wigglebun.data.repo

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kartikkhandelwal.wigglebun.data.db.AppDatabase
import com.kartikkhandelwal.wigglebun.domain.model.SparkReason
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
}
