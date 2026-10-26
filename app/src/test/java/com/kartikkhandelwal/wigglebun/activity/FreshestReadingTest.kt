package com.kartikkhandelwal.wigglebun.activity

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** StepCounter.readTotal()'s flush-then-read logic, without a real sensor. */
class FreshestReadingTest {

    private val cached = StepReading(total = 1_000, at = Instant.parse("2026-10-26T17:00:00Z"))
    private val fresh = StepReading(total = 1_150, at = Instant.parse("2026-10-26T17:04:59Z"))

    @Test
    fun flushThatNeverCompletesFallsBackToCachedAfterTimeout() = runBlocking {
        val started = System.nanoTime()
        val reading = freshestReading(
            requestFlush = { true },
            flushCompleted = CompletableDeferred(), // never completed
            latest = { cached },
            timeoutMillis = 200,
        )
        val waitedMillis = (System.nanoTime() - started) / 1_000_000
        assertEquals(cached, reading)
        assertTrue("waited $waitedMillis ms", waitedMillis >= 200)
    }

    @Test
    fun completedFlushReturnsTheNewestReading() = runBlocking {
        var latest = cached
        val done = CompletableDeferred<Unit>()
        val reading = freshestReading(
            requestFlush = {
                // The flushed batch arrives, then the flush-complete event.
                latest = fresh
                done.complete(Unit)
                true
            },
            flushCompleted = done,
            latest = { latest },
            timeoutMillis = 2_000,
        )
        assertEquals(fresh, reading)
    }

    @Test
    fun rejectedFlushReturnsCachedWithoutWaiting() = runBlocking {
        val started = System.nanoTime()
        val reading = freshestReading(
            requestFlush = { false },
            flushCompleted = CompletableDeferred(),
            latest = { cached },
            timeoutMillis = 5_000,
        )
        assertEquals(cached, reading)
        assertTrue((System.nanoTime() - started) / 1_000_000 < 1_000)
    }
}
