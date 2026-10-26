package com.kartikkhandelwal.wigglebun.ui.pet

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.domain.model.Mood

/**
 * Face shapes from design/pet_moods/bun_<mood>.svg, rewritten as SVG path data relative to
 * each eye centre (eyes) or to [PetSpec.mouthAnchor] (mouths), so every pet can reuse them.
 */
enum class EyeShape(val pathData: String?) {
    /** Filled dots, radius [PetSpec.eyeDotRadius]. The only shape that blinks. */
    Dots(null),

    /** Flat lines: tired, stiff. */
    Half("M-6 0 L6 0"),

    /** Curved down: sleeping. */
    Closed("M-6 -2 Q0 4 6 -2"),

    /** ^ ^ : stretch, celebrate. */
    Happy("M-6 2 Q0 -6 6 2"),
}

enum class MouthShape(val pathData: String, val filled: Boolean) {
    Nose("M-4 -2 Q0 3 4 -2 Z", filled = true),
    Flat("M-4 0 L4 0", filled = false),
    Frown("M-5 3 Q0 -2 5 3", filled = false),
    Small("M-2 0 Q0 2 2 0", filled = false),
    Open("M-8 -3 Q0 11 8 -3 Z", filled = true),
}

enum class ArmPose { Side, Up }

@Immutable
data class MoodSpec(
    val eyes: EyeShape,
    val mouth: MouthShape,
    /** Lean of everything except the feet, degrees about [PetSpec.pivot]. */
    val leanDeg: Float,
    val arms: ArmPose,
    val sweat: Boolean = false,
    val sleepZ: Boolean = false,
    val confetti: Boolean = false,
    /** Idle: one breath (scaleY 1 → 1 + amplitude → 1) per period; the body bobs in sync. */
    val breathPeriodSec: Float,
    val breathAmplitude: Float,
    val bobAmplitude: Float,
    val blinks: Boolean,
    @StringRes val description: Int,
)

/** Cached: called every frame by the animation loop, so it must not allocate. */
fun Mood.spec(): MoodSpec = moodSpecs[ordinal]

private val moodSpecs: List<MoodSpec> = Mood.entries.map { it.buildSpec() }

private fun Mood.buildSpec(): MoodSpec = when (this) {
    Mood.HAPPY -> MoodSpec(
        eyes = EyeShape.Dots, mouth = MouthShape.Nose, leanDeg = 0f, arms = ArmPose.Side,
        breathPeriodSec = 2.5f, breathAmplitude = 0.03f, bobAmplitude = 2f, blinks = true,
        description = R.string.pet_desc_happy,
    )
    Mood.TIRED -> MoodSpec(
        eyes = EyeShape.Half, mouth = MouthShape.Flat, leanDeg = 2f, arms = ArmPose.Side,
        breathPeriodSec = 3.5f, breathAmplitude = 0.02f, bobAmplitude = 1f, blinks = false,
        description = R.string.pet_desc_tired,
    )
    Mood.STIFF -> MoodSpec(
        eyes = EyeShape.Half, mouth = MouthShape.Frown, leanDeg = 7f, arms = ArmPose.Side,
        sweat = true,
        breathPeriodSec = 4.5f, breathAmplitude = 0.012f, bobAmplitude = 0.5f, blinks = false,
        description = R.string.pet_desc_stiff,
    )
    Mood.STRETCH -> MoodSpec(
        eyes = EyeShape.Happy, mouth = MouthShape.Nose, leanDeg = 0f, arms = ArmPose.Up,
        breathPeriodSec = 2.5f, breathAmplitude = 0.02f, bobAmplitude = 1f, blinks = false,
        description = R.string.pet_desc_stretch,
    )
    Mood.CELEBRATE -> MoodSpec(
        eyes = EyeShape.Happy, mouth = MouthShape.Open, leanDeg = -4f, arms = ArmPose.Up,
        confetti = true,
        breathPeriodSec = 2.5f, breathAmplitude = 0.03f, bobAmplitude = 2f, blinks = false,
        description = R.string.pet_desc_celebrate,
    )
    Mood.SLEEPING -> MoodSpec(
        eyes = EyeShape.Closed, mouth = MouthShape.Small, leanDeg = 0f, arms = ArmPose.Side,
        sleepZ = true,
        breathPeriodSec = 4f, breathAmplitude = 0.03f, bobAmplitude = 0f, blinks = false,
        description = R.string.pet_desc_sleeping,
    )
}
