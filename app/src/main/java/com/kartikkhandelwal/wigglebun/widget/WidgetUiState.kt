package com.kartikkhandelwal.wigglebun.widget

import androidx.annotation.DrawableRes
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.domain.sitting.SitStatus

/** What the home-screen widget shows (CLAUDE.md §8). One static PNG per mood: Glance can't animate. */
data class WidgetUiState(
    val mood: Mood,
    /** Minutes since the last break; null outside work hours (the pet sleeps, no timer). */
    val minutes: Int?,
    val petName: String,
    /** Opened by the medium widget's "Take a break" button. */
    val suggestedBreak: BreakType,
) {
    val isSleeping: Boolean get() = minutes == null

    @get:DrawableRes
    val petImage: Int get() = moodImage(mood)

    companion object {
        fun from(status: SitStatus, petName: String): WidgetUiState = WidgetUiState(
            mood = widgetMood(status),
            minutes = if (status.isWorkTime) status.sitMinutes else null,
            petName = petName,
            suggestedBreak = BreakType.suggestedFor(status.now.epochSecond / 60),
        )

        /**
         * CELEBRATE lasts 3 s, but the widget may not be redrawn for 15 min, so it would be stuck
         * cheering. Show the mood the pet settles into instead.
         */
        private fun widgetMood(status: SitStatus): Mood = when {
            !status.isWorkTime -> Mood.SLEEPING
            status.mood == Mood.CELEBRATE -> Mood.HAPPY
            else -> status.mood
        }

        @DrawableRes
        fun moodImage(mood: Mood): Int = when (mood) {
            Mood.HAPPY -> R.drawable.bun_mood_happy
            Mood.TIRED -> R.drawable.bun_mood_tired
            Mood.STIFF -> R.drawable.bun_mood_stiff
            Mood.STRETCH -> R.drawable.bun_mood_stretch
            Mood.CELEBRATE -> R.drawable.bun_mood_celebrate
            Mood.SLEEPING -> R.drawable.bun_mood_sleep
        }
    }
}
