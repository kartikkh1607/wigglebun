package com.kartikkhandelwal.wigglebun.widget

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.glance.GlanceId
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.kartikkhandelwal.wigglebun.data.prefs.SettingsRepository
import com.kartikkhandelwal.wigglebun.data.prefs.SitStateStore
import com.kartikkhandelwal.wigglebun.data.repo.PetRepository
import com.kartikkhandelwal.wigglebun.domain.sitting.SittingEngine
import com.kartikkhandelwal.wigglebun.ui.pet.displayName
import com.kartikkhandelwal.wigglebun.ui.theme.Dimens
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface WidgetEntryPoint {
    fun engine(): SittingEngine

    fun store(): SitStateStore

    fun settings(): SettingsRepository

    fun pets(): PetRepository
}

/** Reads the current sit state once; the widget is redrawn by [WidgetRefresher], not by a ticker. */
internal suspend fun loadWidgetState(context: Context): WidgetUiState {
    val app = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
    val status = app.engine().status(app.store().current(), app.settings().current())
    val pet = app.pets().activePet.first()
    return WidgetUiState.from(status, pet.id, pet.displayName(context))
}

/**
 * Home-screen widget (CLAUDE.md §8). Glance turns this into RemoteViews: no animation, and the
 * minutes only move when it is redrawn (every 15 min in work hours, and right after any change).
 */
class WigglebunWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = loadWidgetState(context)
        provideContent {
            WidgetContent(state, medium = LocalSize.current.width >= MEDIUM.width)
        }
    }

    companion object {
        val SMALL = DpSize(Dimens.Widget.SmallMinWidth, Dimens.Widget.MinHeight)
        val MEDIUM = DpSize(Dimens.Widget.MediumMinWidth, Dimens.Widget.MinHeight)
    }
}

/** Runs the 15-minute refresh while at least one widget is on the home screen (work hours only). */
class WigglebunWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WigglebunWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // Scheduled without checking work hours (that needs the saved state, too slow for a
        // receiver callback): if this is outside them, the first run sees the sleeping pet and
        // cancels itself until the next session starts.
        WidgetRefreshWorker.schedule(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetRefreshWorker.cancel(context)
    }
}
