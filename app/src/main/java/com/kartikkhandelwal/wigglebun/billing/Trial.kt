package com.kartikkhandelwal.wigglebun.billing

/**
 * Free trial terms (CLAUDE.md §5.5). The trial itself is the free phase of the yearly base plan's
 * offer in Play Console, which must match [FREE_TRIAL_DAYS]. Copy ("Try 7 days free") and the
 * trial-end reminder use these; the paywall still checks the offer really has a free phase.
 */
object Trial {
    const val FREE_TRIAL_DAYS = 7

    /** Our own "Your free trial ends on …" notification: Android doesn't send one. */
    const val END_REMINDER_DAYS_BEFORE = 2
}
