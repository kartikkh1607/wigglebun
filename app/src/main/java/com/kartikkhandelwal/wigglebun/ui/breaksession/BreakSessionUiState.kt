package com.kartikkhandelwal.wigglebun.ui.breaksession

import androidx.annotation.StringRes
import com.kartikkhandelwal.wigglebun.data.repo.BreakDoneResult
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.domain.session.SessionTimer
import com.kartikkhandelwal.wigglebun.ui.pet.PoseSpec

data class BreakSessionUiState(
    val type: BreakType,
    /** Null until the user names the pet (M9). */
    val petName: String?,
    val stepIndex: Int,
    val stepCount: Int,
    @StringRes val instruction: Int,
    val remainingMillis: Long,
    /** 0..1 through the current step (the ring). */
    val progress: Float,
    val pose: PoseSpec?,
    val mood: Mood,
    val isPaused: Boolean,
    val isLastStep: Boolean,
    /** Last step at 0:00, waiting for Done. */
    val isFinished: Boolean,
    /** Set once Done has been recorded: the celebration replaces the steps. */
    val result: BreakDoneResult?,
) {
    /** "Move gently. Stop if anything hurts." opens every session. */
    val showSafetyNote: Boolean get() = stepIndex == 0

    /** Only while the countdown runs; a paused or finished session may let the screen sleep. */
    val keepScreenOn: Boolean get() = !isPaused && !isFinished && result == null
}

/** Pure mapping from the timer to what the screen shows (unit-tested). */
fun sessionUiState(
    type: BreakType,
    petName: String?,
    timer: SessionTimer,
    now: Long,
    result: BreakDoneResult?,
): BreakSessionUiState {
    val step = type.sessionSteps()[timer.stepIndex]
    val pose = step.poses.getOrNull(timer.poseIndex(now, step.poses.size, POSE_HOLD_MILLIS))
    return BreakSessionUiState(
        type = type,
        petName = petName,
        stepIndex = timer.stepIndex,
        stepCount = timer.stepCount,
        instruction = step.text,
        remainingMillis = timer.remainingMillis(now),
        progress = timer.progress(now),
        pose = pose,
        mood = if (result != null) Mood.CELEBRATE else type.sessionMood,
        isPaused = !timer.isRunning,
        isLastStep = timer.isLastStep,
        isFinished = timer.isFinished(now),
        result = result,
    )
}
