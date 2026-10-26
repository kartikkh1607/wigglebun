package com.kartikkhandelwal.wigglebun.domain.model

/** Pet mood, see CLAUDE.md §5.2. */
enum class Mood {
    /** 0–50% of the break interval sat. */
    HAPPY,

    /** 50–100% of the interval. */
    TIRED,

    /** Over the interval. */
    STIFF,

    /** During a break. */
    STRETCH,

    /** Just finished a break (3 s, then HAPPY). */
    CELEBRATE,

    /** Outside work hours. */
    SLEEPING,
}
