package com.kartikkhandelwal.wigglebun.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * One-shot outcomes a ViewModel reports once its work has finished. They travel in a [Channel], so
 * each is delivered to exactly one collector and then gone: not replayed on rotation, on coming
 * back to the screen, or when a state Flow emits the same value again.
 */
sealed interface FeelEvent {
    /** A break was logged and paid; the celebration is on screen. [breakKey] is one per logged break. */
    data class Celebrated(val breakKey: Any, val sparksGranted: Int) : FeelEvent

    /** A pet was adopted or an accessory bought: the confirm tick and the `success` sound. */
    data object Success : FeelEvent

    /** A session just started. */
    data object BreakStarted : FeelEvent

    /** An action was blocked (e.g. work hours that start and end together). */
    data object Rejected : FeelEvent
}

/** A ViewModel's outbox of [FeelEvent]s. */
class FeelEvents {
    private val channel = Channel<FeelEvent>(Channel.BUFFERED)
    val flow: Flow<FeelEvent> = channel.receiveAsFlow()
    fun send(event: FeelEvent) {
        channel.trySend(event)
    }
}

/** Plays each event once, as it arrives. Put it in the route composable that owns the ViewModel. */
@Composable
fun ConsumeFeel(events: Flow<FeelEvent>) {
    val feel = LocalFeel.current
    LaunchedEffect(events, feel) {
        events.collect { event ->
            when (event) {
                is FeelEvent.Celebrated -> {
                    feel.confirm()
                    feel.celebrate(event.breakKey, event.sparksGranted)
                }
                FeelEvent.Success -> feel.confirm(FeelSound.Success)
                FeelEvent.BreakStarted -> feel.play(FeelSound.BreakStart)
                FeelEvent.Rejected -> feel.reject()
            }
        }
    }
}
