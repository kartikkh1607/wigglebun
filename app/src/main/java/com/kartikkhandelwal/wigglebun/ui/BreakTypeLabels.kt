package com.kartikkhandelwal.wigglebun.ui

import androidx.annotation.StringRes
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.domain.model.BreakType

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
