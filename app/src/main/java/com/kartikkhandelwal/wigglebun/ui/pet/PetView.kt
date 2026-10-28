package com.kartikkhandelwal.wigglebun.ui.pet

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.ui.theme.BreakSky
import com.kartikkhandelwal.wigglebun.ui.theme.Card
import com.kartikkhandelwal.wigglebun.ui.theme.Cream
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.Fredoka
import com.kartikkhandelwal.wigglebun.ui.theme.LockedSilhouette
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme

/** Energy drains/refills with a slow tween so the colour visibly moves (CLAUDE.md §5.2: ≥ 600 ms). */
private const val ENERGY_TWEEN_MS = 700

/** Poses settle with a little overshoot, well within the ~1.2 s hold (CLAUDE.md §7). */
private const val POSE_DAMPING = 0.6f

/**
 * The pet, drawn layer by layer from its [PetSpec] (CLAUDE.md §7).
 *
 * @param energy 0..1; the body colour fills from the bottom up to this level.
 * @param animated idle breathing, blinking, lean spring, celebrate jump, z's and sweat.
 *   Off in previews and screenshot tests, which then show the exact still pose of
 *   design/pet_moods/bun_<mood>.svg.
 * @param sleepZSide RIGHT as in bun_sleep.svg; Home uses LEFT so the z's stay clear of the
 *   plant pot in the desk scene.
 * @param pose break-session pose (CLAUDE.md §7); its rotation, scale and lift spring from the
 *   previous pose, its arm / eye / feet layers swap at once. Null = the mood alone.
 */
@Composable
fun PetView(
    energy: Float,
    mood: Mood,
    modifier: Modifier = Modifier,
    spec: PetSpec = PetSpecs.Bun,
    name: String = stringResource(spec.defaultName),
    animated: Boolean = !LocalInspectionMode.current,
    sleepZSide: SleepZSide = SleepZSide.RIGHT,
    pose: PoseSpec? = null,
) {
    val moodSpec = mood.spec()
    val energyState = animateFloatAsState(
        targetValue = energy.coerceIn(0f, 1f),
        animationSpec = if (animated) tween(ENERGY_TWEEN_MS, easing = FastOutSlowInEasing) else snap(),
        label = "petEnergy",
    )
    val leanState = animateFloatAsState(
        targetValue = moodSpec.leanDeg,
        animationSpec = if (animated) {
            spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow)
        } else {
            snap()
        },
        label = "petLean",
    )
    val poseSpring = if (animated) spring<Float>(dampingRatio = POSE_DAMPING, stiffness = Spring.StiffnessLow) else snap()
    val poseRotation = animateFloatAsState(pose?.rotationDeg ?: 0f, poseSpring, label = "poseRotation")
    val poseScaleX = animateFloatAsState(pose?.scaleX ?: 1f, poseSpring, label = "poseScaleX")
    val poseScaleY = animateFloatAsState(pose?.scaleY ?: 1f, poseSpring, label = "poseScaleY")
    val poseLift = animateFloatAsState(pose?.liftY ?: 0f, poseSpring, label = "poseLift")

    val geometry = rememberPetGeometry(spec)

    val motion = remember { PetMotion() }
    val currentMood by rememberUpdatedState(mood)
    if (animated) {
        LaunchedEffect(motion) {
            // One callback instance for the whole loop, so no per-frame lambda allocation.
            val onFrame: (Long) -> Unit = { nanos -> motion.step(nanos, currentMood, currentMood.spec()) }
            while (true) {
                withFrameNanos(onFrame)
            }
        }
    }

    val description = stringResource(moodSpec.description, name)
    Canvas(modifier.semantics { contentDescription = description }) {
        // Reading frameNanos here (draw phase only) redraws every frame without recomposing.
        motion.frameNanos
        drawPet(
            geometry = geometry,
            spec = spec,
            mood = moodSpec,
            energy = energyState.value,
            leanDeg = leanState.value,
            motion = motion,
            still = !animated,
            sleepZSide = sleepZSide,
            pose = pose,
            poseRotation = poseRotation.value,
            poseScaleX = poseScaleX.value,
            poseScaleY = poseScaleY.value,
            poseLift = poseLift.value,
        )
    }
}

/**
 * A pet that isn't adopted yet: a flat grey silhouette (Collection.dc.html), no face, no motion.
 * [description] is read by TalkBack.
 */
@Composable
fun LockedPetView(spec: PetSpec, description: String, modifier: Modifier = Modifier) {
    val geometry = rememberPetGeometry(spec)
    Canvas(modifier.semantics { contentDescription = description }) {
        drawLockedPet(geometry, spec, LockedSilhouette)
    }
}

