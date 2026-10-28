package com.kartikkhandelwal.wigglebun.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.ui.theme.Card
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.TopButtonEdge
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunShapes
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTextStyles

private val H = Dimens.Home

/** The sparks balance pill (Main.dc.html, Collection.dc.html): white, chunky edge, star + number. */
@Composable
fun SparksPill(sparks: Int, modifier: Modifier = Modifier) {
    val description = pluralStringResource(R.plurals.cd_sparks, sparks, sparks)
    ChunkySurface(
        shape = WigglebunShapes.Pill,
        faceColor = Card,
        edgeColor = TopButtonEdge,
        edgeHeight = H.TopButtonEdge,
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        Row(
            modifier = Modifier
                .height(H.SparksHeight)
                .padding(start = H.SparksPaddingStart, end = H.SparksPaddingEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(H.SparksGap),
        ) {
            SparkIcon(H.SparksIcon)
            Text(sparks.toString(), style = WigglebunTextStyles.Sparks, color = Ink)
        }
    }
}

/**
 * The sparks star, in its own colours. [tint] draws it in one colour instead, for a Sun-coloured
 * background where the yellow star would vanish (the Adopt button).
 */
@Composable
fun SparkIcon(size: Dp, tint: Color = Color.Unspecified) {
    Icon(
        painter = painterResource(R.drawable.ic_spark),
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(size),
    )
}
