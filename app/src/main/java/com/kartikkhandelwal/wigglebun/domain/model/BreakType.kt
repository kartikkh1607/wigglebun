package com.kartikkhandelwal.wigglebun.domain.model

/** Shown as the chip on the focused break card. */
enum class BreakCategory { MOVEMENT, STRETCH, EYES, WATER }

/** Break types from CLAUDE.md §5.3. Rewards are in sparks. */
enum class BreakType(val durationMinutes: Int, val defaultReward: Int, val category: BreakCategory) {
    STAND_UP(durationMinutes = 1, defaultReward = 5, category = BreakCategory.MOVEMENT),
    WALK(durationMinutes = 2, defaultReward = 10, category = BreakCategory.MOVEMENT),
    NECK(durationMinutes = 2, defaultReward = 5, category = BreakCategory.STRETCH),
    SHOULDERS(durationMinutes = 1, defaultReward = 5, category = BreakCategory.STRETCH),
    WRIST(durationMinutes = 1, defaultReward = 5, category = BreakCategory.STRETCH),
    EYES(durationMinutes = 1, defaultReward = 5, category = BreakCategory.EYES),
    WATER(durationMinutes = 1, defaultReward = 5, category = BreakCategory.WATER),
    ;

    companion object {
        /** Rotates the suggested break so consecutive reminders vary (deterministic per minute). */
        fun suggestedFor(epochMinute: Long): BreakType = entries[Math.floorMod(epochMinute, entries.size.toLong()).toInt()]

        /** Parses a stored/sent name (route arg, intent extra); null if unknown. */
        fun fromName(name: String?): BreakType? = entries.firstOrNull { it.name == name }
    }
}

enum class BreakOutcome { DONE, SKIPPED }
