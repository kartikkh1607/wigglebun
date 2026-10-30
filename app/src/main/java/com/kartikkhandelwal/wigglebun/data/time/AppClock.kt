package com.kartikkhandelwal.wigglebun.data.time

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The app's clock: the system clock, optionally sped up for the debug Pet lab
 * ("fast clock": 1 minute = 1 second). Release builds never change the speed.
 *
 * The zone is read on every call, so a timezone change is picked up immediately.
 */
// Open so tests can pin the time.
@Singleton
open class AppClock @Inject constructor() : Clock() {

    private data class Config(val speed: Int, val realAnchorMillis: Long, val fakeAnchorMillis: Long)

    @Volatile
    private var config = Config(speed = 1, realAnchorMillis = 0L, fakeAnchorMillis = 0L)

    private val _speed = MutableStateFlow(1)
    val speed: StateFlow<Int> = _speed.asStateFlow()

    /** Switch speed; app time continues from the current moment at the new rate. */
    @Synchronized
    fun setSpeed(newSpeed: Int) {
        require(newSpeed >= 1)
        val real = realMillis()
        config = if (newSpeed == 1) {
            // Back to real time. App time jumps back; stored future instants are clamped by the engine.
            Config(1, 0L, 0L)
        } else {
            Config(newSpeed, realAnchorMillis = real, fakeAnchorMillis = real)
        }
        _speed.value = newSpeed
    }

    /** The real wall-clock time, before any speed-up. */
    protected open fun realMillis(): Long = System.currentTimeMillis()

    override fun millis(): Long {
        val c = config
        val real = realMillis()
        return if (c.speed == 1) real else c.fakeAnchorMillis + (real - c.realAnchorMillis) * c.speed
    }

    override fun instant(): Instant = Instant.ofEpochMilli(millis())

    override fun getZone(): ZoneId = ZoneId.systemDefault()

    override fun withZone(zone: ZoneId): Clock = ZonedView(zone)

    /** Converts an app-time instant to the real wall-clock time AlarmManager needs. */
    fun toRealEpochMillis(instant: Instant): Long {
        val c = config
        val ms = instant.toEpochMilli()
        return if (c.speed == 1) ms else c.realAnchorMillis + (ms - c.fakeAnchorMillis) / c.speed
    }

    /** Inverse of [toRealEpochMillis]: a real wall-clock time (e.g. a sensor event) in app time. */
    fun fromRealEpochMillis(realMillis: Long): Instant {
        val c = config
        return Instant.ofEpochMilli(
            if (c.speed == 1) realMillis else c.fakeAnchorMillis + (realMillis - c.realAnchorMillis) * c.speed,
        )
    }

    private inner class ZonedView(private val zone: ZoneId) : Clock() {
        override fun getZone(): ZoneId = zone
        override fun withZone(zone: ZoneId): Clock = ZonedView(zone)
        override fun instant(): Instant = this@AppClock.instant()
    }

    companion object {
        const val FAST_SPEED = 60
    }
}
