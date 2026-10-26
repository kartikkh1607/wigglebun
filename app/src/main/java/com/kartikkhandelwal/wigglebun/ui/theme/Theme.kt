package com.kartikkhandelwal.wigglebun.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

// Light only: the design has no dark variant, and dynamic colour would override the brand palette.
private val WigglebunColorScheme = lightColorScheme(
    primary = Ink,
    onPrimary = Card,
    secondary = DoneGreen,
    onSecondary = Card,
    tertiary = Sun,
    onTertiary = Ink,
    error = Coral,
    onError = Card,
    background = Cream,
    onBackground = Ink,
    surface = Card,
    onSurface = Ink,
    surfaceVariant = Track,
    onSurfaceVariant = InkMuted,
    outline = ButtonBorder,
    outlineVariant = CardEdge,
)

@Composable
fun WigglebunTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WigglebunColorScheme,
        typography = WigglebunTypography,
    ) {
        // Text outside a Material Surface would otherwise default to pure black.
        CompositionLocalProvider(LocalContentColor provides Ink, content = content)
    }
}