/** Built once per pet; the sleeping z's are measured in viewBox units. */
@Composable
private fun rememberPetGeometry(spec: PetSpec): PetGeometry {
    val textMeasurer = rememberTextMeasurer()
    return remember(spec, textMeasurer) {
        // Density(1) makes 1 sp = 1 px, so the z sizes are in viewBox units like everything else.
        fun measureZ(size: Float) = textMeasurer.measure(
            text = "z",
            style = TextStyle(
                color = spec.sleepZColor,
                fontFamily = Fredoka,
                fontWeight = FontWeight.SemiBold,
                fontSize = size.sp,
            ),
            density = Density(1f),
        )
        PetGeometry(spec, sleepZLarge = measureZ(24f), sleepZSmall = measureZ(16f))
    }
}

/** Energy of the still poses in design/pet_moods and design/widget_png. */
internal fun Mood.previewEnergy(): Float = when (this) {
    Mood.TIRED -> 0.55f
    Mood.STIFF -> 0.18f
    else -> 1f
}

/** The pet at widget-PNG proportions (200×210, the SVG viewBox) on the app background. */
@Composable
internal fun PetMoodPreview(mood: Mood, spec: PetSpec = PetSpecs.Bun) {
    WigglebunTheme {
        PetView(
            energy = mood.previewEnergy(),
            mood = mood,
            spec = spec,
            modifier = Modifier
                .testTag("pet")
                .background(Cream)
                .size(200.dp, 210.dp),
        )
    }
}

@Preview(name = "Happy")
@Composable
private fun PetHappyPreview() = PetMoodPreview(Mood.HAPPY)

@Preview(name = "Tired")
@Composable
private fun PetTiredPreview() = PetMoodPreview(Mood.TIRED)

@Preview(name = "Stiff")
@Composable
private fun PetStiffPreview() = PetMoodPreview(Mood.STIFF)

@Preview(name = "Stretch")
@Composable
private fun PetStretchPreview() = PetMoodPreview(Mood.STRETCH)

@Preview(name = "Celebrate")
@Composable
private fun PetCelebratePreview() = PetMoodPreview(Mood.CELEBRATE)

@Preview(name = "Sleeping")
@Composable
private fun PetSleepingPreview() = PetMoodPreview(Mood.SLEEPING)

@Preview(name = "All moods", widthDp = 420)
@Composable
internal fun PetAllMoodsPreview(spec: PetSpec = PetSpecs.Bun) {
    WigglebunTheme {
        FlowRow(
            modifier = Modifier
                .background(Cream)
                .padding(Dimens.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(Dimens.ListGap),
            verticalArrangement = Arrangement.spacedBy(Dimens.ListGap),
        ) {
            Mood.entries.forEach { mood ->
                PetView(
                    energy = mood.previewEnergy(),
                    mood = mood,
                    spec = spec,
                    modifier = Modifier.size(120.dp, 126.dp),
                )
            }
        }
    }
}

/** Every break-session pose, in the order of the three frame SVGs, on the session background. */
@Preview(name = "All poses", widthDp = 420)
@Composable
internal fun PetAllPosesPreview(spec: PetSpec = PetSpecs.Bun) {
    val poses = listOf(
        Poses.NeckCentre, Poses.NeckTiltRight, Poses.NeckCentre, Poses.NeckTiltLeft,
        Poses.Sit, Poses.PushUp, Poses.Stand, Poses.ReachUp,
        Poses.ShouldersNeutral, Poses.ShouldersUp, Poses.ShouldersBack, Poses.ShouldersDown,
        Poses.Stand, Poses.WristRight, Poses.Stand, Poses.WristLeft,
        Poses.MarchA, Poses.MarchB, Poses.MarchC, Poses.EyesClosed,
    )
    WigglebunTheme {
        FlowRow(
            modifier = Modifier
                .background(BreakSky)
                .padding(Dimens.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(Dimens.ListGap),
            verticalArrangement = Arrangement.spacedBy(Dimens.ListGap),
            maxItemsInEachRow = 4,
        ) {
            poses.forEach { pose ->
                PetView(energy = 1f, mood = Mood.STRETCH, spec = spec, pose = pose, modifier = Modifier.size(88.dp, 92.dp))
            }
        }
    }
}

@Preview(name = "Pip moods", widthDp = 420)
@Composable
private fun PipMoodsPreview() = PetAllMoodsPreview(PetSpecs.Pip)

@Preview(name = "Toast moods", widthDp = 420)
@Composable
private fun ToastMoodsPreview() = PetAllMoodsPreview(PetSpecs.Toast)

@Preview(name = "Nimbus moods", widthDp = 420)
@Composable
private fun NimbusMoodsPreview() = PetAllMoodsPreview(PetSpecs.Nimbus)

@Preview(name = "Locked", widthDp = 420)
@Composable
private fun LockedPetsPreview() {
    WigglebunTheme {
        FlowRow(
            modifier = Modifier.background(Card).padding(Dimens.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(Dimens.ListGap),
        ) {
            listOf(PetSpecs.Pip, PetSpecs.Toast, PetSpecs.Nimbus).forEach { spec ->
                LockedPetView(spec, description = stringResource(spec.defaultName), modifier = Modifier.size(112.dp))
            }
        }
    }
}
