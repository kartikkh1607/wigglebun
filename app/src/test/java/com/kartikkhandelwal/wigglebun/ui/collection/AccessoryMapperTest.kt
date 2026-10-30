package com.kartikkhandelwal.wigglebun.ui.collection

import com.kartikkhandelwal.wigglebun.domain.model.AccessoryId
import com.kartikkhandelwal.wigglebun.domain.model.ActivePet
import com.kartikkhandelwal.wigglebun.domain.model.Outfit
import com.kartikkhandelwal.wigglebun.domain.model.PetId
import com.kartikkhandelwal.wigglebun.domain.rewards.AccessoryPurchases
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessoryMapperTest {

    private val bunInGlasses = ActivePet(PetId.BUN, null, wearing = listOf(AccessoryId.ROUND_GLASSES))
    private val owned = setOf(AccessoryId.FLOWER_CLIP, AccessoryId.ROUND_GLASSES)

    private fun state(sparks: Int, item: AccessoryId? = null) = toCollectionUiState(
        owned = setOf(PetId.BUN),
        active = bunInGlasses,
        sparks = sparks,
        selection = null,
        ownedAccessories = owned,
        outfits = mapOf(PetId.BUN to Outfit(setOf(AccessoryId.ROUND_GLASSES))),
        tab = CollectionTab.ACCESSORIES,
        accessorySelection = item?.let { AccessorySelection(it, AccessoryStep.DETAILS) },
    )

    @Test
    fun listIsCheapestFirstWithStatusForThePetWithYou() {
        val s = state(sparks = 0)
        assertEquals(AccessoryId.entries.sortedBy { it.price }, s.accessories.map { it.id })
        assertEquals(
            listOf(AccessoryStatus.OWNED, AccessoryStatus.WORN, AccessoryStatus.LOCKED, AccessoryStatus.LOCKED),
            s.accessories.map { it.status },
        )
        assertEquals(listOf(AccessoryId.ROUND_GLASSES), s.pets.first { it.id == PetId.BUN }.wearing)
    }

    @Test
    fun sheetButtonFollowsStatusAndBalance() {
        assertEquals(AccessoryAction.Wear, state(0, AccessoryId.FLOWER_CLIP).accessorySheet?.action)
        assertEquals(AccessoryAction.TakeOff, state(0, AccessoryId.ROUND_GLASSES).accessorySheet?.action)
        assertEquals(AccessoryAction.Buy(100), state(100, AccessoryId.KNIT_SCARF).accessorySheet?.action)
        assertEquals(AccessoryAction.NeedMore(35), state(90, AccessoryId.BEANIE).accessorySheet?.action)
    }

    /** The sheet shows the pet trying the item on: same slot replaced, the rest kept. */
    @Test
    fun previewTriesTheItemOn() {
        assertEquals(
            listOf(AccessoryId.ROUND_GLASSES, AccessoryId.BEANIE),
            state(0, AccessoryId.BEANIE).accessorySheet?.preview?.wearing,
        )
    }

    @Test
    fun lockedPetsWearNothing() {
        val s = toCollectionUiState(
            owned = setOf(PetId.BUN),
            active = ActivePet.Default,
            sparks = 0,
            selection = null,
            ownedAccessories = owned,
            outfits = mapOf(PetId.PIP to Outfit(owned)),
        )
        assertTrue(s.pets.first { it.id == PetId.PIP }.wearing.isEmpty())
    }

    @Test
    fun ledgerKeys() {
        assertEquals("accessory:beanie", AccessoryPurchases.key(AccessoryId.BEANIE))
        assertEquals(setOf(AccessoryId.BEANIE), AccessoryPurchases.owned(listOf("accessory:beanie", "unlock:pip", "accessory:hat")))
    }
}
