package com.kartikkhandelwal.wigglebun.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * A screen's background and the chunky edge under the white cards and buttons on it
 * (design/ui_v2: the edge is a darker shade of the background).
 */
@Immutable
data class SurfaceColors(val background: Color, val edge: Color)

/** One per screen (design/ui_v2/README.md). */
object Surfaces {
    /** Home's floor, under the timer card and the break list. */
    val Home = SurfaceColors(HomeFloor, HomeFloorEdge)

    /** Home's wall: the room scene behind the top buttons and the pet. */
    val HomeWall = SurfaceColors(com.kartikkhandelwal.wigglebun.ui.theme.HomeWall, HomeWallEdge)
    val Collection = SurfaceColors(CollectionMint, CollectionEdge)
    val Settings = SurfaceColors(SettingsLilac, SettingsEdge)
    val Profile = SurfaceColors(Sky, SkyEdge)
    val Session = SurfaceColors(BreakSky, SessionEdge)

    /** Onboarding and the paywall placeholder until their UI v2 restyle. */
    val Cream = SurfaceColors(com.kartikkhandelwal.wigglebun.ui.theme.Cream, CardEdge)
}

/**
 * The surface the content is on. Each screen provides its own at its root, and the chunky
 * pieces ([com.kartikkhandelwal.wigglebun.ui.components.PillCard], SparksPill, ChunkySurface…)
 * take its edge by default, so screens don't pass colours around.
 */
val LocalSurface = staticCompositionLocalOf { Surfaces.Cream }

/** Puts [content] on [surface]: the caller still paints the background (it knows its shape). */
@Composable
fun OnSurface(surface: SurfaceColors, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalSurface provides surface, content = content)
}
