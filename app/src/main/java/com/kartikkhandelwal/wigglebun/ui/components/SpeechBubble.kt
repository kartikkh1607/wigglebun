package com.kartikkhandelwal.wigglebun.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kartikkhandelwal.wigglebun.ui.theme.Card
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.LocalSurface
import com.kartikkhandelwal.wigglebun.ui.theme.OnSurface
import com.kartikkhandelwal.wigglebun.ui.theme.Surfaces
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunShapes
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTextStyles
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunTheme

private val S = Dimens.Shared

/**
 * What the pet says, in a white bubble with its tail pointing down at the pet (Main.dc.html).
 * The edge under the bubble is the surface's; the tail is drawn over it. Plain text for
 * TalkBack; the screen decides how it's announced. The caller limits the width.
 */
@Composable
fun SpeechBubble(text: String, modifier: Modifier = Modifier, edgeColor: Color = LocalSurface.current.edge) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .drawBehind {
                    val outline = WigglebunShapes.Bubble.createOutline(size, layoutDirection, this)
                    translate(top = S.BubbleEdge.toPx()) { drawOutline(outline, edgeColor) }
                }
                .background(Card, WigglebunShapes.Bubble)
                .padding(horizontal = S.BubblePaddingHorizontal, vertical = S.BubblePaddingVertical),
        ) {
            Text(text, style = WigglebunTextStyles.Bubble, color = Ink, textAlign = TextAlign.Center)
        }
        Canvas(Modifier.offset(y = -S.BubbleTailOverlap).size(S.BubbleTailWidth, S.BubbleTailHeight)) {
            val tail = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2, size.height)
                close()
            }
            drawPath(tail, Card)
        }
    }
}

// Preview copy is sample data only; real screens read strings from resources.
@Preview(widthDp = 390)
@Composable
internal fun SpeechBubblePreview() {
    WigglebunTheme {
        OnSurface(Surfaces.HomeWall) {
            Column(
                Modifier.background(LocalSurface.current.background).padding(Dimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.ListGap),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                listOf("Feeling bouncy!", "Getting sleepy… stretch with me?", "So stiff… walk with me?").forEach {
                    SpeechBubble(it, Modifier.widthIn(max = 198.dp))
                }
            }
        }
    }
}
