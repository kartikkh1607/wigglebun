package com.kartikkhandelwal.wigglebun.ui.pet

import com.kartikkhandelwal.wigglebun.domain.model.Mood
import com.kartikkhandelwal.wigglebun.ui.theme.PetGrey
import com.kartikkhandelwal.wigglebun.ui.theme.PetYellow
import org.junit.Assert.assertEquals
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
}
