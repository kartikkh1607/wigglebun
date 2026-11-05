package com.kartikkhandelwal.wigglebun.ui.theme

import androidx.compose.ui.graphics.Color

// Design tokens from CLAUDE.md §11. Screens must use these, never raw hex values.

// Core
/** The window, the splash and the first-launch loading box; each screen paints its own surface. */
val Cream = Color(0xFFFFF4E8)
val Card = Color(0xFFFFFFFF)
/** The chunky edge on Cream (onboarding until its UI v2 restyle, inactive buttons). */
val CardEdge = Color(0xFFEFE3D4)
val Ink = Color(0xFF2B2A3A)
val InkEdge = Color(0xFF14131F)
/** Secondary text: 7.2:1 on Card, at least 5.8:1 on every screen surface below (lowest: HomeWall). */
val InkMuted = Color(0xFF56556A)
val Track = Color(0xFFF1EBE3)
val DoneGreen = Color(0xFF3BAA6E)
val DoneGreenEdge = Color(0xFF2C8655)
val Sun = Color(0xFFFFC857)
val SunEdge = Color(0xFFD69E2A)
val Coral = Color(0xFFE5603A)
val Night = Color(0xFF2F2E45)
val BreakSky = Color(0xFFEAF3FD)

// Screen surfaces (design/ui_v2/README.md). Each tab has its own background; the chunky edge
// under its cards is a darker shade of it (see Surfaces.kt).
val HomeWall = Color(0xFFD6EBDA)
val HomeWallLower = Color(0xFFC8E2CF)
/** Under things on the wall: the top buttons, the speech bubble, a panel's top edge. */
val HomeWallEdge = Color(0xFFBCDAC4)
val HomeFloor = Color(0xFFF4E8D6)
val HomeFloorSeam = Color(0xFFEADCC6)
val HomeFloorEdge = Color(0xFFE3D3BC)
val Rug = Color(0xFFCFE3FB)
val RugEdge = Color(0xFFB2D0F2)
val CollectionMint = Color(0xFFDDF1E6)
val CollectionEdge = Color(0xFFBFE0CE)
val SettingsLilac = Color(0xFFECE9F7)
val SettingsEdge = Color(0xFFD6D1EC)
/** The paywall's top and the Pet profile tab (#30 direction B). */
val Sky = Color(0xFFE2EEFB)
val SkyEdge = Color(0xFFC7DAF1)
/** Onboarding's night room (WakeUp.dc.html in design/ui_v2). */
val NightRoomWall = Color(0xFF2F3352)
val NightRoomFloor = Color(0xFF2A2C45)

// Tab bar (design/ui_v2 nav): the selected tab's pill, and the bar's top border on that tab
val TabHomeTint = Color(0xFFFFE3D8)
val TabCollectionTint = Color(0xFFD3F0E1)
val TabPetTint = Color(0xFFFFF0CC)
val TabSettingsTint = Color(0xFFE6DDFB)
val TabHomeBorder = Color(0xFFEFE6DA)
val TabCollectionBorder = Color(0xFFCFE8DA)
/** Not in ui_v2 (no Pet profile mockup): between Sky and SkyEdge, as the others are. */
val TabPetBorder = Color(0xFFD5E5F7)
val TabSettingsBorder = Color(0xFFDDD8EF)

// Shared pieces (design/ui_v2)
/** "On" / "Added" / "With you": 5.3:1. */
val StatusOnChip = Color(0xFFD3F0E1)
val StatusOnText = Color(0xFF1F6B48)
/** Between the rows of a grouped Settings card. */
val GroupDivider = Color(0xFFF1EEF8)
/** Settings' "Bun & you" card on [Night] (design/ui_v2/Settings.dc.html): edge, pet tile, sub line (9.6:1). */
val NightEdge = Color(0xFF1E1D30)
val NightTile = Color(0xFF45445F)
val NightSubtle = Color(0xFFD9D7EC)

/** Onboarding steps still to come. */
val StepDotIdle = Color(0xFFE3DCD2)

// Onboarding's night room (WakeUp.dc.html in design/ui_v2), besides NightRoomWall and NightRoomFloor
val NightRoomLowerWall = Color(0xFF2A2D4A)
val NightRoomTrim = Color(0xFF3D4166)
val NightRoomLedge = Color(0xFF45496F)
val NightRoomFloorLine = Color(0xFF323454)
val NightRoomWindowGlass = Color(0xFF1E2140)
val NightRoomMoon = Color(0xFFFFF3C9)
val NightRoomRugEdge = Color(0xFF3B4472)
val NightRoomRug = Color(0xFF48548A)
val NightRoomRugStitch = Color(0xFF6D79B0)
/** Text on the night room: the line under the title (9.2:1) and the Skip link. */
val NightBody = Color(0xFFCFCDE6)
val NightLink = Color(0xFFE4E1F0)

// Name screen (Name.dc.html): suggestion chips, the pet name field's edge
val NameChipYellow = Color(0xFFFFF0CC)
val NameChipMint = Color(0xFFDDF3E9)
val NameChipLilac = Color(0xFFE6DDFB)
val NameChipPink = Color(0xFFFCE1EC)
val NameFieldEdge = Color(0xFFE8E0D5)

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
val TimerHappy = Color(0xFF4DB88A)
val TimerTired = Color(0xFFF2A93B)
val TimerStiff = Color(0xFFE5603A)

// Mockup-only colours (design/mockups_source), named so screens never hard-code them
val ButtonBorder = Color(0xFFE8E0D5)
val ButtonEdge = Color(0xFFDDD3C6)
val CheckIdle = Color(0xFFB5ADA3)
val TopButtonEdge = Color(0xFFE8DCCD)
val SparkEdge = Color(0xFFE5A52A)

// Home (design/ui_v2/Main.dc.html, FocusCard.dc.html)
/** Behind a break card's spark reward. */
val RewardChip = Color(0xFFFFF4D6)
/** The streak's flame chip on the timer card: 7.8:1. */
val FlameChip = Color(0xFFFFE9DF)
val FlameChipText = Color(0xFF9A3D20)
/** A streak-goal segment not reached yet. */
val StreakSegmentIdle = Color(0xFFE9E1D6)
// Collection (design/ui_v2/Collection.dc.html)
/** The soft circles and dots behind the header. */
val CollectionDecor = Color(0xFFD1ECDD)
val CollectionDecorDot = CollectionEdge
/** Pets | Accessories switch: the track, the selected half's edge, an unselected label (5.4:1). */
val CollectionSwitchTrack = Color(0xFFC3E3D2)
val CollectionSwitchEdge = Color(0xFFA9D3BD)
val CollectionSwitchText = Color(0xFF2E5E48)
/** "Next pet" card: the pet's tile and the progress bar's track. */
val NextPetTile = Color(0xFFE6F6EE)
val NextPetTrack = Color(0xFFE6F1EA)
/** The warm glow behind the pet that is with you. */
val ActivePetHalo = Color(0xFFFFF6DA)

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
