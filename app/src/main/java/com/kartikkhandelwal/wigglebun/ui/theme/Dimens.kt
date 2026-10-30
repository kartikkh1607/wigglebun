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

        // Large font sizes (WidgetTextFit): the text keeps the user's size, so the pet and the
        // spacing give way. 130%: smaller pet; 200%: no pet, tighter padding.
        val MediumPetWidthLargeText = 56.dp
        val ColumnGapLargeText = 2.dp
        val PaddingHugeText = 8.dp

        // Glance can't use bundled fonts, so the system font at the app's type sizes.
        val TitleSize = 20.sp
        val LabelSize = 14.sp
        val ButtonTextSize = 14.sp
    }

    /** Onboarding, measured from design/mockups_source/WakeUp.dc.html (390 x 844). */
    object Onboarding {
        /** Below this height the mockup's big gaps give way, so the button and dots stay on screen. */
        val TallScreen = 780.dp
        val SkipRowTop = 22.dp
        val SkipRowHorizontal = 24.dp
        val SkipPaddingVertical = 10.dp
        val SkipPaddingHorizontal = 4.dp
        val SkipMinSize = 48.dp
        val TitleTop = 70.dp
        val TitleTopShort = 12.dp
        val TitleSize = 34.sp
        val TextHorizontal = 32.dp
        val TextGap = 10.dp
        val BodySize = 17.sp
        val BodyLineHeight = 25.sp
        val RingTop = 60.dp
        val RingTopShort = 20.dp
        val RingBox = 240.dp
        /** Share of the screen height the ring may use on a short screen. */
        const val RingBoxShortFraction = 0.3f
        val RingStroke = 3.dp
        val RingDash = 9.dp
        /** Pet and dashed ring, as fractions of [RingBox]. */
        const val PetInRing = 210f / 240f
        const val DashedRingInBox = 236f / 240f
        /** The floor ellipse under the pet, as fractions of [RingBox]. */
        const val FloorCentreY = 283f / 240f
        const val FloorRadiusX = 150f / 240f
        const val FloorRadiusY = 26f / 240f
        val CtaMinWidth = 280.dp
        val CtaMinHeight = 61.dp
        val CtaIcon = 22.dp
        val CtaIconGap = 10.dp
        val CtaBottom = 107.dp
        val CtaBottomShort = 16.dp
        val DotsBottom = 40.dp
        val DotsBottomShort = 16.dp
        val DotGap = 8.dp
        val DotSize = 8.dp
        val DotActiveWidth = 22.dp

        // Naming (no mockup: sized from the wake-up screen and the card tokens)
        val NamePetSize = 170.dp
        val NamePetTop = 8.dp
        val NameTextTop = 16.dp
        val NameFieldTop = 24.dp
        val NameFieldHorizontal = 24.dp
        val NameFieldPadding = 16.dp
        val NameCounterGap = 10.dp
        val NameButtonHorizontal = 24.dp

        // Work hours and interval (no mockup)
        val QuestionTop = 8.dp
        val QuestionGap = 12.dp
        val QuestionAnswerTop = 4.dp
        val DayChip = 40.dp
        val DayChipTouch = 48.dp
        val IntervalPetSize = 170.dp
        val SheetHorizontal = 24.dp
        val SheetBottom = 24.dp
        val SheetGap = 16.dp

        // First stretch and the paywall placeholder (no mockup)
        val StretchPetSize = 220.dp
        val StretchButtonBottom = 24.dp
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

    /** Collection tab, measured from design/mockups_source/Collection.dc.html. */
    object Collection {
        val HeaderPaddingTop = 22.dp
        val HeaderPaddingHorizontal = 20.dp
        val HeaderPaddingBottom = 4.dp
        val SubtitlePaddingBottom = 16.dp
        val GridGap = 12.dp
        val CardPaddingTop = 10.dp
        val CardPaddingHorizontal = 10.dp
        val CardPaddingBottom = 14.dp
        val CardGap = 6.dp
        val CardEdge = 4.dp
        val ActiveBorder = 3.dp
        val PetSize = 112.dp
        val PillPaddingHorizontal = 12.dp
        val PillPaddingVertical = 4.dp
        val PillIcon = 14.dp
        val PillIconGap = 4.dp
        val ListBottomPadding = 24.dp

        // Pet sheet (no mockup: sheet radius from CLAUDE.md §11, buttons as elsewhere)
        val SheetPaddingHorizontal = 24.dp
        val SheetPaddingBottom = 24.dp
        val SheetGap = 8.dp
        val SheetPetSize = 168.dp
        val SheetSmallPetSize = 120.dp
        val SheetButtonTop = 12.dp
        val SheetButtonIcon = 20.dp
        val SheetButtonIconGap = 6.dp

        // Pets | Accessories switch (segmented pill, CLAUDE.md §11 pills fully rounded)
        val TabsPaddingBottom = 14.dp
        val TabsInnerPadding = 4.dp
        val TabPaddingVertical = 8.dp
    }

    /** Pet profile tab (no mockup: Home's card style, CLAUDE.md §6.6). */
    object Profile {
        val TopPadding = 16.dp
        val PetSize = 180.dp
        val PetShadowWidth = 112.dp
        val PetShadowHeight = 14.dp
        /** The shadow sits under the feet: the viewBox ground (y ≈ 200 of 210) at 180 dp. */
        val PetShadowOffset = 168.dp
        val NameTop = 4.dp
        val CardsTop = 16.dp
        val CardGap = 12.dp
        val CardPadding = 16.dp
        val CardEdge = 4.dp
        val CardLabelGap = 4.dp
        val ProgressTop = 10.dp
        val ProgressHeight = 8.dp
        val ChartTop = 12.dp
        /** Tallest bar; the day's total sits above it. */
        val BarMaxHeight = 104.dp
        /** Bars stay thinner than 24 dp and get a 4 dp rounded tip (dataviz mark specs). */
        val BarWidth = 22.dp
        val BarRadius = 4.dp
        /** Surface-coloured gap between stacked segments, so neighbours never blur together. */
        val BarGapInStack = 2.dp
        val EmptyBarHeight = 4.dp
        val BarLabelGap = 6.dp
        val LegendTop = 12.dp
        val LegendGap = 12.dp
        val LegendDot = 10.dp
        val LegendDotGap = 6.dp
        val ListBottomPadding = 24.dp
    }
}
