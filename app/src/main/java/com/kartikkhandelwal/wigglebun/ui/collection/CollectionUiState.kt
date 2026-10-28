package com.kartikkhandelwal.wigglebun.ui.collection

import com.kartikkhandelwal.wigglebun.domain.model.ActivePet
import com.kartikkhandelwal.wigglebun.domain.model.PetId
import com.kartikkhandelwal.wigglebun.domain.rewards.PetPrices

/** Where a pet stands in the collection. */
enum class PetStatus { ACTIVE, OWNED, LOCKED }

data class CollectionPetUi(
    val id: PetId,
    val status: PetStatus,
    /** Sparks to adopt; 0 for the starter pet. */
    val price: Int,
) {
    /** For display: the user's name for Bun (M9), every other pet's own name. */
    fun asActivePet(chosenName: String?) = ActivePet(id, chosenName)
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
): CollectionUiState {
    val pets = PetId.entries.sortedBy(PetPrices::of).map { id ->
        val status = when {
            id == active.id -> PetStatus.ACTIVE
            id in owned -> PetStatus.OWNED
            else -> PetStatus.LOCKED
        }
        CollectionPetUi(id, status, PetPrices.of(id))
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
    return CollectionUiState(sparks, pets, active.chosenName, sheet)
}
