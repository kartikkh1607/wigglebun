package com.kartikkhandelwal.wigglebun.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartikkhandelwal.wigglebun.data.repo.AdoptResult
import com.kartikkhandelwal.wigglebun.data.repo.PetRepository
import com.kartikkhandelwal.wigglebun.data.repo.WalletRepository
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

    /** Null until the ledger and settings have loaded. */
    val state: StateFlow<CollectionUiState?> = combine(
        petRepository.owned,
        petRepository.activePet,
        walletRepository.balance,
        selection,
        busy,
    ) { owned, active, sparks, sel, isBusy ->
        toCollectionUiState(owned, active, sparks, sel, isBusy)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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
