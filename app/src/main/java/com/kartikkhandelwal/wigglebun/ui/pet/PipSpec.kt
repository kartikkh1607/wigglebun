package com.kartikkhandelwal.wigglebun.ui.pet

import androidx.compose.ui.geometry.Offset
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.domain.model.AccessorySlot
import com.kartikkhandelwal.wigglebun.domain.model.PetId
import com.kartikkhandelwal.wigglebun.ui.theme.BreakEyes
import com.kartikkhandelwal.wigglebun.ui.theme.BreakNeck
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.PetBlush
import com.kartikkhandelwal.wigglebun.ui.theme.PetGrey
import com.kartikkhandelwal.wigglebun.ui.theme.PetPink
import com.kartikkhandelwal.wigglebun.ui.theme.PipBeak
import com.kartikkhandelwal.wigglebun.ui.theme.PipGreen
import com.kartikkhandelwal.wigglebun.ui.theme.PipGreenEdge

/**
 * Pip the bird. Numbers copied from design/pets/pip/pip_pet.svg and pip_<mood>.svg. The feather
 * tuft is part of the silhouette; a beak replaces the mouth (open only when celebrating).
 */
internal val PipSpec = PetSpec(
    id = PetId.PIP,
    defaultName = R.string.pet_name_pip,
    blurb = R.string.pet_blurb_pip,
    viewBoxWidth = 200f,
    viewBoxHeight = 210f,
    bodyPath = "M100 54 C123 54 137 72 145 94 C155 117 166 133 166 153 " +
        "C166 178 140 190 100 190 C60 190 34 178 34 153 C34 133 45 117 55 94 " +
        "C63 72 77 54 100 54 Z",
    silhouetteOvals = listOf(
        OvalSpec(100f, 47f, 7.5f, 11f),
        OvalSpec(88f, 52f, 6.5f, 10f, rotationDeg = -32f),
        OvalSpec(112f, 52f, 6.5f, 10f, rotationDeg = 32f),
    ),
    feet = listOf(OvalSpec(80f, 192f, 13f, 10f), OvalSpec(120f, 192f, 13f, 10f)),
    // Wings.
    armsSide = listOf(
        OvalSpec(37f, 134f, 9f, 19f, rotationDeg = 38f),
        OvalSpec(163f, 134f, 9f, 19f, rotationDeg = -38f),
    ),
    armsUp = listOf(
        OvalSpec(46f, 80f, 9f, 20f, rotationDeg = -28f),
        OvalSpec(154f, 80f, 9f, 20f, rotationDeg = 28f),
    ),
    earsInner = emptyList(),
    cheeks = listOf(OvalSpec(72f, 118f, 9f, 5.5f), OvalSpec(128f, 118f, 9f, 5.5f)),
    beak = BeakSpec(
        closedPath = "M92 112 Q100 107 108 112 Q100 123 92 112 Z",
        openPath = "M91 110 Q100 104 109 110 Q100 114 91 110 Z M94 115 Q100 113 106 115 Q100 125 94 115 Z",
        color = PipBeak,
    ),
    bodyColor = PipGreen,
    bodyEdgeColor = PipGreenEdge,
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
    sweatAnchor = Offset(150f, 74f),
    sleepZAnchor = Offset(150f, 40f),
    sleepZStep = Offset(16f, -18f),
    // Bun's rule: the mirror of the right anchor, 11 further out and 6 lower.
    sleepZAnchorLeft = Offset(39f, 46f),
    // HEAD on top of the head, under the feather tuft (which a hat covers).
    accessoryAnchors = AccessorySpecs.DefaultAnchors + (AccessorySlot.HEAD to AccessoryAnchor(Offset(100f, 54f), scale = 0.95f)),
)
