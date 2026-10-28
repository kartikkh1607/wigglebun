package com.kartikkhandelwal.wigglebun.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.domain.model.BreakCategory
import com.kartikkhandelwal.wigglebun.domain.model.BreakType
import com.kartikkhandelwal.wigglebun.ui.theme.BreakEyes
import com.kartikkhandelwal.wigglebun.ui.theme.BreakEyesTint
import com.kartikkhandelwal.wigglebun.ui.theme.BreakNeck
import com.kartikkhandelwal.wigglebun.ui.theme.BreakNeckTint
import com.kartikkhandelwal.wigglebun.ui.theme.BreakStandTint
import com.kartikkhandelwal.wigglebun.ui.theme.BreakWalk
import com.kartikkhandelwal.wigglebun.ui.theme.BreakWalkTint
import com.kartikkhandelwal.wigglebun.ui.theme.BreakWater
import com.kartikkhandelwal.wigglebun.ui.theme.BreakWaterTint
import com.kartikkhandelwal.wigglebun.ui.theme.BreakWristTint
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.MoodHappyChip
import com.kartikkhandelwal.wigglebun.ui.theme.MovementChipText

@get:StringRes
val BreakType.labelRes: Int
    get() = when (this) {
        BreakType.STAND_UP -> R.string.break_stand_up
        BreakType.WALK -> R.string.break_walk
        BreakType.NECK -> R.string.break_neck
        BreakType.SHOULDERS -> R.string.break_shoulders
        BreakType.WRIST -> R.string.break_wrist
        BreakType.EYES -> R.string.break_eyes
        BreakType.WATER -> R.string.break_water
    }

@get:DrawableRes
val BreakType.iconRes: Int
    get() = when (this) {
        BreakType.STAND_UP -> R.drawable.ic_break_stand_up
        BreakType.WALK -> R.drawable.ic_break_walk
        BreakType.NECK -> R.drawable.ic_break_neck
        BreakType.SHOULDERS -> R.drawable.ic_break_shoulders
        BreakType.WRIST -> R.drawable.ic_break_wrist
        BreakType.EYES -> R.drawable.ic_break_eyes
        BreakType.WATER -> R.drawable.ic_break_water
    }

/** Light tint for icon tiles (CLAUDE.md §11). Shoulder rolls shares Neck's blue (no own colour). */
val BreakType.tint: Color
    get() = when (this) {
        BreakType.STAND_UP -> BreakStandTint
        BreakType.WALK -> BreakWalkTint
        BreakType.NECK, BreakType.SHOULDERS -> BreakNeckTint
        BreakType.WRIST -> BreakWristTint
        BreakType.EYES -> BreakEyesTint
        BreakType.WATER -> BreakWaterTint
    }

@get:StringRes
val BreakCategory.labelRes: Int
    get() = when (this) {
        BreakCategory.MOVEMENT -> R.string.break_category_movement
        BreakCategory.STRETCH -> R.string.break_category_stretch
        BreakCategory.EYES -> R.string.break_category_eyes
        BreakCategory.WATER -> R.string.break_category_water
    }

/**
 * Weekly chart colours (CLAUDE.md §11): one per category, so stacked segments never repeat a
 * colour (Shoulder rolls has no break-type colour of its own).
 */
val BreakCategory.chartColor: Color
    get() = when (this) {
        BreakCategory.MOVEMENT -> BreakWalk
        BreakCategory.STRETCH -> BreakNeck
        BreakCategory.EYES -> BreakEyes
        BreakCategory.WATER -> BreakWater
    }

/** Category chip colours: Movement as in FocusCard.dc.html; others use the break's tint + Ink. */
fun BreakType.categoryChipColors(): Pair<Color, Color> =
    if (category == BreakCategory.MOVEMENT) MoodHappyChip to MovementChipText else tint to Ink
