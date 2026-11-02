package com.kartikkhandelwal.wigglebun.ui.setup

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** What each card says and does, for everything Android can answer. */
class SetupCardsTest {

    private val nothingYet = SystemSetup(
        notificationsOn = false,
        notificationsNeedPermission = true,
        notificationsRationale = false,
        movementGranted = false,
        movementRationale = false,
        widgetAdded = false,
        widgetPinSupported = true,
    )

    private fun ui(
        system: SystemSetup = nothingYet,
        asked: Set<SetupCard> = emptySet(),
        dismissed: Set<SetupCard> = emptySet(),
        movementEnabled: Boolean = true,
    ) = setupCardsUi(system, asked, dismissed, movementEnabled)

    private fun SetupCardsUi.card(card: SetupCard) = cards.single { it.card == card }

    private val notifications = SetupAction.RequestPermission(Manifest.permission.POST_NOTIFICATIONS)
    private val movement = SetupAction.RequestPermission(Manifest.permission.ACTIVITY_RECOGNITION)

    @Test
    fun aNewUserCanTurnEverythingOn() {
        val state = ui()
        assertEquals(SetupCardUi(SetupCard.NOTIFICATIONS, SetupStatus.OFF, notifications), state.card(SetupCard.NOTIFICATIONS))
        assertEquals(SetupCardUi(SetupCard.MOVEMENT, SetupStatus.OFF, movement), state.card(SetupCard.MOVEMENT))
        assertEquals(SetupCardUi(SetupCard.WIDGET, SetupStatus.OFF, SetupAction.PinWidget), state.card(SetupCard.WIDGET))
    }

    /** Refused once: Android says to show a rationale and will ask again. */
    @Test
    fun refusedOnceCanStillBeAsked() {
        val state = ui(nothingYet.copy(notificationsRationale = true), asked = setOf(SetupCard.NOTIFICATIONS))
        assertEquals(SetupStatus.OFF, state.card(SetupCard.NOTIFICATIONS).status)
        assertEquals(notifications, state.card(SetupCard.NOTIFICATIONS).action)
    }

    /** Refused for good: same answer from Android as "never asked", told apart by our record. */
    @Test
    fun refusedForGoodGoesToThePhonesSettings() {
        val state = ui(asked = setOf(SetupCard.NOTIFICATIONS, SetupCard.MOVEMENT))
        assertEquals(SetupCardUi(SetupCard.NOTIFICATIONS, SetupStatus.BLOCKED, SetupAction.OpenAppSettings), state.card(SetupCard.NOTIFICATIONS))
        assertEquals(SetupCardUi(SetupCard.MOVEMENT, SetupStatus.BLOCKED, SetupAction.OpenAppSettings), state.card(SetupCard.MOVEMENT))
    }

    /** Below Android 13, or switched off for the whole app: no dialog can help. */
    @Test
    fun notificationsOffAtAppLevelGoToNotificationSettings() {
        val state = ui(nothingYet.copy(notificationsNeedPermission = false))
        assertEquals(SetupCardUi(SetupCard.NOTIFICATIONS, SetupStatus.BLOCKED, SetupAction.OpenNotificationSettings), state.card(SetupCard.NOTIFICATIONS))
    }

    @Test
    fun whatsOnHasNothingToDo() {
        val state = ui(nothingYet.copy(notificationsOn = true, notificationsNeedPermission = false, movementGranted = true, widgetAdded = true))
        state.cards.forEach { assertEquals(SetupStatus.ON, it.status) }
        state.cards.forEach { assertEquals(SetupAction.None, it.action) }
        assertNull(state.homePrompt)
    }

    /** Granted, but the app's own switch is off: no dialog needed. */
    @Test
    fun movementSwitchedOffJustTurnsBackOn() {
        val state = ui(nothingYet.copy(movementGranted = true), movementEnabled = false)
        assertEquals(SetupCardUi(SetupCard.MOVEMENT, SetupStatus.OFF, SetupAction.EnableMovement), state.card(SetupCard.MOVEMENT))
    }

    @Test
    fun aLauncherThatCantPinWidgetsGetsInstructions() {
        val state = ui(nothingYet.copy(widgetPinSupported = false))
        assertEquals(SetupCardUi(SetupCard.WIDGET, SetupStatus.MANUAL, SetupAction.None), state.card(SetupCard.WIDGET))
    }

    @Test
    fun homeOffersOneCardNotificationsFirst() {
        assertEquals(SetupCard.NOTIFICATIONS, ui().homePrompt?.card)
    }

    @Test
    fun notNowOnHomeMovesToTheNextCard() {
        assertEquals(SetupCard.MOVEMENT, ui(dismissed = setOf(SetupCard.NOTIFICATIONS)).homePrompt?.card)
        assertEquals(SetupCard.WIDGET, ui(dismissed = setOf(SetupCard.NOTIFICATIONS, SetupCard.MOVEMENT)).homePrompt?.card)
        assertNull(ui(dismissed = SetupCard.entries.toSet()).homePrompt)
    }

    /** Asked already (whatever the answer was): Home doesn't nag. Settings still has the card. */
    @Test
    fun homeNeverAsksTwice() {
        val state = ui(nothingYet.copy(notificationsRationale = true), asked = setOf(SetupCard.NOTIFICATIONS))
        assertEquals(SetupCard.MOVEMENT, state.homePrompt?.card)
        assertEquals(SetupStatus.OFF, state.card(SetupCard.NOTIFICATIONS).status)
    }

    /** Home can't open the phone's settings out of nowhere, and someone switched movement off on purpose. */
    @Test
    fun homeLeavesBlockedAndSwitchedOffCardsAlone() {
        val state = ui(
            nothingYet.copy(notificationsNeedPermission = false, movementGranted = true, widgetPinSupported = false),
            movementEnabled = false,
        )
        assertNull(state.homePrompt)
    }
}
