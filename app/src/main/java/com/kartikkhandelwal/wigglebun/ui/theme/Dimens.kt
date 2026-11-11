package com.kartikkhandelwal.wigglebun.ui.theme

import androidx.compose.ui.unit.Dp
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
    val TabIndicatorWidth = 60.dp
    val TabIndicatorHeight = 36.dp
    // design/ui_v2 nav: each icon at its drawn size; the pet is a small PetView.
    val TabHomeIcon = 26.dp
    val TabIcon = 24.dp
    val TabPetIcon = 28.dp
    val TabLabelGap = 3.dp

    /** Pieces several design/ui_v2 screens share (ui/components). */
    object Shared {
        // Back / Close / Menu on a tinted background (Hours.dc.html)
        val TopButtonSize = 44.dp
        val TopButtonEdge = 3.dp
        val TopButtonIcon = 20.dp

        val SkipPaddingHorizontal = 14.dp
        val SkipPaddingVertical = 7.dp
        val SkipEdge = 3.dp

        // Onboarding progress (StepDots)
        val DotGap = 6.dp
        val DotSize = 8.dp
        val DotCurrentWidth = 28.dp

        // White panel rising from the bottom (Name / Hours / Interval .dc.html)
        val PanelEdge = 3.dp
        val PanelPaddingTop = 22.dp
        val PanelPaddingHorizontal = 24.dp
        val PanelGap = 14.dp

        // Grouped Settings card and its rows (Settings.dc.html)
        val DividerThickness = 1.5.dp
        val RowPaddingHorizontal = 16.dp
        /** Above a row's button or value when a very large font moves it under the text. */
        val RowStackedGap = 6.dp
        val RowPaddingVertical = 12.dp
        val RowPaddingVerticalSingle = 10.dp
        val RowMinHeight = 66.dp
        val RowMinHeightSingle = 60.dp
        val RowGap = 14.dp
        val RowTextGap = 1.dp
        val RowTile = 44.dp
        val RowIcon = 32.dp
        val StatusPaddingHorizontal = 12.dp
        val StatusPaddingVertical = 4.dp

        // Speech bubble (Main.dc.html)
        val BubblePaddingHorizontal = 14.dp
        val BubblePaddingVertical = 9.dp
        val BubbleEdge = 3.dp
        val BubbleTailWidth = 18.dp
        val BubbleTailHeight = 10.dp
        /** The tail tucks 1 dp under the bubble, so no seam shows between them. */
        val BubbleTailOverlap = 1.dp
    }

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

    /** Onboarding, measured from design/ui_v2 (WakeUp.dc.html, Name.dc.html: 390 x 844). */
    object Onboarding {
        /**
         * The top bar every onboarding screen shares: Back (if any) and Skip on one centre line.
         * Back is 44 dp inside a 48 dp touch target, so 16 dp puts its face 18 dp from the edge;
         * the Skip pill has no such margin and gets the 18 dp directly.
         */
        val TopBarTop = 14.dp
        val TopBarStart = 16.dp
        val TopBarEnd = 18.dp
        /** Height of the bar's buttons at normal font size (their touch target). */
        val TopBarButton = 48.dp
        val TextHorizontal = 32.dp
        val TextGap = 10.dp
        val CtaBottomShort = 16.dp
        val DotsBottomShort = 16.dp

        // Wake-up: the artboard's positions, which a short phone or a big font squeezes
        const val WakeMinSqueeze = 0.4f
        /** The title and body at normal font size, with a margin: what the scene must leave room for. */
        val WakeTextHeight = 140.dp
        /** Extra text height per unit of font scale above 1, for the lines that wrapping adds. */
        const val WakeTextWrapGrowth = 0.3f
        /** Space above and below the button on a short phone. */
        val WakeCtaPaddingShort = 16.dp
        val WakeBlobTop = 318.dp
        val WakeBlobLeft = 105.dp
        val WakeBlobWidth = 180.dp
        val WakeBlobHeight = 153.dp
        val WakeZLeft = 270.dp
        val WakeZTop = 300.dp
        val WakeZSize = 30.sp
        val WakeZSmallSize = 22.sp
        val WakeZSmallOffsetX = 3.dp
        val WakeZSmallOffsetY = (-20).dp
        val WakeTitleTop = 556.dp
        val WakeTextGap = 12.dp
        val WakeTextHorizontal = 28.dp
        val WakeTitleSize = 32.sp
        val WakeBodySize = 16.sp
        val WakeBodyLineHeight = 23.sp
        val WakeCtaHorizontal = 24.dp
        val WakeCtaBottom = 44.dp
        val WakeCtaHeight = 58.dp
        val WakeCtaIcon = 22.dp
        val WakeCtaIconGap = 10.dp

        // Naming: the day room on top, the white panel from 380 down
        val NameRoomHeight = 340.dp
        val NamePanelTop = 380.dp
        /** Above 115% the room shrinks and the button moves into the panel; from 160% the bubble keeps one line. */
        const val NameLargeFontScale = 1.15f
        const val NameHugeFontScale = 1.6f
        const val NameMinRoomScale = 0.5f
        val NamePetSize = 170.dp
        val NameBubbleTop = 62.dp
        /** When the room shrinks the bubble would reach up under the Skip pill, so it starts below it. */
        val NameBubbleTopBelowSkip = 78.dp
        val NameBubbleWidth = 250.dp
        val NameBubbleTitleSize = 16.sp
        /** Between the bubble's tail and the pet's ears. */
        val NameBubbleToPet = 7.dp
        /** The pet's feet stand this far above the room's bottom edge, on the rug. */
        val NamePetFloorGap = 23.dp
        val NameTextGap = 6.dp
        val NameCounterGap = 10.dp
        val NameTitleSize = 29.sp
        val NameFieldHeight = 58.dp
        val NameFieldBorder = 2.5.dp
        val NameFieldEdge = 4.dp
        val NameFieldPaddingHorizontal = 16.dp
        val NameFieldGap = 6.dp
        val NameChipGap = 8.dp
        val NameChipPaddingHorizontal = 14.dp
        val NameChipPaddingVertical = 8.dp
        val NameCtaHorizontal = 24.dp
        val NameCtaTop = 12.dp
        val NameCtaBottom = 32.dp

        // Interval and first stretch still use the earlier layout (no ui_v2 restyle yet)
        val NameTextTop = 16.dp
        val NameFieldHorizontal = 24.dp
        val NameButtonHorizontal = 24.dp

        // Work hours and interval (no mockup)
        val QuestionTop = 8.dp
        val QuestionGap = 12.dp
        val QuestionAnswerTop = 4.dp
        val DayChip = 40.dp
        /** The least space between two day circles; narrower cells shrink the circles. */
        val DayChipGap = 6.dp
        val DayChipTouch = 48.dp
        val IntervalPetSize = 170.dp
        val SheetHorizontal = 24.dp
        val SheetBottom = 24.dp
        val SheetGap = 16.dp

        // First stretch and the paywall placeholder (no mockup)
        val StretchPetSize = 220.dp
        val StretchButtonBottom = 24.dp
    }

    /** The Settings tab, measured from design/ui_v2/Settings.dc.html. */
    object Settings {
        /** From this font scale the "Bun & you" card's Premium chip sits under its text. */
        const val BadgeBelowFontScale = 1.3f
        /** Title, section headings and the about lines sit 4 dp in from the cards. */
        val TextInset = 4.dp
        val TitleBottom = 14.dp
        val SectionTop = 20.dp
        val SectionBottom = 10.dp
        val AboutVertical = 12.dp
        /** Save work hours and its reason, under the card. */
        val HoursSaveTop = 10.dp

        // "Bun & you" card
        val HeaderEdge = 4.dp
        val HeaderPaddingStart = 12.dp
        val HeaderPaddingEnd = 16.dp
        val HeaderPaddingVertical = 12.dp
        val HeaderGap = 12.dp
        val HeaderTextGap = 2.dp
        val HeaderTile = 56.dp
        val HeaderTileRadius = 18.dp
        val HeaderPetWidth = 46.dp
        val HeaderPetHeight = 48.dp

        // Day circles row
        val DaysPaddingHorizontal = 16.dp
        /** 14 dp around the 40 dp circles in the mockup; the 48 dp touch cells take 4 of it. */
        val DaysPaddingVertical = 10.dp

        // Small Ink "Turn on" / "Add" button on a setup row
        val ActionPaddingHorizontal = 14.dp
        val ActionPaddingVertical = 7.dp
        val ActionRadius = 12.dp
        val ActionEdge = 3.dp

        val HintPaddingTop = 12.dp
    }

    /** Setup cards in Settings and on Home (no mockup: sized from the card and icon-tile tokens). */
    object Setup {
        val IconTile = 44.dp
        val Icon = 32.dp
        val RowGap = 12.dp
        val ChipHorizontal = 12.dp
        val ChipVertical = 4.dp
        val NotNowMinSize = 48.dp
        val SheetHorizontal = 24.dp
        val SheetBottom = 24.dp
        val SheetGap = 12.dp
        val SheetPet = 120.dp
        val HomePromptTop = 12.dp
    }

    /** Home screen, measured from design/ui_v2/Main.dc.html (focused card: mockups_source/FocusCard.dc.html). */
    object Home {
        /** The room below the status bar; it only grows when a large font makes the bubble taller. */
        val SceneHeight = 340.dp
        /** 170 dp wide at the viewBox's 200×210 proportions. */
        val PetWidth = 170.dp
        val PetHeight = 178.5.dp
        /** From the pet's box to the scene's bottom: its feet stand on the rug. */
        val PetAboveSceneBottom = 23.5.dp
        /** The bubble's tail tip reaches this far into the pet's box (the space above its ears). */
        val BubbleIntoPet = 17.dp
        /** Mockup width at 100% font; wider (up to the screen's margins) at larger font sizes. */
        val BubbleMaxWidth = 198.dp
        val BubbleSideMargin = 24.dp
        /** Least room between the top buttons and the bubble before the scene grows. */
        val BubbleBelowTopBar = 12.dp
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

        // Timer card
        val PillPaddingHorizontal = 18.dp
        val PillPaddingTop = 16.dp
        val PillPaddingBottom = 14.dp
        val PillGap = 10.dp
        val PillLabelGap = 2.dp
        val ProgressHeight = 12.dp
        val CtaTopGap = 4.dp
        val DividerThickness = 1.5.dp
        val StreakGap = 8.dp
        val SegmentWidth = 26.dp
        val SegmentHeight = 8.dp
        val SegmentGap = 4.dp
        val FlamePaddingStart = 7.dp
        val FlamePaddingEnd = 10.dp
        val FlamePaddingVertical = 5.dp
        val FlameGap = 5.dp
        val FlameIcon = 16.dp

        val HeaderPaddingHorizontal = 20.dp
        val HeaderPaddingTop = 20.dp
        val HeaderPaddingBottom = 10.dp

        // Break cards
        val CardEdge = 3.dp
        val CardPaddingHorizontal = 12.dp
        val CardPaddingVertical = 11.dp
        val CardGap = 12.dp
        val CardTextGap = 1.dp
        val IconTileSize = 50.dp
        /** Full-colour break icons fill about 3/4 of their tile (design/ui_v2: 38 in 50). */
        val BreakIcon = 38.dp
        val RewardGap = 3.dp
        val RewardIcon = 16.dp
        val RewardChipPaddingStart = 7.dp
        val RewardChipPaddingEnd = 9.dp
        val RewardChipPaddingVertical = 4.dp
        val RewardChipIcon = 15.dp
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
        val FocusIcon = 54.dp
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

    /** Break session + celebration, measured from design/ui_v2/Stretch.dc.html and Celebrate.dc.html (390-wide artboards). */
    object BreakSession {
        val TopPadding = 16.dp
        val HeaderGap = 8.dp

        // The pill with the break's icon and name
        val PillIcon = 28.dp
        val PillPaddingStart = 8.dp
        val PillPaddingEnd = 14.dp
        val PillPaddingVertical = 6.dp
        val PillEdge = 3.dp

        val SegmentsTop = 14.dp
        val SegmentsHorizontal = 24.dp
        val SegmentGap = 6.dp
        val SegmentHeight = 8.dp

        // The ring: a 300 dp box, soft disc r138, track r130, stroke 12
        val RingSize = 300.dp
        val RingStroke = 12.dp
        const val RingRadiusFraction = 130f / 300f
        const val DiscRadiusFraction = 138f / 300f
        /** The pet's width in the ring (166 of 300). */
        const val PetInRing = 166f / 300f
        /** The ring never shrinks below this fraction of [RingSize] to make room for the text. */
        const val RingMinScale = 0.45f
        val RingTop = 14.dp
        /** The pet sits a little low in its box; this lifts it to the middle of the ring. */
        const val PetLift = 0.07f
        /** Above this font scale the ring, timer and text scroll together above the pinned buttons. */
        const val LargeFontScale = 1.3f
        /** The ring's size in that layout, as a fraction of [RingSize]. */
        const val LargeFontRingScale = 0.5f
        /** From this font scale the ring is smaller still, so the step's title and text start on the first screen. */
        const val HugeFontScale = 1.6f
        const val HugeFontRingScale = 0.3f
        /** The timer pill overlaps the ring's box by this much at full size (it sits on the ring's foot). */
        val TimerOverlap = 20.dp
        val TimerGap = 8.dp
        val TimerEdge = 3.dp
        val TimerPaddingHorizontal = 16.dp
        val TimerPaddingVertical = 4.dp
        val ChipPaddingHorizontal = 14.dp
        val ChipPaddingVertical = 8.dp

        // The white panel with the step's text, and its buttons
        /** The text area is at least this tall (when there is room), so the buttons sit at the panel's foot as in the mockup. */
        val TextMinHeight = 220.dp
        val NoteIcon = 16.dp
        val NoteGap = 8.dp
        val ButtonsTop = 14.dp
        val ButtonsBottom = 40.dp
        val ButtonsGap = 12.dp
        val ButtonHeight = 58.dp
        val PauseWidth = 112.dp
        val ButtonPaddingHorizontal = 12.dp

        // Celebration: the scene is drawn 390 × 400 and scaled
        val SceneHeight = 400.dp
        val SceneWidth = 390.dp
        /**
         * What the title and subtitle, then each reward card and the streak card, need at normal
         * font size; the scene (pet and glow) gets the rest, so three cards still fit on 390 x 844.
         */
        val LowerBase = 142.dp
        val RewardCardSlot = 92.dp
        val StreakCardSlot = 96.dp
        const val SceneMinScale = 0.45f
        /** Above this font scale Continue scrolls with the content instead of staying pinned. */
        const val CelebrateLargeFontScale = 1.15f
        val PetSize = 200.dp
        val CelebrateHorizontal = 20.dp
        val CelebrateGap = 12.dp
        val TitleGap = 8.dp
        val TextHorizontal = 24.dp
        val RewardCardPaddingHorizontal = 16.dp
        val RewardCardPaddingVertical = 14.dp
        val RewardCardEdge = 4.dp
        val RewardTile = 48.dp
        val RewardTileIcon = 30.dp
        val RewardGap = 12.dp
        val StreakBarHeight = 12.dp
        val StreakBarGap = 6.dp
        val CelebrateButtonBottom = 40.dp
    }

    /** Collection tab, measured from design/mockups_source/Collection.dc.html. */
    // design/ui_v2/Collection.dc.html (390-wide artboard)
    object Collection {
        val HeaderPaddingTop = 22.dp
        val HeaderPaddingStart = 20.dp
        val HeaderPaddingEnd = 16.dp
        val HeaderPaddingBottom = 8.dp
        val SubtitlePaddingHorizontal = 20.dp
        val SubtitlePaddingBottom = 18.dp
        val GridGap = 12.dp
        val CardMinHeight = 206.dp
        val CardPaddingTop = 10.dp
        val CardPaddingHorizontal = 10.dp
        val CardPaddingBottom = 14.dp
        val CardGap = 6.dp
        val CardEdge = 4.dp
        val ActiveBorder = 3.dp
        val PetWidth = 112.dp
        val PetHeight = 118.dp
        val PillPaddingHorizontal = 12.dp
        val PillPaddingVertical = 4.dp
        val PillIcon = 14.dp
        val PillIconGap = 4.dp
        val ListBottomPadding = 24.dp

        // The glow behind the pet that is with you: 120×84, round top, at the card's top 14.
        val HaloWidth = 120.dp
        val HaloHeight = 84.dp
        val HaloTopRadius = 60.dp
        val HaloBottomRadius = 18.dp
        /** From the pet's top (the card centres it 13 dp down) to the glow's. */
        val HaloTopInPet = 1.dp

        // Circles and dots behind the header, in artboard units; x from the left or right edge.
        val DecorLargeRadius = 70.dp
        val DecorLargeFromRight = 38.dp
        val DecorLargeY = 128.dp
        val DecorSmallRadius = 44.dp
        val DecorSmallX = 26.dp
        val DecorSmallY = 40.dp
        /** Artboard (300, 30) and (318, 52) measured from the right, (90, 150) from the left. */
        val DecorDots = listOf(
            DecorDot(x = 90.dp, y = 30.dp, radius = 4.dp, fromRight = true),
            DecorDot(x = 72.dp, y = 52.dp, radius = 3.dp, fromRight = true),
            DecorDot(x = 90.dp, y = 150.dp, radius = 3.5.dp, fromRight = false),
        )

        // "Next pet" card
        val NextCardRadius = 22.dp
        val NextCardPaddingHorizontal = 14.dp
        val NextCardPaddingVertical = 12.dp
        val NextCardGap = 12.dp
        val NextCardTextGap = 6.dp
        val NextCardPaddingBottom = 20.dp
        val NextTile = 56.dp
        val NextTileRadius = 18.dp
        val NextTilePetWidth = 44.dp
        val NextTilePetHeight = 46.dp
        val NextBar = 10.dp

        // Pet sheet (no mockup: sheet radius from CLAUDE.md §11, buttons as elsewhere)
        val SheetPaddingHorizontal = 24.dp
        val SheetPaddingBottom = 24.dp
        val SheetGap = 8.dp
        val SheetPetSize = 168.dp
        val SheetSmallPetSize = 120.dp
        val SheetButtonTop = 12.dp
        val SheetButtonIcon = 20.dp
        val SheetButtonIconGap = 6.dp

        // Pets | Accessories switch: 46 dp track, the selected half white on a 2 dp edge
        val TabsPaddingBottom = 16.dp
        val TabsHeight = 46.dp
        val TabsInnerPadding = 4.dp
        val TabSelectedEdge = 2.dp
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

    /** Hours and Interval onboarding screens (Hours.dc.html, Interval.dc.html: 390 x 844, panel from y 302). */
    object OnboardingPanel {
        val SceneHeight = 302.dp
        /** The scene never shrinks below this fraction, whatever the font or phone. */
        const val MinScale = 0.45f
        /** Above this font scale the button scrolls with the panel instead of staying pinned. */
        const val LargeFontScale = 1.15f
        /** Below Back and Skip at normal font size, and how much more per extra font scale (the Skip pill grows). */
        val TopButtonsClear = 68.dp
        val TopButtonsClearPerFont = 24.dp
        val CtaHorizontal = 24.dp
        val CtaTop = 12.dp
        val CtaBottom = 32.dp
        val ErrorTop = 8.dp
        val BodyGap = 6.dp
        val TitleSize = 29.sp
        /** What the Hours and Interval panels need at normal font size: the scene leaves room for it. */
        val HoursPanelHeight = 340.dp
        val IntervalPanelHeight = 380.dp

        // Hours: the pet and the 24-hour bar
        val HoursPetTop = 64.dp
        val HoursPetSize = 130.dp
        val ClockBarTop = 220.dp
        val ClockBarHeight = 34.dp
        val ClockBarInset = 30.dp
        val ClockBarEdge = 4.dp
        val ClockLabelsTop = 262.dp
        /** The same as [ClockBarInset]: the two midnight labels start and end where the bar does. */
        val ClockLabelsInset = 30.dp
        val ClockLabelGap = 6.dp
        val ClockSunRadius = 9.dp
        val ClockSunRay = 2.4.dp
        val ClockMoonRadius = 8.dp
        /** A sun or moon needs at least this much of the bar around it. */
        val ClockSymbolRoom = 34.dp
        val RowsRadius = 22.dp
        val RowsDividerThickness = 1.5.dp

        // Interval: the two pets and the slider card
        val IntervalPetsTop = 96.dp
        val IntervalPetSize = 118.dp
        val IntervalPetColumn = 124.dp
        val IntervalPetLabelGap = 14.dp
        val IntervalArrowWidth = 44.dp
        val IntervalArrowHeight = 24.dp
        val IntervalArrowGap = 10.dp
        val IntervalArrowDrop = 64.dp
        val IntervalLabelPaddingHorizontal = 12.dp
        val IntervalLabelPaddingVertical = 4.dp
        val IntervalLabelsBottomLargeFont = 14.dp
        val IntervalHillRadiusX = 78.dp
        val IntervalHillRadiusY = 14.dp
        val IntervalHillOffset = 20.dp
        val IntervalColumnCentre = 94.dp
        val IntervalChipGap = 4.dp
        val IntervalLabelSize = 13.sp
        val IntervalCardRadius = 24.dp
        val IntervalCardPaddingTop = 16.dp
        val IntervalCardPaddingHorizontal = 18.dp
        val IntervalCardPaddingBottom = 18.dp
        val IntervalCardGap = 14.dp
        val IntervalValueSize = 40.sp
        val SliderHeight = 48.dp
        val SliderTrackHeight = 8.dp
        val SliderThumb = 30.dp
        val SliderThumbRing = 3.dp
        val SliderThumbDrop = 4.dp
    }

    /** Home while the app is locked (design/ui_v2/Locked.dc.html); the room itself uses [Home]'s sizes. */
    object LockedHome {
        val CardTop = 10.dp
        val CardBottom = 16.dp
        val CardRadius = 28.dp
        val CardEdge = 5.dp
        val CardPadding = 20.dp
        val CardPaddingTop = 22.dp
        val Gap = 10.dp
        val TitleSize = 26.sp
        val ChipsVertical = 2.dp
        val ChipGap = 8.dp
        val ChipPaddingHorizontal = 11.dp
        val ChipPaddingVertical = 5.dp
    }

    /** The paywall (design/ui_v2/Paywall.dc.html, a 390 x 844 artboard). */
    object Paywall {
        // The sky with the pet; the panel's rounded top covers the pet's feet.
        val SceneHeight = 212.dp
        const val MinScale = 0.45f
        /** Above this font scale the button and small print scroll with the panel instead of staying pinned. */
        const val LargeFontScale = 1.15f
        /** What the panel needs at normal font size (title, benefits, timeline, plans): the scene leaves room for it. */
        val PanelNeed = 446.dp
        /**
         * A screen shorter than this pins only the button: the small print and the links go right
         * under the plans, so the plans are not hidden behind a tall footer.
         */
        val CompactHeight = 760.dp
        val HaloY = 168.dp
        val HaloOuter = 112.dp
        val HaloInner = 80.dp
        val PetTop = 74.dp
        val PetWidth = 154.dp
        val PetHeight = 162.dp
        val TopBarTop = 14.dp
        val TopBarStart = 16.dp
        val TopBarEnd = 12.dp
        val RestoreMinSize = 48.dp
        val RestorePaddingHorizontal = 8.dp
        val RestoreSize = 14.sp

        val PanelBottom = 12.dp
        /** The mockup has 27 sp in a wider panel; 25 keeps "Keep Bun moving with you" on one line down to 360 dp. */
        val TitleSize = 25.sp
        val BenefitGap = 8.dp
        val BenefitTile = 34.dp
        val BenefitTileRadius = 11.dp
        val BenefitTextStart = 12.dp
        val BenefitPetWidth = 24.dp
        val BenefitPetHeight = 25.dp
        const val BenefitPetEnergy = 0.55f
        val BenefitNeckIcon = 26.dp
        val BenefitSparkIcon = 22.dp

        val TimelineRadius = 18.dp
        val TimelinePaddingHorizontal = 10.dp
        val TimelinePaddingTop = 12.dp
        val TimelinePaddingBottom = 10.dp
        val TimelineDot = 18.dp
        val TimelineDotRing = 4.dp
        val TimelineLine = 3.dp
        val TimelineTextGap = 4.dp

        /** Room above the cards for the badge that sits on the yearly card's top edge. */
        val PlansTop = 10.dp
        val PlanGap = 8.dp
        val PlanHeight = 104.dp
        val PlanRadius = 20.dp
        val PlanEdge = 4.dp
        val PlanBorderSelected = 3.dp
        val PlanBorder = 2.dp
        val PlanPaddingHorizontal = 4.dp
        val PlanPaddingVertical = 8.dp
        val PlanTextGap = 2.dp
        val PlanLabelSize = 14.sp
        val PlanPriceSize = 21.sp
        val PlanPriceMinSize = 12.sp
        val PlanSubSize = 12.sp
        val BadgeLift = 11.dp
        val BadgePaddingHorizontal = 9.dp
        val BadgePaddingVertical = 3.dp
        val BadgeSize = 11.sp
        val ProblemPadding = 16.dp
        val ProblemGap = 12.dp

        /** The same as the panel's own side padding (Shared.PanelPaddingHorizontal). */
        val FooterHorizontal = 24.dp
        val FooterTop = 10.dp
        val FooterBottom = 12.dp
        val FooterGap = 8.dp
        val FinePrintSize = 12.5.sp
        val FinePrintLineHeight = 18.sp
        val LinkGap = 6.dp
        val LinkMinSize = 48.dp
        val LinkPaddingHorizontal = 8.dp
        val LinkSize = 13.sp
    }
}

/** A dot behind a header, [x] from the left edge or, with [fromRight], from the right one. */
data class DecorDot(val x: Dp, val y: Dp, val radius: Dp, val fromRight: Boolean)
