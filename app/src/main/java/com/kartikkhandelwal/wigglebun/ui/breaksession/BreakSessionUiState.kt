package com.kartikkhandelwal.wigglebun.ui.breaksession

import androidx.annotation.StringRes
import com.kartikkhandelwal.wigglebun.data.repo.BreakDoneResult
import com.kartikkhandelwal.wigglebun.domain.model.ActivePet
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.domain.model.PetId
import com.kartikkhandelwal.wigglebun.domain.session.SessionTimer
import com.kartikkhandelwal.wigglebun.ui.pet.PoseSpec

/**
 * The line under "+5 sparks": the next pet to adopt and whether the sparks already cover it. With
 * every pet adopted, [id] is null and the line points at the accessories instead.
 */
data class NextPetHint(val id: PetId?, val price: Int = 0, val affordable: Boolean = false)

/** Used where the pet's real energy isn't known (previews, tests): the colour fills from here. */
const val SESSION_START_ENERGY = 0.4f

data class BreakSessionUiState(
    val type: BreakType,
    /** The active pet; null for the few ms until it loads (the pet isn't drawn until then). */
    val pet: ActivePet?,
    val stepIndex: Int,
    val stepCount: Int,
    @StringRes val title: Int,
    @StringRes val instruction: Int,
    val remainingMillis: Long,
    /** 0..1 through the current step (the ring). */
    val progress: Float,
    /** The step's poses in turn, each held [POSE_HOLD_MILLIS]. */
    val pose: PoseSpec?,
    /** The step's first pose, held for the whole step when animations are removed. */
    val stillPose: PoseSpec?,
    val mood: Mood,
    val isPaused: Boolean,
    val isLastStep: Boolean,
    /** Last step at 0:00, waiting for Done. */
    val isFinished: Boolean,
    /** Set once Done has been recorded: the celebration replaces the steps. */
    val result: BreakDoneResult?,
    /** 0..1 through every step; the pet's colour fills with it. */
    val overallProgress: Float = 0f,
    /** Shown on the celebration when this break paid sparks. */
    val nextPet: NextPetHint? = null,
    /** The pet's energy when the session began (what sitting had drained it to). */
    val startEnergy: Float = SESSION_START_ENERGY,
) {
    /** The pet's fill: its energy at the start, full when the last step ends. */
    val energy: Float get() = if (result != null) 1f else startEnergy + (1f - startEnergy) * overallProgress

    /** "Colour coming back!" until the colour is all back. */
    val colourIsBack: Boolean get() = overallProgress >= 1f

    /** No chip about colour when there was none to bring back (a pet that began at full energy). */
    val showColourChip: Boolean get() = startEnergy < 0.98f

    /** "Move gently. Stop if anything hurts." opens every session that moves the body. */
    val showSafetyNote: Boolean get() = stepIndex == 0 && type.hasSafetyNote

    /** Only while the countdown runs; a paused or finished session may let the screen sleep. */
    val keepScreenOn: Boolean get() = !isPaused && !isFinished && result == null
}

/** Pure mapping from the timer to what the screen shows (unit-tested). */
fun sessionUiState(
    type: BreakType,
    pet: ActivePet?,
    timer: SessionTimer,
    now: Long,
    result: BreakDoneResult?,
    nextPet: NextPetHint? = null,
    startEnergy: Float = SESSION_START_ENERGY,
): BreakSessionUiState {
    val step = type.sessionSteps()[timer.stepIndex]
    val pose = step.poses.getOrNull(timer.poseIndex(now, step.poses.size, POSE_HOLD_MILLIS))
    return BreakSessionUiState(
        type = type,
        pet = pet,
        stepIndex = timer.stepIndex,
        stepCount = timer.stepCount,
        title = step.title,
        instruction = step.text,
        remainingMillis = timer.remainingMillis(now),
        progress = timer.progress(now),
        pose = pose,
        stillPose = step.poses.firstOrNull(),
        mood = if (result != null) Mood.CELEBRATE else type.sessionMood,
        isPaused = !timer.isRunning,
        isLastStep = timer.isLastStep,
        isFinished = timer.isFinished(now),
        result = result,
        overallProgress = timer.overallProgress(now),
        nextPet = nextPet,
        startEnergy = startEnergy,
    )
}
