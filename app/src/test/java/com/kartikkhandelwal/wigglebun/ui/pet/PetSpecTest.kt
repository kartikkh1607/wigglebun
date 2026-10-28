package com.kartikkhandelwal.wigglebun.ui.pet

import androidx.compose.ui.geometry.Offset
import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.domain.model.PetId
import com.kartikkhandelwal.wigglebun.ui.theme.NimbusLilac
import com.kartikkhandelwal.wigglebun.ui.theme.NimbusLilacEdge
import com.kartikkhandelwal.wigglebun.ui.theme.PetGrey
import com.kartikkhandelwal.wigglebun.ui.theme.PetYellow
import com.kartikkhandelwal.wigglebun.ui.theme.PipBeak
import com.kartikkhandelwal.wigglebun.ui.theme.PipGreen
import com.kartikkhandelwal.wigglebun.ui.theme.PipGreenEdge
import com.kartikkhandelwal.wigglebun.ui.theme.ToastCrust
import com.kartikkhandelwal.wigglebun.ui.theme.ToastCrustDrained
import com.kartikkhandelwal.wigglebun.ui.theme.ToastGold
import com.kartikkhandelwal.wigglebun.ui.theme.ToastGoldEdge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Checks the data against design/pet_moods/bun_<mood>.svg and CLAUDE.md §5.2 / §7. */
class PetSpecTest {

    private val bun = PetSpecs.Bun

    @Test
    fun fillTopMatchesMoodSvgs() {
        assertEquals(36f, bun.fillTop(1f), 0.01f) // happy, sleep, stretch, celebrate
        assertEquals(108f, bun.fillTop(0.55f), 0.01f) // tired
        assertEquals(167.2f, bun.fillTop(0.18f), 0.01f) // stiff
        assertEquals(196f, bun.fillTop(-1f), 0.01f) // clamped
    }

    @Test
    fun feetAndArmsColourFollowFillTop() {
        // Tired: both still yellow.
        assertEquals(PetYellow, partColor(bun, bun.feetColoredAbove, bun.fillTop(0.55f)))
        assertEquals(PetYellow, partColor(bun, bun.armsColoredAbove, bun.fillTop(0.55f)))
        // Stiff: arms drained, feet still yellow (fillTop 167.2 is between 128 and 185).
        assertEquals(PetGrey, partColor(bun, bun.armsColoredAbove, bun.fillTop(0.18f)))
        assertEquals(PetYellow, partColor(bun, bun.feetColoredAbove, bun.fillTop(0.18f)))
        // Empty: everything drained.
        assertEquals(PetGrey, partColor(bun, bun.feetColoredAbove, bun.fillTop(0f)))
    }

    @Test
    fun moodTableMatchesDesign() {
        assertEquals(listOf(0f, 2f, 7f, 0f, -4f, 0f), Mood.entries.map { it.spec().leanDeg })
        assertEquals(EyeShape.Dots, Mood.HAPPY.spec().eyes)
        assertEquals(EyeShape.Half, Mood.TIRED.spec().eyes)
        assertEquals(MouthShape.Frown, Mood.STIFF.spec().mouth)
        assertEquals(EyeShape.Closed, Mood.SLEEPING.spec().eyes)
        assertEquals(MouthShape.Open, Mood.CELEBRATE.spec().mouth)
        assertEquals(ArmPose.Up, Mood.STRETCH.spec().arms)
        assertEquals(listOf(Mood.STIFF), Mood.entries.filter { it.spec().sweat })
        assertEquals(listOf(Mood.CELEBRATE), Mood.entries.filter { it.spec().confetti })
        assertEquals(listOf(Mood.SLEEPING), Mood.entries.filter { it.spec().sleepZ })
    }

    @Test
    fun specIsCachedSoTheFrameLoopDoesNotAllocate() {
        Mood.entries.forEach { assert(it.spec() === it.spec()) }
    }

    // ---- Collection pets (M8): numbers from design/pets/README.md and <pet>_pet.svg ----

    @Test
    fun everyPetIdHasItsOwnSpec() {
        PetId.entries.forEach { assertEquals(it, PetSpecs.of(it).id) }
    }

    @Test
    fun collectionPetsShareBunsSystem() {
        listOf(PetSpecs.Pip, PetSpecs.Toast, PetSpecs.Nimbus).forEach { pet ->
            assertEquals(bun.fillTop(0.55f), pet.fillTop(0.55f), 0.01f)
            assertEquals(bun.feet, pet.feet)
            assertEquals(bun.eyeLeft, pet.eyeLeft)
            assertEquals(bun.mouthAnchor, pet.mouthAnchor)
            assertEquals(bun.pivot, pet.pivot)
            assertEquals(185f, pet.feetColoredAbove)
            assertEquals(128f, pet.armsColoredAbove)
            assertEquals(Offset(16f, -18f), pet.sleepZStep)
            assertTrue("no ears_inner on ${pet.id}", pet.earsInner.isEmpty())
        }
    }

    @Test
    fun colourAndExtrasMatchTheReadme() {
        with(PetSpecs.Pip) {
            assertEquals(PipGreen, bodyColor)
            assertEquals(PipGreenEdge, bodyEdgeColor)
            assertEquals(Offset(150f, 74f), sweatAnchor)
            assertEquals(Offset(150f, 40f), sleepZAnchor)
            assertEquals(3, silhouetteOvals.size) // the feather tuft
        }
        with(PetSpecs.Toast) {
            assertEquals(ToastGold, bodyColor)
            assertEquals(ToastGoldEdge, bodyEdgeColor)
            assertEquals(Offset(140f, 64f), sweatAnchor)
            assertEquals(Offset(176f, 30f), sleepZAnchor)
            assertEquals(OvalSpec(28f, 118f, 9f, 18f, rotationDeg = -52f), armsUp.first()) // from the sides
        }
        with(PetSpecs.Nimbus) {
            assertEquals(NimbusLilac, bodyColor)
            assertEquals(NimbusLilacEdge, bodyEdgeColor)
            assertEquals(Offset(160f, 80f), sweatAnchor)
            assertEquals(Offset(150f, 32f), sleepZAnchor)
            assertEquals(3, silhouetteOvals.size) // three puffs unioned with the base
            assertEquals(86f, armsUp.first().cy)
        }
    }

    /** Pip's beak replaces the mouth; Toast's crust drains with the fill. Nobody else has either. */
    @Test
    fun beakAndCrustBelongToOnePetEach() {
        assertEquals(listOf(PetId.PIP), PetId.entries.filter { PetSpecs.of(it).beak != null })
        assertEquals(listOf(PetId.TOAST), PetId.entries.filter { PetSpecs.of(it).crust != null })
        assertEquals(PipBeak, PetSpecs.Pip.beak?.color)
        assertEquals(CrustSpec(width = 12f, color = ToastCrust, drainedColor = ToastCrustDrained), PetSpecs.Toast.crust)
    }

    /** Arm offsets only where the body hides Bun's arms (pet_all_poses_<pet>.png). */
    @Test
    fun poseArmOffsetsOnlyWhereNeeded() {
        assertTrue(bun.poseArmOffsets.isEmpty())
        assertTrue(PetSpecs.Pip.poseArmOffsets.isEmpty())
        assertEquals(setOf(ArmPose.Shrug), PetSpecs.Toast.poseArmOffsets.keys)
        assertEquals(setOf(ArmPose.Shrug), PetSpecs.Nimbus.poseArmOffsets.keys)
    }
}

