package com.kartikkhandelwal.wigglebun.ui.collection

import com.kartikkhandelwal.wigglebun.domain.model.ActivePet
import com.kartikkhandelwal.wigglebun.domain.model.PetId
import com.kartikkhandelwal.wigglebun.domain.rewards.PetPrices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CollectionMapperTest {

    private fun state(owned: Set<PetId>, active: PetId, sparks: Int, sheetFor: PetId? = null) = toCollectionUiState(
        owned = owned,
        active = ActivePet(active, chosenName = null),
        sparks = sparks,
        selection = sheetFor?.let { SheetSelection(it, SheetStep.DETAILS) },
    )

    @Test
    fun gridListsEveryPetCheapestFirstWithItsStatus() {
        val s = state(setOf(PetId.BUN, PetId.TOAST), active = PetId.TOAST, sparks = 0)
        assertEquals(listOf(PetId.BUN, PetId.PIP, PetId.TOAST, PetId.NIMBUS), s.pets.map { it.id })
        assertEquals(listOf(PetStatus.OWNED, PetStatus.LOCKED, PetStatus.ACTIVE, PetStatus.LOCKED), s.pets.map { it.status })
        assertEquals(listOf(0, 150, 200, 300), s.pets.map { it.price })
        assertNull(s.sheet)
    }

    @Test
    fun sheetButtonFollowsStatusAndBalance() {
        val onlyBun = setOf(PetId.BUN)
        assertEquals(SheetAction.WithYou, state(onlyBun, PetId.BUN, 0, sheetFor = PetId.BUN).sheet?.action)
        assertEquals(SheetAction.Adopt(150), state(onlyBun, PetId.BUN, 150, sheetFor = PetId.PIP).sheet?.action)
        assertEquals(SheetAction.NeedMore(140), state(onlyBun, PetId.BUN, 160, sheetFor = PetId.NIMBUS).sheet?.action)
        assertEquals(SheetAction.Choose, state(setOf(PetId.BUN, PetId.PIP), PetId.PIP, 0, sheetFor = PetId.BUN).sheet?.action)
    }

    @Test
    fun pricesArePlaceholdersInOnePlace() {
        assertEquals(listOf(0, 150, 200, 300), PetId.entries.map(PetPrices::of))
        assertEquals("unlock:pip", PetPrices.unlockKey(PetId.PIP))
        assertEquals(PetId.NIMBUS, PetPrices.petForUnlockKey("unlock:nimbus"))
        assertNull(PetPrices.petForUnlockKey("break:2026-10-28:WALK"))
        assertEquals(setOf(PetId.BUN, PetId.TOAST), PetPrices.ownedPets(listOf("unlock:toast", "unlock:unknown")))
    }
}
