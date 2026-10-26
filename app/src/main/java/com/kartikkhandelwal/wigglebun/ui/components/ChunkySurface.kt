package com.kartikkhandelwal.wigglebun.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp

/**
 * Shared "chunky" look: a solid face with a darker, same-shaped edge offset below it.
 * When pressed, the face slides down by the edge height and covers the edge (80 ms);
 * on release it springs back up.
 */
@Composable
internal fun ChunkySurface(
    shape: Shape,
    faceColor: Color,
    edgeColor: Color,
    edgeHeight: Dp,
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    role: Role = Role.Button,
    interactionSource: MutableInteractionSource? = null,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val pressedDown = pressed && enabled && onClick != null
    val press by animateFloatAsState(
        targetValue = if (pressedDown) 1f else 0f,
        animationSpec = if (pressedDown) {
            tween(durationMillis = 80)
        } else {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        },
        label = "chunkyPress",
    )

    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = source,
            indication = null, // the press-down motion is the feedback
            enabled = enabled,
            role = role,
            onClick = onClick,
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            // Reserve room for the edge so it never overlaps neighbours.
            .padding(bottom = edgeHeight)
            .drawBehind {
                // The edge stays put; when pressed, the face slides down over it.
                val outline = shape.createOutline(size, layoutDirection, this)
                translate(top = edgeHeight.toPx()) {
                    drawOutline(outline, edgeColor)
                }
            }
            .graphicsLayer { translationY = edgeHeight.toPx() * press }
            .clip(shape)
            .background(faceColor)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .then(clickModifier),
        contentAlignment = contentAlignment,
        propagateMinConstraints = true,
        content = content,
    )
}
