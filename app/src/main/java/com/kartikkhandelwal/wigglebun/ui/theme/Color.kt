package com.kartikkhandelwal.wigglebun.ui.theme

import androidx.compose.ui.graphics.Color

// Design tokens from CLAUDE.md §11. Screens must use these, never raw hex values.

// Core
val Cream = Color(0xFFFFF4E8)
val Card = Color(0xFFFFFFFF)
val CardEdge = Color(0xFFEFE3D4)
val Ink = Color(0xFF2B2A3A)
val InkEdge = Color(0xFF14131F)
val InkMuted = Color(0xFF6B6A7B)
val Track = Color(0xFFF1EBE3)
val DoneGreen = Color(0xFF3BAA6E)
val DoneGreenEdge = Color(0xFF2C8655)
val Sun = Color(0xFFFFC857)
val SunEdge = Color(0xFFD69E2A)
val Coral = Color(0xFFE5603A)
val Night = Color(0xFF2F2E45)
val BreakSky = Color(0xFFEAF3FD)

// Pet
val PetYellow = Color(0xFFF5DC8B)
val PetYellowEdge = Color(0xFFEBCB6A)
val PetGrey = Color(0xFFE9E5DE)
val PetPink = Color(0xFFF6A98A)
val PetBlush = Color(0xFFF7A58B)

// Collection pets (design/pets/README.md)
val PipGreen = Color(0xFF9EDCC3)
val PipGreenEdge = Color(0xFF7FC9AC)
val PipBeak = Color(0xFFF4A261)
val ToastGold = Color(0xFFEFC587)
val ToastGoldEdge = Color(0xFFDDAF6B)
val ToastCrust = Color(0xFFC98F4E)
val ToastCrustDrained = Color(0xFFD6CFC4)
val NimbusLilac = Color(0xFFC9B8F2)
val NimbusLilacEdge = Color(0xFFB39FE6)

// Accessories (design/accessories/acc_*.svg).
val AccessoryPetal = Color(0xFFF4A3C0)
val AccessoryLeaf = Color(0xFF6CC49F)
val AccessoryScarf = Color(0xFFE5603A)
val AccessoryScarfStripe = Color(0xFFFFF4E8)
val AccessoryBeanie = Color(0xFF7FB8F5)
val AccessoryBeanieBrim = Color(0xFF5E9EE3)
/** A pet not adopted yet, on a white Collection card (Collection.dc.html). */
val LockedSilhouette = Color(0xFFDCD5CB)

// Break types (icons, chips; the weekly chart uses one per category) and their light tints (icon tiles)
val BreakWalk = Color(0xFF7DD3B0)
val BreakNeck = Color(0xFF7FB8F5)
val BreakStand = Color(0xFFFFC857)
val BreakEyes = Color(0xFFB9A3F0)
val BreakWrist = Color(0xFFF68A6B)
val BreakWater = Color(0xFFF4A3C0)
val BreakWalkTint = Color(0xFFCDEFE0)
val BreakNeckTint = Color(0xFFCFE3FB)
val BreakStandTint = Color(0xFFFFE3A3)
val BreakEyesTint = Color(0xFFE6DDFB)
val BreakWristTint = Color(0xFFFDDCD2)
val BreakWaterTint = Color(0xFFFCE1EC)

// Mood chip backgrounds and timer bar colours
val MoodHappyChip = Color(0xFFDDF3E9)
val MoodTiredChip = Color(0xFFFFF0CC)
val MoodStiffChip = Color(0xFFFDDCD2)
val TimerHappy = Color(0xFF4DB88A)
val TimerTired = Color(0xFFF2A93B)
val TimerStiff = Color(0xFFE5603A)

// Mockup-only colours (design/mockups_source), named so screens never hard-code them
val ButtonBorder = Color(0xFFE8E0D5)
val ButtonEdge = Color(0xFFDDD3C6)
val CheckIdle = Color(0xFFB5ADA3)
val TopButtonEdge = Color(0xFFE8DCCD)
val TabBarBorder = Color(0xFFF3E9DD)
val TabActive = Color(0xFFFDE6CF)
val SparkEdge = Color(0xFFE5A52A)

// Home (Main.dc.html, FocusCard.dc.html)
val PetShadow = Color(0xFFE6D9C8)
/** Focused card overlay backdrop: rgba(33, 32, 48, 0.72). */
val Scrim = Color(0xB8212030)
val ActionEdge = Color(0xFFCFC6BA)
val CloseBorder = Color(0xFF8F8CA8)
val MovementChipText = Color(0xFF2B7A57)

// Break session (BreakSession.dc.html)
val SessionAccent = Color(0xFF4E8FD9)
val SessionTrack = Color(0xFFD5E5F7)
val SessionEdge = Color(0xFFCBDDF1)
val SessionSegmentIdle = Color(0xFFCFE0F3)
/** "Step 2 of 4" and the safety line: 5.6:1 on BreakSky. */
val SessionMuted = Color(0xFF5B6577)
