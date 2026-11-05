package com.kartikkhandelwal.wigglebun.ui.collection

import com.kartikkhandelwal.wigglebun.domain.model.AccessoryId
import com.kartikkhandelwal.wigglebun.domain.model.ActivePet
import com.kartikkhandelwal.wigglebun.domain.model.Outfit
import com.kartikkhandelwal.wigglebun.domain.model.PetId
import com.kartikkhandelwal.wigglebun.domain.rewards.PetPrices
import com.kartikkhandelwal.wigglebun.domain.rewards.SparksPace
import java.time.LocalDate

/** Where a pet stands in the collection. */
enum class PetStatus { ACTIVE, OWNED, LOCKED }

data class CollectionPetUi(
    val id: PetId,
    val status: PetStatus,
    /** Sparks to adopt; 0 for the starter pet. */
    val price: Int,
    /** What this pet wears (owned pets only). */
    val wearing: List<AccessoryId> = emptyList(),
) {
    /** For display: the user's name for Bun (M9), every other pet's own name, and its outfit. */
    fun asActivePet(chosenName: String?) = ActivePet(id, chosenName, wearing)
}

/**
 * The "next pet" card above the grid (design/ui_v2/Collection.dc.html): the cheapest pet not yet
 * adopted and how far the sparks are toward it.
 */
data class NextPetUi(
    val id: PetId,
    val price: Int,
    /** Sparks toward it, at most [price]. */
    val saved: Int,
    /** Days of breaks still needed at the user's own pace ([SparksPace]); 0 once affordable. */
    val daysLeft: Int,
) {
    val affordable: Boolean get() = saved >= price

    /** One day or less to go: "About a day of breaks to go" rather than "About 1 more day" (D4). */
    val aboutADay: Boolean get() = !affordable && daysLeft <= 1

    /** 0..1 for the progress bar. */
    val progress: Float get() = if (price == 0) 1f else saved.toFloat() / price

    /** Under a quarter of the price the card says "Saving up for Pip", from there "Pip is almost yours" (the mockup's 30 / 75). */
    val almostThere: Boolean get() = saved * 4 >= price
}

/** The sheet's steps: the pet's details, "Adopt Pip?" and the welcome celebration. */
enum class SheetStep { DETAILS, CONFIRM, ADOPTED }

/** What the sheet's one main button does (or says, when it can't be tapped). */
sealed interface SheetAction {
    data object Choose : SheetAction

    /** Already the active pet: shown disabled. */
    data object WithYou : SheetAction

    data class Adopt(val price: Int) : SheetAction

    /** Locked and the sparks don't cover it yet: shown disabled. */
    data class NeedMore(val missing: Int) : SheetAction
}

data class PetSheetUi(
    val pet: CollectionPetUi,
    val step: SheetStep,
    val action: SheetAction,
    /** An adoption is being saved: buttons ignore taps. */
    val busy: Boolean,
)

data class CollectionUiState(
    val sparks: Int,
    /** In collection order (cheapest first, the starter pet leading). */
    val pets: List<CollectionPetUi>,
    /** The name the user gave Bun (M9); other pets keep their own. */
    val chosenName: String?,
    val sheet: PetSheetUi?,
    val tab: CollectionTab = CollectionTab.PETS,
    /** The pet that is with you, wearing its outfit: accessories are tried on and worn by it. */
    val activePet: ActivePet = ActivePet.Default,
    val accessories: List<AccessoryUi> = emptyList(),
    val accessorySheet: AccessorySheetUi? = null,
    /** Null once every pet is adopted. */
    val nextPet: NextPetUi? = null,
)

/** Which pet's sheet is open, and at which step. */
data class SheetSelection(val pet: PetId, val step: SheetStep)

/** Pure mapping from the repositories to the screen (unit-tested). */
fun toCollectionUiState(
    owned: Set<PetId>,
    active: ActivePet,
    sparks: Int,
    selection: SheetSelection?,
    busy: Boolean = false,
    ownedAccessories: Set<AccessoryId> = emptySet(),
    outfits: Map<PetId, Outfit> = emptyMap(),
    tab: CollectionTab = CollectionTab.PETS,
    accessorySelection: AccessorySelection? = null,
    /** Break rewards per break day, for the next pet's days estimate ([SparksPace]). */
    breakRewardsByDay: Map<LocalDate, Int> = emptyMap(),
    /** Today's break day: it isn't over, so it doesn't count toward the pace. */
    today: LocalDate = LocalDate.MAX,
): CollectionUiState {
    val pets = PetId.entries.sortedBy(PetPrices::of).map { id ->
        val status = when {
            id == active.id -> PetStatus.ACTIVE
            id in owned -> PetStatus.OWNED
            else -> PetStatus.LOCKED
        }
        val wearing = if (status == PetStatus.LOCKED) emptyList() else outfits[id]?.owned(ownedAccessories)?.list.orEmpty()
        CollectionPetUi(id, status, PetPrices.of(id), wearing)
    }
    val sheet = selection?.let { sel ->
        val pet = pets.first { it.id == sel.pet }
        val action = when (pet.status) {
            PetStatus.ACTIVE -> SheetAction.WithYou
            PetStatus.OWNED -> SheetAction.Choose
            PetStatus.LOCKED -> if (sparks >= pet.price) SheetAction.Adopt(pet.price) else SheetAction.NeedMore(pet.price - sparks)
        }
        PetSheetUi(pet, sel.step, action, busy)
    }
    val accessories = accessoryList(ownedAccessories, active)
    val accessorySheet = accessorySelection?.let { accessorySheet(accessories, active, sparks, it, busy) }
    val nextPet = pets.firstOrNull { it.status == PetStatus.LOCKED }?.let { pet ->
        val daysLeft = SparksPace.daysToEarn(pet.price - sparks, SparksPace.perDay(breakRewardsByDay, today))
        NextPetUi(pet.id, pet.price, saved = sparks.coerceIn(0, pet.price), daysLeft = daysLeft)
    }
    return CollectionUiState(sparks, pets, active.chosenName, sheet, tab, active, accessories, accessorySheet, nextPet)
}
