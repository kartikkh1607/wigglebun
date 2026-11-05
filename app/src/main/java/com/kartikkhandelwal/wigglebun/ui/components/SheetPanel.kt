package com.kartikkhandelwal.wigglebun.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kartikkhandelwal.wigglebun.ui.theme.Card
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.InkMuted
import com.kartikkhandelwal.wigglebun.ui.theme.LocalSurface
import com.kartikkhandelwal.wigglebun.ui.theme.OnSurface
import com.kartikkhandelwal.wigglebun.ui.theme.Surfaces
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunShapes
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme

private val S = Dimens.Shared

/**
 * The white panel that rises from the bottom of an onboarding screen (Name, Hours, Interval
 * .dc.html): 32 dp top corners and a 3 dp edge above them in the colour of the surface behind.
 * Not a modal sheet: it is part of the screen. The caller sizes it (usually to the bottom).
 */
@Composable
fun SheetPanel(
    modifier: Modifier = Modifier,
    edgeColor: Color = LocalSurface.current.edge,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .drawBehind {
                // The same rounded top, 3 dp higher, peeks out above the white face.
                val outline = WigglebunShapes.PanelTop.createOutline(size, layoutDirection, this)
                translate(top = -S.PanelEdge.toPx()) { drawOutline(outline, edgeColor) }
            }
            .background(Card, WigglebunShapes.PanelTop)
            .padding(top = S.PanelPaddingTop, start = S.PanelPaddingHorizontal, end = S.PanelPaddingHorizontal),
        verticalArrangement = Arrangement.spacedBy(S.PanelGap),
        content = content,
    )
}

// Preview copy is sample data only; real screens read strings from resources.
@Preview(widthDp = 390, heightDp = 360)
@Composable
internal fun SheetPanelPreview() {
    WigglebunTheme {
        OnSurface(Surfaces.HomeWall) {
            Box(Modifier.fillMaxSize().background(LocalSurface.current.background)) {
                SheetPanel(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(260.dp)) {
                    StepDots(step = 2, count = 4)
                    Text("When do you work?", style = MaterialTheme.typography.displayMedium, color = Ink)
                    Text(
                        "I only watch the clock during work hours. The rest of the time, I sleep.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = InkMuted,
                    )
                }
            }
        }
    }
}
