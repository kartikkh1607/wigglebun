package com.kartikkhandelwal.wigglebun.ui.collection

import com.kartikkhandelwal.wigglebun.domain.model.AccessoryId
import com.kartikkhandelwal.wigglebun.domain.model.ActivePet
import com.kartikkhandelwal.wigglebun.domain.model.Outfit

/** The Collection's two lists. */
enum class CollectionTab { PETS, ACCESSORIES }

/** Where an accessory stands: not bought, bought, or worn by the pet that is with you. */
enum class AccessoryStatus { LOCKED, OWNED, WORN }

data class AccessoryUi(val id: AccessoryId, val status: AccessoryStatus) {
    val price: Int get() = id.price
}

/** The accessory sheet's steps: details (try-on), and "Buy Beanie?". */
enum class AccessoryStep { DETAILS, CONFIRM }

/** What the accessory sheet's main button does (or says, when it can't be tapped). */
sealed interface AccessoryAction {
    data class Buy(val price: Int) : AccessoryAction

    /** Not bought and the sparks don't cover it yet: shown disabled. */
    data class NeedMore(val missing: Int) : AccessoryAction

    /** Bought, not worn by the active pet. */
    data object Wear : AccessoryAction

    data object TakeOff : AccessoryAction
}

data class AccessorySheetUi(
    val item: AccessoryUi,
    val step: AccessoryStep,
    val action: AccessoryAction,
    /** The active pet trying the item on: its outfit with [item] in its slot. */
    val preview: ActivePet,
    val busy: Boolean,
)

/** Which accessory's sheet is open, and at which step. */
data class AccessorySelection(val item: AccessoryId, val step: AccessoryStep)

/** The accessories in shop order (cheapest first) for the pet that is with you. */
fun accessoryList(owned: Set<AccessoryId>, active: ActivePet): List<AccessoryUi> =
    AccessoryId.entries.sortedBy { it.price }.map { id ->
        val status = when {
            id in active.wearing -> AccessoryStatus.WORN
            id in owned -> AccessoryStatus.OWNED
            else -> AccessoryStatus.LOCKED
        }
        AccessoryUi(id, status)
    }

/** The active pet wearing its outfit with [item] in that slot (the try-on in cards and the sheet). */
fun ActivePet.tryingOn(item: AccessoryId): ActivePet = copy(wearing = Outfit(wearing.toSet()).wearing(item).list)

fun accessorySheet(
    items: List<AccessoryUi>,
    active: ActivePet,
    sparks: Int,
    selection: AccessorySelection,
    busy: Boolean,
): AccessorySheetUi {
    val item = items.first { it.id == selection.item }
    val action = when (item.status) {
        AccessoryStatus.WORN -> AccessoryAction.TakeOff
        AccessoryStatus.OWNED -> AccessoryAction.Wear
        AccessoryStatus.LOCKED -> if (sparks >= item.price) AccessoryAction.Buy(item.price) else AccessoryAction.NeedMore(item.price - sparks)
    }
    return AccessorySheetUi(item, selection.step, action, active.tryingOn(item.id), busy)
}
