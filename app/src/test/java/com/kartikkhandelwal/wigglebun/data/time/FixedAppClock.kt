package com.kartikkhandelwal.wigglebun.data.time

import java.time.Instant
import java.time.ZoneId

/**
 * An [AppClock] stopped at noon on a Monday, for tests that sit for a while before "now". Real
 * time would make them fail just after midnight, when a work day that ends at midnight has only
 * just started and the sit timer can't be older than that.
 */
internal class FixedAppClock(
    private val now: Instant = NOON,
    private val fixedZone: ZoneId = ZONE,
) : AppClock() {
    override fun realMillis(): Long = now.toEpochMilli()

    override fun getZone(): ZoneId = fixedZone

    companion object {
        val ZONE: ZoneId = ZoneId.of("Asia/Kolkata")
        val NOON: Instant = Instant.parse("2026-10-26T06:30:00Z") // Monday 12:00 in Kolkata
    }
}
