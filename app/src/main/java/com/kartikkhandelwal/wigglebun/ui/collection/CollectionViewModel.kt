package com.kartikkhandelwal.wigglebun.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartikkhandelwal.wigglebun.data.repo.AdoptResult
import com.kartikkhandelwal.wigglebun.data.repo.PetRepository
import com.kartikkhandelwal.wigglebun.data.repo.WalletRepository
import com.kartikkhandelwal.wigglebun.domain.model.AccessoryId
import com.kartikkhandelwal.wigglebun.domain.model.PetId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val petRepository: PetRepository,
    walletRepository: WalletRepository,
) : ViewModel() {

    private val selection = MutableStateFlow<SheetSelection?>(null)
    private val busy = MutableStateFlow(false)
    private val tab = MutableStateFlow(CollectionTab.PETS)
    private val accessorySelection = MutableStateFlow<AccessorySelection?>(null)

    private data class Ui(val tab: CollectionTab, val sheet: SheetSelection?, val accessory: AccessorySelection?, val busy: Boolean)

    private val ui = combine(tab, selection, accessorySelection, busy, ::Ui)

    /** Null until the ledger and settings have loaded. */
    val state: StateFlow<CollectionUiState?> = combine(
        combine(petRepository.owned, petRepository.ownedAccessories, petRepository.outfits, ::Triple),
        petRepository.activePet,
        walletRepository.balance,
        ui,
    ) { (owned, ownedAccessories, outfits), active, sparks, ui ->
        toCollectionUiState(owned, active, sparks, ui.sheet, ui.busy, ownedAccessories, outfits, ui.tab, ui.accessory)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun selectTab(selected: CollectionTab) {
        tab.value = selected
    }

    fun openAccessory(item: AccessoryId) {
        accessorySelection.value = AccessorySelection(item, AccessoryStep.DETAILS)
    }

    fun closeAccessory() {
        accessorySelection.value = null
    }

    /** "Buy for 125": ask first, nothing is charged yet. */
    fun askToBuy(item: AccessoryId) {
        accessorySelection.value = AccessorySelection(item, AccessoryStep.CONFIRM)
    }

    fun cancelBuy(item: AccessoryId) {
        accessorySelection.value = AccessorySelection(item, AccessoryStep.DETAILS)
    }

    /** Pays once (the ledger key makes a double tap harmless) and puts it on the pet with you. */
    fun confirmBuy(item: AccessoryId) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            petRepository.buy(item)
            // Bought: the details now say "Take off". Not enough sparks (the balance changed
            // meanwhile): the details say how many are missing.
            accessorySelection.value = AccessorySelection(item, AccessoryStep.DETAILS)
            busy.value = false
        }
    }

    fun wear(item: AccessoryId) {
        viewModelScope.launch { petRepository.wear(item) }
    }

    fun takeOff(item: AccessoryId) {
        viewModelScope.launch { petRepository.takeOff(item) }
    }

    fun openPet(pet: PetId) {
        selection.value = SheetSelection(pet, SheetStep.DETAILS)
    }

    fun closeSheet() {
        selection.value = null
    }

    /** Makes an owned pet the one "with you" and closes the sheet. */
    fun choose(pet: PetId) {
        viewModelScope.launch {
            petRepository.choose(pet)
            selection.value = null
        }
    }

    /** "Adopt for 150": ask first, nothing is charged yet. */
    fun askToAdopt(pet: PetId) {
        selection.value = SheetSelection(pet, SheetStep.CONFIRM)
    }

    fun cancelAdopt(pet: PetId) {
        selection.value = SheetSelection(pet, SheetStep.DETAILS)
    }

    /**
     * Pays and welcomes the pet. A second tap while saving is ignored here, and the ledger's
     * unique key means it could never charge twice anyway.
     */
    fun confirmAdopt(pet: PetId) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            val step = when (petRepository.adopt(pet)) {
                AdoptResult.Adopted, AdoptResult.AlreadyOwned -> SheetStep.ADOPTED
                // The balance changed meanwhile: back to the details, which now say how many are missing.
                is AdoptResult.NotEnoughSparks -> SheetStep.DETAILS
            }
            selection.value = SheetSelection(pet, step)
            busy.value = false
        }
    }
}
