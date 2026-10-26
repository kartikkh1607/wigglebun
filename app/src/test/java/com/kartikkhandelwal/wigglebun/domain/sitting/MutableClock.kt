package com.kartikkhandelwal.wigglebun.domain.sitting

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/** A clock the test moves by hand. */
internal class MutableClock(var now: Instant, private val zone: ZoneId) : Clock() {
    override fun getZone(): ZoneId = zone
    override fun withZone(zone: ZoneId): Clock = MutableClock(now, zone)
    override fun instant(): Instant = now
    fun advance(minutes: Long) { now = now.plusSeconds(minutes * 60) }
    fun advanceSeconds(seconds: Long) { now = now.plusSeconds(seconds) }
}
