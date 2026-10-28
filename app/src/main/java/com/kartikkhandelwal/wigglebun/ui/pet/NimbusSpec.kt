package com.kartikkhandelwal.wigglebun.ui.pet

import androidx.compose.ui.geometry.Offset
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.domain.model.PetId
import com.kartikkhandelwal.wigglebun.ui.theme.BreakEyes
import com.kartikkhandelwal.wigglebun.ui.theme.BreakNeck
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.NimbusLilac
import com.kartikkhandelwal.wigglebun.ui.theme.NimbusLilacEdge
import com.kartikkhandelwal.wigglebun.ui.theme.PetBlush
import com.kartikkhandelwal.wigglebun.ui.theme.PetGrey
import com.kartikkhandelwal.wigglebun.ui.theme.PetPink

/**
 * Nimbus the cloud. Numbers copied from design/pets/nimbus/nimbus_pet.svg and nimbus_<mood>.svg.
 * The silhouette is the base plus three circles (unioned); arms_up sit lower and wider (y 86).
 */
internal val NimbusSpec = PetSpec(
    id = PetId.NIMBUS,
    defaultName = R.string.pet_name_nimbus,
    blurb = R.string.pet_blurb_nimbus,
    viewBoxWidth = 200f,
    viewBoxHeight = 210f,
    bodyPath = "M38 142 C38 110 62 98 100 98 C138 98 162 110 162 142 C162 176 140 190 100 190 " +
        "C60 190 38 176 38 142 Z",
    silhouetteOvals = listOf(OvalSpec(63f, 104f, 26f), OvalSpec(100f, 74f, 38f), OvalSpec(137f, 104f, 26f)),
    feet = listOf(OvalSpec(80f, 192f, 13f, 10f), OvalSpec(120f, 192f, 13f, 10f)),
    armsSide = listOf(
        OvalSpec(36f, 132f, 10f, 17f, rotationDeg = 28f),
        OvalSpec(164f, 132f, 10f, 17f, rotationDeg = -28f),
    ),
    armsUp = listOf(
        OvalSpec(34f, 86f, 10f, 19f, rotationDeg = -30f),
        OvalSpec(166f, 86f, 10f, 19f, rotationDeg = 30f),
    ),
    // The side puffs hide Bun's shrug: move the arms out to stick out past the puffs as on Bun.
    poseArmOffsets = mapOf(ArmPose.Shrug to Offset(-10f, 0f)),
    earsInner = emptyList(),
    cheeks = listOf(OvalSpec(72f, 118f, 9f, 5.5f), OvalSpec(128f, 118f, 9f, 5.5f)),
    bodyColor = NimbusLilac,
    bodyEdgeColor = NimbusLilacEdge,
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
    torsoPivot = Offset(100f, 172f),
    groundY = 202f,
    sweatAnchor = Offset(160f, 80f),
    sleepZAnchor = Offset(150f, 32f),
    sleepZStep = Offset(16f, -18f),
    // Bun's rule: the mirror of the right anchor, 11 further out and 6 lower.
    sleepZAnchorLeft = Offset(39f, 38f),
)
