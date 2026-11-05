package com.kartikkhandelwal.wigglebun.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kartikkhandelwal.wigglebun.R

// Both fonts are bundled from Google Fonts (SIL OFL 1.1, licences in assets/licenses).
val Fredoka = FontFamily(
    Font(R.font.fredoka_semibold, FontWeight.SemiBold),
)

val Nunito = FontFamily(
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
)

val WigglebunTypography = Typography(
    // Fredoka SemiBold: display 32, title 22, button 18
    displayMedium = TextStyle(
        fontFamily = Fredoka,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 1.1.em,
    ),
    titleLarge = TextStyle(
        fontFamily = Fredoka,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Fredoka,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 22.sp,
    ),
    // Nunito: card title ExtraBold 17, body SemiBold 15, labels Bold 13–15
    // Fredoka SemiBold 28: focused card title
    headlineMedium = TextStyle(
        fontFamily = Fredoka,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Nunito,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Nunito,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    // Rewards ("+5"), overlay button labels
    titleSmall = TextStyle(
        fontFamily = Nunito,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    // Break card sub line ("2 min")
    bodySmall = TextStyle(
        fontFamily = Nunito,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 19.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Nunito,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Nunito,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    // Tab bar labels
    labelSmall = TextStyle(
        fontFamily = Nunito,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)

/** Styles from the mockups that don't map onto a Material slot. */
object WigglebunTextStyles {
    /** Mood chip on the timer pill (13 sp, 800). */
    val Chip = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, lineHeight = 17.sp)

    /** Sparks counter (16 sp, 800). */
    val Sparks = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, lineHeight = 20.sp)

    /** "2 of 7 done" (14 sp, 700). */
    val Count = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 19.sp)

    /** Focused card sub line (15 sp, 700). */
    val FocusSub = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp)

    /** Break session title (Fredoka 20). */
    val SessionTitle = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp)

    /** Break session countdown "0:11" (Fredoka 44). */
    val Countdown = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 44.sp, lineHeight = 52.sp)

    /** Session button labels shrink to at most this (from 18) rather than being cut off. */
    val SessionButtonMinFontSize = 13.sp

    /** Pet profile screen title (design/mockups_source/Collection.dc.html: Fredoka 30); ui_v2 tabs use [TabScreenTitle]. */
    val ScreenTitle = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 36.sp)

    /** Pet name on a Collection card (design/ui_v2/Collection.dc.html: Fredoka 20). */
    val PetCardName = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 25.sp)

    /** A ui_v2 tab's title, "Collection" (design/ui_v2: Fredoka 34). */
    val TabScreenTitle = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 40.sp)

    /** A small card's title, "Pip is almost yours" (design/ui_v2: Nunito 16, 800). */
    val SmallCardTitle = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, lineHeight = 21.sp)

    /** A quiet line under a progress bar, "About 3 more days of breaks" (design/ui_v2: Nunito 13, 600). */
    val Hint = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 17.sp)

    /** Big numbers on the Pet profile cards ("3 days", "128"). */
    val StatNumber = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp)

    /** Break session instruction (Nunito 18, 700, line height 1.45). */
    val Instruction = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 26.sp)

    /** The pet's speech bubble (design/ui_v2/Main.dc.html: Nunito 14, 800, line height 1.3). */
    val Bubble = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, lineHeight = 18.sp)

    /** Short bold labels (design/ui_v2: Nunito 14, 800): "Streak goal", a card's spark reward. */
    val StrongLabel = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, lineHeight = 19.sp)

    /** "On" / "Added" status chip (design/ui_v2/Settings.dc.html). */
    val Status = StrongLabel

    /** The timer card's "32 min" (design/ui_v2/Main.dc.html: Fredoka 34, line height 1.1). */
    val TimerMinutes = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 37.sp)
}
