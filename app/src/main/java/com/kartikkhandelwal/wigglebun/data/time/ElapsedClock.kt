package com.kartikkhandelwal.wigglebun.data.time

/**
 * Monotonic milliseconds for on-screen timers (break session). Unlike [AppClock] it never jumps
 * when the user changes the time, and it keeps counting while the screen is off
 * (SystemClock.elapsedRealtime in the app, a fake in tests).
 */
fun interface ElapsedClock {
    fun nowMillis(): Long
}
