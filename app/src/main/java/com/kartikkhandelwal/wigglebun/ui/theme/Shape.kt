package com.kartikkhandelwal.wigglebun.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object WigglebunShapes {
    val Card = RoundedCornerShape(24.dp)
    val IconTile = RoundedCornerShape(16.dp)
    val SmallButton = RoundedCornerShape(18.dp)
    val FocusedCard = RoundedCornerShape(28.dp)
    val SheetTop = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val Pill = CircleShape

    /** A break card on Home (design/ui_v2/Main.dc.html). */
    val BreakCard = RoundedCornerShape(22.dp)

    // design/ui_v2 shared pieces
    val TopButton = RoundedCornerShape(14.dp)
    val RowTile = RoundedCornerShape(14.dp)
    val Bubble = RoundedCornerShape(18.dp)
    val PanelTop = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
}
