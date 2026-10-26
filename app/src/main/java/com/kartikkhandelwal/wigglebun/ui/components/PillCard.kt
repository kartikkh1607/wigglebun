package com.kartikkhandelwal.wigglebun.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kartikkhandelwal.wigglebun.ui.theme.Card
import com.kartikkhandelwal.wigglebun.ui.theme.CardEdge
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.InkMuted
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunShapes
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme

/**
 * White card with the chunky CardEdge below it. Pass [shape] = [WigglebunShapes.Pill]
 * for pill-shaped chips such as the sparks counter. Clickable cards press down like buttons.
 */
@Composable
fun PillCard(
    modifier: Modifier = Modifier,
    shape: Shape = WigglebunShapes.Card,
    edgeHeight: Dp = Dimens.CardEdge,
    contentPadding: PaddingValues = PaddingValues(Dimens.CardPadding),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ChunkySurface(
        shape = shape,
        faceColor = Card,
        edgeColor = CardEdge,
        edgeHeight = edgeHeight,
        modifier = modifier,
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

// Preview copy is sample data only; real screens read strings from resources.
@Preview(showBackground = true, backgroundColor = 0xFFFFF4E8)
@Composable
internal fun PillCardPreview() {
    WigglebunTheme {
        Column(
            modifier = Modifier.padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.ListGap),
        ) {
            PillCard(modifier = Modifier.fillMaxWidth()) {
                Text("Time since last break", style = MaterialTheme.typography.labelMedium, color = InkMuted)
                Text("32 min", style = MaterialTheme.typography.displayMedium)
            }
            PillCard(
                modifier = Modifier.fillMaxWidth(),
                edgeHeight = 3.dp,
                contentPadding = PaddingValues(12.dp),
                onClick = {},
            ) {
                Text("Neck stretch", style = MaterialTheme.typography.titleMedium)
                Text("2 min", style = MaterialTheme.typography.bodyMedium, color = InkMuted)
            }
            Row {
                PillCard(
                    shape = WigglebunShapes.Pill,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text("120", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
