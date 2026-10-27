package com.kartikkhandelwal.wigglebun.domain.session

/**
 * The break-session countdown (CLAUDE.md §6.4). Computed from elapsed time, never by counting
 * ticks, so late or missed ticks and time spent in the background can't make it drift.
 * All times are monotonic milliseconds. Immutable: every operation returns a new timer.
 *
 * Steps advance by themselves when their time is up (overflow carries into the next step); the
 * last step stops at 0:00 and waits for Done.
 */
data class SessionTimer(
    val stepDurationsMillis: List<Long>,
    val stepIndex: Int = 0,
    /** Time already counted in the current step before [runningSinceMillis]. */
    val countedMillis: Long = 0,
    /** When the current run started; null while paused. */
    val runningSinceMillis: Long? = null,
) {
    init {
        require(stepDurationsMillis.isNotEmpty() && stepDurationsMillis.all { it > 0 })
        require(stepIndex in stepDurationsMillis.indices)
    }

    val stepCount: Int get() = stepDurationsMillis.size
    val stepDurationMillis: Long get() = stepDurationsMillis[stepIndex]
    val isLastStep: Boolean get() = stepIndex == stepDurationsMillis.lastIndex
    val isRunning: Boolean get() = runningSinceMillis != null

    /** Time spent in the current step, at most its duration. Call [advance] first to leave finished steps. */
    fun elapsedMillis(now: Long): Long = rawElapsed(now).coerceIn(0, stepDurationMillis)

    fun remainingMillis(now: Long): Long = stepDurationMillis - elapsedMillis(now)

    fun progress(now: Long): Float = elapsedMillis(now).toFloat() / stepDurationMillis

    fun isFinished(now: Long): Boolean = isLastStep && remainingMillis(now) == 0L

    /** Moves past every step whose time is up, carrying the overflow; the last step stops at 0:00. */
    fun advance(now: Long): SessionTimer {
        var timer = this
        while (!timer.isLastStep && timer.rawElapsed(now) >= timer.stepDurationMillis) {
            val overflow = timer.rawElapsed(now) - timer.stepDurationMillis
            timer = timer.copy(
                stepIndex = timer.stepIndex + 1,
                countedMillis = overflow,
                runningSinceMillis = if (timer.isRunning) now else null,
            )
        }
        return timer
    }

    fun pause(now: Long): SessionTimer {
        val current = advance(now)
        if (!current.isRunning) return current
        return current.copy(countedMillis = current.elapsedMillis(now), runningSinceMillis = null)
    }

    fun resume(now: Long): SessionTimer = if (isRunning) this else copy(runningSinceMillis = now)

    /** Skip to the next step straight away (a no-op on the last step, which ends with Done). */
    fun next(now: Long): SessionTimer {
        val current = advance(now)
        if (current.isLastStep) return current
        return current.copy(
            stepIndex = current.stepIndex + 1,
            countedMillis = 0,
            runningSinceMillis = if (current.isRunning) now else null,
        )
    }

    /** Which of [poseCount] poses to show: each is held [holdMillis], looping. */
    fun poseIndex(now: Long, poseCount: Int, holdMillis: Long): Int =
        if (poseCount <= 1) 0 else ((elapsedMillis(now) / holdMillis) % poseCount).toInt()

    private fun rawElapsed(now: Long): Long =
        countedMillis + (runningSinceMillis?.let { (now - it).coerceAtLeast(0) } ?: 0)

    companion object {
        /** [stepCount] steps sharing [totalMillis] evenly (the last step takes any remainder), running from [now]. */
        fun start(totalMillis: Long, stepCount: Int, now: Long): SessionTimer =
            SessionTimer(evenSplit(totalMillis, stepCount), runningSinceMillis = now)

        fun evenSplit(totalMillis: Long, stepCount: Int): List<Long> {
            require(stepCount > 0)
            val each = totalMillis / stepCount
            return List(stepCount) { i -> if (i == stepCount - 1) totalMillis - each * (stepCount - 1) else each }
        }
    }
}
