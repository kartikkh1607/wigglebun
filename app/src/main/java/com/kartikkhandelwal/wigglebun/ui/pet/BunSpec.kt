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
        groundY = 202f,
        sweatAnchor = Offset(150f, 70f),
        sleepZAnchor = Offset(150f, 40f),
    )
}
