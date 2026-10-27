package com.kartikkhandelwal.wigglebun.data.repo

import com.kartikkhandelwal.wigglebun.data.prefs.inMemoryPreferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class WalletRepositoryTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @After
    fun tearDown() = scope.cancel()

    @Test
    fun newWalletStartsAtZero() = runBlocking {
        val wallet = WalletRepository(inMemoryPreferencesDataStore(scope))
        assertEquals(0, wallet.balance.first())
    }
}
