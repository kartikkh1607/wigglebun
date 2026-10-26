package com.kartikkhandelwal.wigglebun.domain.model

/** Break types from CLAUDE.md §5.3. Rewards are in sparks. */
enum class BreakType(val durationMinutes: Int, val defaultReward: Int) {
    STAND_UP(durationMinutes = 1, defaultReward = 5),
    WALK(durationMinutes = 2, defaultReward = 10),
    NECK(durationMinutes = 2, defaultReward = 5),
    SHOULDERS(durationMinutes = 1, defaultReward = 5),
    WRIST(durationMinutes = 1, defaultReward = 5),
    EYES(durationMinutes = 1, defaultReward = 5),
    WATER(durationMinutes = 1, defaultReward = 5),
    ;

    companion object {
        /** Rotates the suggested break so consecutive reminders vary (deterministic per minute). */
        fun suggestedFor(epochMinute: Long): BreakType = entries[Math.floorMod(epochMinute, entries.size.toLong()).toInt()]
    }
}

enum class BreakOutcome { DONE, SKIPPED }
