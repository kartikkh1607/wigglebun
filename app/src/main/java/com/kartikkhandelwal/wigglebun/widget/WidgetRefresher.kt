package com.kartikkhandelwal.wigglebun.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import com.kartikkhandelwal.wigglebun.data.ApplicationScope
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "Widget"

/** Redraws the home-screen widget after anything that changes the pet (Done, Snooze, Skip, walks, alarms). */
fun interface WidgetRefresher {
    fun requestUpdate()
}

@Singleton
class GlanceWidgetRefresher @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
) : WidgetRefresher {
    // Conflated: a burst of requests (e.g. Done then the alarm re-arm) becomes one redraw.
    private val requests = Channel<Unit>(Channel.CONFLATED)

    init {
        scope.launch {
            for (request in requests) {
                // Cheap when no widget is placed: updateAll finds no ids and returns.
                try {
                    WigglebunWidget().updateAll(context)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Widget update failed", e) // the next change or 15-min refresh retries
                }
            }
        }
    }

    override fun requestUpdate() {
        requests.trySend(Unit)
    }

    companion object {
        suspend fun hasWidgets(context: Context): Boolean =
            GlanceAppWidgetManager(context).getGlanceIds(WigglebunWidget::class.java).isNotEmpty()
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class WidgetModule {
    @Binds
    abstract fun bindWidgetRefresher(impl: GlanceWidgetRefresher): WidgetRefresher
}
