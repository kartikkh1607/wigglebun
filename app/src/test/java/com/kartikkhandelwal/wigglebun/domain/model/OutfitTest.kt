package com.kartikkhandelwal.wigglebun.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class OutfitTest {
    @Test
    fun wearingReplacesTheSameSlotOnly() {
        val outfit = Outfit.None.wearing(AccessoryId.FLOWER_CLIP).wearing(AccessoryId.KNIT_SCARF).wearing(AccessoryId.BEANIE)
        assertEquals(listOf(AccessoryId.KNIT_SCARF, AccessoryId.BEANIE), outfit.list)
    }

    @Test
    fun ownedKeepsOneBoughtItemPerSlot() {
        val stored = Outfit(setOf(AccessoryId.FLOWER_CLIP, AccessoryId.BEANIE, AccessoryId.ROUND_GLASSES))
        assertEquals(listOf(AccessoryId.ROUND_GLASSES), stored.owned(setOf(AccessoryId.ROUND_GLASSES)).list)
        assertEquals(1, stored.owned(AccessoryId.entries.toSet()).items.count { it.slot == AccessorySlot.HEAD })
    }

    @Test
    fun v1PricesAndKeys() {
        assertEquals(listOf(50, 75, 100, 125), AccessoryId.entries.map { it.price })
        assertEquals(AccessoryId.BEANIE, AccessoryId.fromKey("beanie"))
    }
}
