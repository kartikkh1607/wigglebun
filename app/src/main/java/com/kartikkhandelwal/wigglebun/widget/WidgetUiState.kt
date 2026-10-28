package com.kartikkhandelwal.wigglebun.widget

import androidx.annotation.DrawableRes
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.domain.model.PetId
import com.kartikkhandelwal.wigglebun.domain.sitting.SitStatus

/** What the home-screen widget shows (CLAUDE.md §8). One static PNG per mood per pet: Glance can't animate. */
data class WidgetUiState(
    val mood: Mood,
    /** Minutes since the last break; null outside work hours (the pet sleeps, no timer). */
    val minutes: Int?,
    /** The active pet (M8), drawn from its own images. */
    val pet: PetId,
    val petName: String,
    /** Opened by the medium widget's "Take a break" button. */
    val suggestedBreak: BreakType,
) {
    val isSleeping: Boolean get() = minutes == null

    @get:DrawableRes
    val petImage: Int get() = moodImage(pet, mood)

    companion object {
        fun from(status: SitStatus, pet: PetId, petName: String): WidgetUiState = WidgetUiState(
            mood = widgetMood(status),
            minutes = if (status.isWorkTime) status.sitMinutes else null,
            pet = pet,
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

        /** res/drawable-nodpi/<pet>_mood_<mood>.png, rendered from PetView by WidgetPetImageTest. */
        @DrawableRes
        fun moodImage(pet: PetId, mood: Mood): Int = when (pet) {
            PetId.BUN -> when (mood) {
                Mood.HAPPY -> R.drawable.bun_mood_happy
                Mood.TIRED -> R.drawable.bun_mood_tired
                Mood.STIFF -> R.drawable.bun_mood_stiff
                Mood.STRETCH -> R.drawable.bun_mood_stretch
                Mood.CELEBRATE -> R.drawable.bun_mood_celebrate
                Mood.SLEEPING -> R.drawable.bun_mood_sleep
            }
            PetId.PIP -> when (mood) {
                Mood.HAPPY -> R.drawable.pip_mood_happy
                Mood.TIRED -> R.drawable.pip_mood_tired
                Mood.STIFF -> R.drawable.pip_mood_stiff
                Mood.STRETCH -> R.drawable.pip_mood_stretch
                Mood.CELEBRATE -> R.drawable.pip_mood_celebrate
                Mood.SLEEPING -> R.drawable.pip_mood_sleep
            }
            PetId.TOAST -> when (mood) {
                Mood.HAPPY -> R.drawable.toast_mood_happy
                Mood.TIRED -> R.drawable.toast_mood_tired
                Mood.STIFF -> R.drawable.toast_mood_stiff
                Mood.STRETCH -> R.drawable.toast_mood_stretch
                Mood.CELEBRATE -> R.drawable.toast_mood_celebrate
                Mood.SLEEPING -> R.drawable.toast_mood_sleep
            }
            PetId.NIMBUS -> when (mood) {
                Mood.HAPPY -> R.drawable.nimbus_mood_happy
                Mood.TIRED -> R.drawable.nimbus_mood_tired
                Mood.STIFF -> R.drawable.nimbus_mood_stiff
                Mood.STRETCH -> R.drawable.nimbus_mood_stretch
                Mood.CELEBRATE -> R.drawable.nimbus_mood_celebrate
                Mood.SLEEPING -> R.drawable.nimbus_mood_sleep
            }
        }
    }
}
