package com.kartikkhandelwal.wigglebun.ui.setup

import android.Manifest

/** The just-in-time setup cards in Settings and on Home (CLAUDE.md §6.7, §10). */
enum class SetupCard { NOTIFICATIONS, MOVEMENT, WIDGET }

enum class SetupStatus {
    /** Can be turned on from here. */
    OFF,

    /** Android won't ask again (or it's off at app level), so only the phone's settings can change it. */
    BLOCKED,

    ON,

    /** The launcher can't add widgets from an app, so all we can do is say how. */
    MANUAL,
}

/** What the card's main button does once the user has read the explanation. */
sealed interface SetupAction {
    data class RequestPermission(val permission: String) : SetupAction

    data object EnableMovement : SetupAction

    data object PinWidget : SetupAction

    data object OpenNotificationSettings : SetupAction

    data object OpenAppSettings : SetupAction

    /** Only an explanation: there's nothing to do from here. */
    data object None : SetupAction
}

data class SetupCardUi(val card: SetupCard, val status: SetupStatus, val action: SetupAction)

data class SetupCardsUi(
    val cards: List<SetupCardUi>,
    /** The one card Home offers, if any. */
    val homePrompt: SetupCardUi?,
)

/** What the phone says right now; re-read every time the screen resumes (it can change in Settings). */
data class SystemSetup(
    val notificationsOn: Boolean,
    /** Android 13+ and the permission isn't granted, so the system dialog can still help. */
    val notificationsNeedPermission: Boolean,
    val notificationsRationale: Boolean,
    val movementGranted: Boolean,
    val movementRationale: Boolean,
    val widgetAdded: Boolean,
    val widgetPinSupported: Boolean,
)

/**
 * Android answers "should show rationale" with false both before the first request and after the
 * user has refused for good, so we keep our own record of having asked to tell them apart.
 */
fun setupCardsUi(
    system: SystemSetup,
    asked: Set<SetupCard>,
    dismissed: Set<SetupCard>,
    movementEnabled: Boolean,
): SetupCardsUi {
    val cards = listOf(
        notificationsCard(system, SetupCard.NOTIFICATIONS in asked),
        movementCard(system, SetupCard.MOVEMENT in asked, movementEnabled),
        widgetCard(system),
    )
    // Home only offers what it can actually do and hasn't offered yet; Settings always has the rest.
    val prompt = cards.firstOrNull { ui ->
        ui.status == SetupStatus.OFF &&
            ui.card !in asked &&
            ui.card !in dismissed &&
            (ui.action is SetupAction.RequestPermission || ui.action == SetupAction.PinWidget)
    }
    return SetupCardsUi(cards, prompt)
}

private fun notificationsCard(system: SystemSetup, asked: Boolean): SetupCardUi = when {
    system.notificationsOn -> SetupCardUi(SetupCard.NOTIFICATIONS, SetupStatus.ON, SetupAction.None)
    system.notificationsNeedPermission && !(asked && !system.notificationsRationale) ->
        SetupCardUi(SetupCard.NOTIFICATIONS, SetupStatus.OFF, SetupAction.RequestPermission(Manifest.permission.POST_NOTIFICATIONS))
    system.notificationsNeedPermission ->
        SetupCardUi(SetupCard.NOTIFICATIONS, SetupStatus.BLOCKED, SetupAction.OpenAppSettings)
    // Permission granted (or not needed) but off at app level: asking again wouldn't change it.
    else -> SetupCardUi(SetupCard.NOTIFICATIONS, SetupStatus.BLOCKED, SetupAction.OpenNotificationSettings)
}

private fun movementCard(system: SystemSetup, asked: Boolean, enabled: Boolean): SetupCardUi = when {
    system.movementGranted && enabled -> SetupCardUi(SetupCard.MOVEMENT, SetupStatus.ON, SetupAction.None)
    system.movementGranted -> SetupCardUi(SetupCard.MOVEMENT, SetupStatus.OFF, SetupAction.EnableMovement)
    asked && !system.movementRationale -> SetupCardUi(SetupCard.MOVEMENT, SetupStatus.BLOCKED, SetupAction.OpenAppSettings)
    else -> SetupCardUi(SetupCard.MOVEMENT, SetupStatus.OFF, SetupAction.RequestPermission(Manifest.permission.ACTIVITY_RECOGNITION))
}

private fun widgetCard(system: SystemSetup): SetupCardUi = when {
    system.widgetAdded -> SetupCardUi(SetupCard.WIDGET, SetupStatus.ON, SetupAction.None)
    system.widgetPinSupported -> SetupCardUi(SetupCard.WIDGET, SetupStatus.OFF, SetupAction.PinWidget)
    else -> SetupCardUi(SetupCard.WIDGET, SetupStatus.MANUAL, SetupAction.None)
}
