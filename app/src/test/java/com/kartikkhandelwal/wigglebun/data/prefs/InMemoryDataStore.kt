package com.kartikkhandelwal.wigglebun.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.core.InterProcessCoordinator
import androidx.datastore.core.ReadScope
import androidx.datastore.core.Storage
import androidx.datastore.core.StorageConnection
import androidx.datastore.core.WriteScope
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger

/**
 * A real Preferences DataStore whose storage is kept in memory, for tests. Only the storage is
 * swapped; DataStore's caching, serialised writes and flows are the production ones.
 *
 * File-backed test stores write a temp file and rename it over the real one. On Windows that
 * rename fails with AccessDeniedException whenever another handle (e.g. a virus scanner) has the
 * file open, which made the repository tests flaky.
 */
fun inMemoryPreferencesDataStore(scope: CoroutineScope): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(storage = InMemoryStorage(emptyPreferences()), scope = scope)

/** Holds the latest value. Reads and writes are serialised the same way FileStorage does it. */
class InMemoryStorage<T>(initial: T) : Storage<T> {

    @Volatile
    private var value: T = initial
    private val transaction = Mutex()
    private val coordinator = SingleProcessTestCoordinator()

    private val readWrite = object : WriteScope<T> {
        override suspend fun readData(): T = value

        override suspend fun writeData(value: T) {
            this@InMemoryStorage.value = value
        }

        override fun close() = Unit
    }

    override fun createConnection(): StorageConnection<T> = object : StorageConnection<T> {
        override val coordinator: InterProcessCoordinator get() = this@InMemoryStorage.coordinator

        // Like FileStorage: a read reports whether it held the transaction lock (no write in flight).
        override suspend fun <R> readScope(block: suspend ReadScope<T>.(locked: Boolean) -> R): R {
            val locked = transaction.tryLock()
            try {
                return readWrite.block(locked)
            } finally {
                if (locked) transaction.unlock()
            }
        }

        override suspend fun writeScope(block: suspend WriteScope<T>.() -> Unit) =
            transaction.withLock { readWrite.block() }

        override fun close() = Unit
    }
}

/** What DataStore uses for a single process: a mutex plus a version counter, no notifications. */
private class SingleProcessTestCoordinator : InterProcessCoordinator {
    private val mutex = Mutex()
    private val version = AtomicInteger(0)

    override val updateNotifications: Flow<Unit> = emptyFlow()

    override suspend fun <T> lock(block: suspend () -> T): T = mutex.withLock { block() }

    override suspend fun <T> tryLock(block: suspend (Boolean) -> T): T {
        val locked = mutex.tryLock()
        try {
            return block(locked)
        } finally {
            if (locked) mutex.unlock()
        }
    }

    override suspend fun getVersion(): Int = version.get()

    override suspend fun incrementAndGetVersion(): Int = version.incrementAndGet()
}
