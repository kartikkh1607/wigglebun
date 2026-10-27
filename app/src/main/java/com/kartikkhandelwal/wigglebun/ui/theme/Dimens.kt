package com.kartikkhandelwal.wigglebun.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Dimens {
    val ScreenPadding = 16.dp
    val CardPadding = 16.dp
    val ListGap = 10.dp

    // Chunky "edge" = solid offset under cards and buttons
    val CardEdge = 4.dp
    val ButtonEdge = 5.dp
    val SecondaryButtonEdge = 4.dp
    val ButtonBorder = 2.dp

    val ButtonHeight = 54.dp
    val ButtonHorizontalPadding = 26.dp
    val IconButtonSize = 54.dp
    val IconButtonIcon = 26.dp

    val TabBarHeight = 84.dp
    val TabBarBorder = 2.dp
    val TabBarPaddingHorizontal = 8.dp
    val TabBarPaddingTop = 8.dp
    val TabBarPaddingBottom = 14.dp
    val TabIndicatorWidth = 56.dp
    val TabIndicatorHeight = 34.dp
    val TabIcon = 22.dp
    val TabPetIcon = 24.dp
    val TabLabelGap = 4.dp

    /** Home-screen widget (Glance). No mockup: sized from the tokens above. Card radius is in widget_background.xml. */
    object Widget {
        val SmallMinWidth = 110.dp
        val MediumMinWidth = 250.dp
        val MinHeight = 110.dp
        val Padding = 12.dp
        val MediumPetWidth = 96.dp
        val Gap = 10.dp
        // Medium text column must fit 110dp minus padding: title + label + gap + button.
        val ColumnGap = 6.dp
        val ButtonHeight = 32.dp
        val ButtonHorizontalPadding = 14.dp

        // Glance can't use bundled fonts, so the system font at the app's type sizes.
        val TitleSize = 20.sp
        val LabelSize = 14.sp
        val ButtonTextSize = 14.sp
    }

    /** Home screen, measured from design/mockups_source/Main.dc.html and FocusCard.dc.html. */
    object Home {
        val SceneHeight = 340.dp
        val PetTop = 116.dp
        val PetSize = 190.dp
        val PetShadowTop = 284.dp
        val PetShadowWidth = 120.dp
        val PetShadowHeight = 16.dp
        val TopBarPadding = 16.dp
        val MenuButtonSize = 48.dp
        val MenuButtonRadius = 16.dp
        val MenuIcon = 22.dp
        val TopButtonEdge = 4.dp
        val SparksHeight = 40.dp
        val SparksPaddingStart = 10.dp
        val SparksPaddingEnd = 14.dp
        val SparksGap = 6.dp
        val SparksIcon = 20.dp

        val PillOverlap = 36.dp
        val PillPaddingHorizontal = 18.dp
        val PillPaddingVertical = 16.dp
        val PillGap = 10.dp
        val PillLabelGap = 2.dp
        val ChipPaddingHorizontal = 12.dp
        val ChipPaddingVertical = 6.dp
        val ProgressHeight = 10.dp
        val CtaTopGap = 4.dp

        val HeaderPaddingHorizontal = 20.dp
        val HeaderPaddingTop = 20.dp
        val HeaderPaddingBottom = 10.dp

        val CardEdge = 3.dp
        val CardPaddingStart = 14.dp
        val CardPadding = 12.dp
        val CardGap = 12.dp
        val CardTextGap = 2.dp
        val IconTileSize = 48.dp
        val BreakIcon = 26.dp
        val RewardGap = 3.dp
        val RewardIcon = 16.dp
        val CheckWidth = 52.dp
        val CheckHeight = 48.dp
        val CheckEdge = 4.dp
        val CheckIcon = 24.dp
        val ListBottomPadding = 24.dp
        val StaggerSlide = 12.dp

        val FocusMargin = 24.dp
        val FocusPaddingTop = 26.dp
        val FocusPaddingHorizontal = 22.dp
        val FocusPaddingBottom = 24.dp
        val FocusGap = 10.dp
        val FocusRewardInset = 18.dp
        val FocusTileSize = 72.dp
        val FocusTileRadius = 24.dp
        val FocusIcon = 38.dp
        val FocusChipPaddingHorizontal = 12.dp
        val FocusChipPaddingVertical = 4.dp
        /** Card bottom (≈ 482) to the Done face top (520) in FocusCard.dc.html. */
        val FocusToActionsGap = 38.dp
        val ActionsPaddingHorizontal = 28.dp
        val ActionLabelGap = 10.dp
        val ActionSize = 64.dp
        val ActionRadius = 22.dp
        val ActionEdge = 5.dp
        val ActionIcon = 28.dp
        val DoneActionSize = 88.dp
        val DoneActionRadius = 30.dp
        val DoneActionEdge = 6.dp
        val DoneActionIcon = 44.dp
        /** Every action button sits at the bottom of a slot as tall as Done (face + edge). */
        val ActionSlotHeight = DoneActionSize + DoneActionEdge
        val ActionsToCloseGap = 30.dp
        val CloseSize = 56.dp
        val CloseBorder = 2.dp
        val CloseIcon = 22.dp
        val CloseBottomMargin = 120.dp
        /** Short screens / large fonts: top and bottom margins shrink to this before anything scrolls. */
        val OverlayMinMargin = 24.dp
    }

    /** Break session + celebration, measured from design/mockups_source/BreakSession.dc.html. */
    object BreakSession {
        val TopPadding = 16.dp
        val CloseSize = 48.dp
        val CloseEdge = 4.dp
        val CloseIcon = 20.dp
        val SegmentsTop = 14.dp
        val SegmentsHorizontal = 24.dp
        val SegmentGap = 6.dp
        val SegmentHeight = 6.dp
        val RingTop = 34.dp
        val RingSize = 280.dp
        val RingStroke = 12.dp
        val TimerTop = 14.dp
        val CardTop = 14.dp
        val CardHorizontal = 20.dp
        val CardPaddingVertical = 18.dp
        val CardPaddingHorizontal = 20.dp
        val CardEdge = 4.dp
        val SafetyTop = 10.dp
        val ButtonsTop = 16.dp
        val ButtonsHorizontal = 20.dp
        val ButtonsBottom = 34.dp
        val ButtonsGap = 12.dp
        val ButtonHeight = 56.dp
        val ButtonPaddingHorizontal = 12.dp

        val CelebratePetSize = 210.dp
        val CelebrateGap = 10.dp
        val BadgePaddingStart = 14.dp
        val BadgePaddingEnd = 18.dp
        val BadgePaddingVertical = 8.dp
        val BadgeGap = 6.dp
        val BadgeIcon = 22.dp
        val BadgeEdge = 4.dp
        val StreakCardTop = 8.dp
        val StreakCardPadding = 16.dp
    }
}
