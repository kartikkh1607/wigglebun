package com.kartikkhandelwal.wigglebun.ui.pet

import androidx.compose.ui.geometry.Offset
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.ui.theme.BreakEyes
import com.kartikkhandelwal.wigglebun.ui.theme.BreakNeck
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.PetBlush
import com.kartikkhandelwal.wigglebun.ui.theme.PetGrey
import com.kartikkhandelwal.wigglebun.ui.theme.PetPink
import com.kartikkhandelwal.wigglebun.ui.theme.PetYellow
import com.kartikkhandelwal.wigglebun.ui.theme.PetYellowEdge

object PetSpecs {
    /** Bun, the starter pet. Numbers are copied from design/bun_pet.svg (viewBox 0 0 200 210). */
    val Bun = PetSpec(
        id = "bun",
        defaultName = R.string.pet_default_name,
        viewBoxWidth = 200f,
        viewBoxHeight = 210f,
        bodyPath = "M100 46 C130 46 144 62 149 86 C153 106 166 124 166 148 " +
            "C166 174 140 190 100 190 C60 190 34 174 34 148 C34 124 47 106 51 86 " +
            "C56 62 70 46 100 46 Z",
        ears = listOf(OvalSpec(62f, 54f, 18f), OvalSpec(138f, 54f, 18f)),
        feet = listOf(OvalSpec(80f, 192f, 13f, 10f), OvalSpec(120f, 192f, 13f, 10f)),
        armsSide = listOf(
            OvalSpec(36f, 132f, 10f, 17f, rotationDeg = 28f),
            OvalSpec(164f, 132f, 10f, 17f, rotationDeg = -28f),
        ),
        armsUp = listOf(
            OvalSpec(44f, 76f, 10f, 19f, rotationDeg = -22f),
            OvalSpec(156f, 76f, 10f, 19f, rotationDeg = 22f),
        ),
        // From design/stand_up_frames.svg and more_stretch_frames.svg.
        poseArms = mapOf(
            ArmPose.Lap to listOf(
                OvalSpec(52f, 150f, 10f, 16f, rotationDeg = 50f),
                OvalSpec(148f, 150f, 10f, 16f, rotationDeg = -50f),
            ),
            ArmPose.Shrug to listOf(
                OvalSpec(44f, 104f, 10f, 18f, rotationDeg = -8f),
                OvalSpec(156f, 104f, 10f, 18f, rotationDeg = 8f),
            ),
            ArmPose.RollBack to listOf(
                OvalSpec(44f, 126f, 8f, 14f, rotationDeg = 40f),
                OvalSpec(156f, 126f, 8f, 14f, rotationDeg = -40f),
            ),
            ArmPose.RollDown to listOf(
                OvalSpec(38f, 140f, 10f, 17f, rotationDeg = 18f),
                OvalSpec(162f, 140f, 10f, 17f, rotationDeg = -18f),
            ),
            ArmPose.ReachRight to listOf(
                OvalSpec(36f, 132f, 10f, 17f, rotationDeg = 28f),
                OvalSpec(180f, 126f, 24f, 10f),
                OvalSpec(203f, 114f, 8f, 12f),
            ),
            ArmPose.ReachLeft to listOf(
                OvalSpec(164f, 132f, 10f, 17f, rotationDeg = -28f),
                OvalSpec(20f, 126f, 24f, 10f),
                OvalSpec(-3f, 114f, 8f, 12f),
            ),
            ArmPose.MarchLeft to listOf(
                OvalSpec(38f, 124f, 10f, 17f, rotationDeg = 10f),
                OvalSpec(164f, 132f, 10f, 17f, rotationDeg = -28f),
            ),
            ArmPose.MarchRight to listOf(
                OvalSpec(36f, 132f, 10f, 17f, rotationDeg = 28f),
                OvalSpec(162f, 124f, 10f, 17f, rotationDeg = -10f),
            ),
        ),
        poseFeet = mapOf(
            FeetPose.Sit to listOf(OvalSpec(74f, 194f, 15f, 9f), OvalSpec(126f, 194f, 15f, 9f)),
            FeetPose.LiftLeft to listOf(OvalSpec(80f, 180f, 12f, 9f), OvalSpec(120f, 192f, 13f, 10f)),
            FeetPose.LiftRight to listOf(OvalSpec(80f, 192f, 13f, 10f), OvalSpec(120f, 180f, 12f, 9f)),
        ),
        earsInner = listOf(OvalSpec(62f, 55f, 9f), OvalSpec(138f, 55f, 9f)),
        cheeks = listOf(OvalSpec(72f, 118f, 9f, 5.5f), OvalSpec(128f, 118f, 9f, 5.5f)),
        bodyColor = PetYellow,
        bodyEdgeColor = PetYellowEdge,
        drainedColor = PetGrey,
        earInnerColor = PetPink,
        cheekColor = PetBlush,
        featureColor = Ink,
        sweatColor = BreakNeck,
        sleepZColor = BreakEyes,
        fillTopFull = 36f,
        fillRange = 160f,
        fillEdgeThickness = 4f,
        feetColoredAbove = 185f,
        armsColoredAbove = 128f,
        eyeLeft = Offset(84f, 106f),
        eyeRight = Offset(116f, 106f),
        eyeDotRadius = 5.5f,
        mouthAnchor = Offset(100f, 114f),
        pivot = Offset(100f, 192f),
        torsoPivot = Offset(100f, 172f), // neck_stretch_frames.svg: rotate(±20 100 172)
        groundY = 202f,
        sweatAnchor = Offset(150f, 70f),
        sleepZAnchor = Offset(150f, 40f),
        sleepZAnchorLeft = Offset(39f, 46f),
    )
}
