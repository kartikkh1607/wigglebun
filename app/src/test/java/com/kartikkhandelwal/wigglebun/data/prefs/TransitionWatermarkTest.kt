package com.kartikkhandelwal.wigglebun.data.prefs

import com.kartikkhandelwal.wigglebun.domain.model.MovementActivity
import com.kartikkhandelwal.wigglebun.domain.model.MovementTransition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class TransitionWatermarkTest {

    private fun event(elapsed: Long, activity: MovementActivity = MovementActivity.STILL) =
        ReceivedTransition(MovementTransition(activity, entered = true, at = Instant.EPOCH.plusNanos(elapsed)), elapsed)

    @Test
    fun noWatermarkAcceptsEverything() {
        val (accepted, mark) = acceptNewerThanWatermark(listOf(event(10), event(20)), watermark = null, bootCount = 1)
        assertEquals(2, accepted.size)
        assertEquals(TransitionWatermark(20, 1), mark)
    }

    @Test
    fun eventsAtOrBelowTheWatermarkAreIgnored() {
        val mark = TransitionWatermark(20, bootCount = 1)
        val (accepted, newMark) = acceptNewerThanWatermark(listOf(event(10), event(20), event(30)), mark, bootCount = 1)
        assertEquals(listOf(30L), accepted.map { it.elapsedRealtimeNanos })
        assertEquals(TransitionWatermark(30, 1), newMark)
    }

    @Test
    fun aReplayLeavesTheWatermarkUnchanged() {
        val mark = TransitionWatermark(20, bootCount = 1)
        val (accepted, newMark) = acceptNewerThanWatermark(listOf(event(20)), mark, bootCount = 1)
        assertEquals(emptyList<ReceivedTransition>(), accepted)
        assertEquals(mark, newMark)
    }

    @Test
    fun aRebootResetsTheWatermark() {
        val mark = TransitionWatermark(9_000_000, bootCount = 1)
        val (accepted, newMark) = acceptNewerThanWatermark(listOf(event(5)), mark, bootCount = 2)
        assertEquals(1, accepted.size)
        assertEquals(TransitionWatermark(5, 2), newMark)
    }

    @Test
    fun emptyBatchFromAnotherBootClearsTheOldWatermark() {
        val (accepted, newMark) = acceptNewerThanWatermark(emptyList(), TransitionWatermark(7, 1), bootCount = 2)
        assertEquals(0, accepted.size)
        assertNull(newMark)
    }
}
