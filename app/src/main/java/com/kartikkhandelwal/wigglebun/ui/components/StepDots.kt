package com.kartikkhandelwal.wigglebun.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import com.kartikkhandelwal.wigglebun.ui.theme.Ink
import com.kartikkhandelwal.wigglebun.ui.theme.StepDotIdle
import com.kartikkhandelwal.wigglebun.ui.theme.WigglebunShapes

private val S = Dimens.Shared

/**
 * Progress dots (design/ui_v2): steps done and the current one in [active], the current one
 * wider, the rest in [inactive]. [step] is 1-based. TalkBack reads "Step 2 of 4".
 */
@Composable
fun StepDots(step: Int, count: Int, modifier: Modifier = Modifier, active: Color = Ink, inactive: Color = StepDotIdle) {
    val label = stringResource(R.string.onboarding_step, step, count)
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(S.DotGap),
    ) {
        repeat(count) { index ->
            Dot(width = if (index == step - 1) S.DotCurrentWidth else S.DotSize, color = if (index < step) active else inactive)
        }
    }
}

@Composable
private fun Dot(width: Dp, color: Color) {
    Box(Modifier.size(width, S.DotSize).clip(WigglebunShapes.Pill).background(color))
}
