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
import com.kartikkhandelwal.wigglebun.ui.theme.ToastCrust
import com.kartikkhandelwal.wigglebun.ui.theme.ToastCrustDrained
import com.kartikkhandelwal.wigglebun.ui.theme.ToastGold
import com.kartikkhandelwal.wigglebun.ui.theme.ToastGoldEdge

/**
 * Toast the slice of bread. Numbers copied from design/pets/toast/toast_pet.svg and
 * toast_<mood>.svg. The crust is the silhouette stroked 12 wide and clipped inside it. The wide
 * dome hides Bun's raised-arm spot, so Toast's own arms_up come out from the sides (y 118).
 */
internal val ToastSpec = PetSpec(
    id = PetId.TOAST,
    defaultName = R.string.pet_name_toast,
    blurb = R.string.pet_blurb_toast,
    viewBoxWidth = 200f,
    viewBoxHeight = 210f,
    bodyPath = "M42 104 L42 174 Q42 188 56 188 L144 188 Q158 188 158 174 L158 104 " +
        "C176 98 182 78 170 60 C158 44 132 38 100 38 C68 38 42 44 30 60 C18 78 24 98 42 104 Z",
    silhouetteOvals = emptyList(),
    feet = listOf(OvalSpec(80f, 192f, 13f, 10f), OvalSpec(120f, 192f, 13f, 10f)),
    armsSide = listOf(
        OvalSpec(36f, 132f, 10f, 17f, rotationDeg = 28f),
        OvalSpec(164f, 132f, 10f, 17f, rotationDeg = -28f),
    ),
    armsUp = listOf(
        OvalSpec(28f, 118f, 9f, 18f, rotationDeg = -52f),
        OvalSpec(172f, 118f, 9f, 18f, rotationDeg = 52f),
    ),
    // The dome hides Bun's shrug (arms at y 104): raise them beside the body, just under the dome.
    poseArmOffsets = mapOf(ArmPose.Shrug to Offset(-8f, 14f)),
    earsInner = emptyList(),
    cheeks = listOf(OvalSpec(72f, 118f, 9f, 5.5f), OvalSpec(128f, 118f, 9f, 5.5f)),
    crust = CrustSpec(width = 12f, color = ToastCrust, drainedColor = ToastCrustDrained),
    bodyColor = ToastGold,
    bodyEdgeColor = ToastGoldEdge,
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
    sweatAnchor = Offset(140f, 64f),
    sleepZAnchor = Offset(176f, 30f),
    sleepZStep = Offset(16f, -18f),
    // Bun's rule: the mirror of the right anchor, 11 further out and 6 lower.
    sleepZAnchorLeft = Offset(13f, 36f),
    // HEAD on the top of the crust, which is wider than Bun's head.
    accessoryAnchors = AccessorySpecs.DefaultAnchors + (AccessorySlot.HEAD to AccessoryAnchor(Offset(100f, 40f), scale = 1.2f)),
)
