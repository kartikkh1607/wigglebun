package com.kartikkhandelwal.wigglebun.data.repo

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kartikkhandelwal.wigglebun.domain.model.AccessoryId
import com.kartikkhandelwal.wigglebun.domain.model.Outfit
import com.kartikkhandelwal.wigglebun.domain.model.PetId
import com.kartikkhandelwal.wigglebun.domain.model.SparkReason
import com.kartikkhandelwal.wigglebun.domain.rewards.PetPrices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Instant

/** Accessories (CLAUDE.md §6.5): bought once with sparks, worn by any pet, one per slot. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class)
class AccessoryPurchaseTest {

    private val app = TestDataLayer(ApplicationProvider.getApplicationContext())
    private val pets = app.pets

    @After
    fun tearDown() = app.close()

    private fun earn(amount: Int) = runBlocking {
        app.wallet.award("break:test:$amount", amount, SparkReason.BREAK, Instant.now())
    }

    private fun wearing() = runBlocking { pets.activePet.first().wearing }

    @Test
    fun buyingChargesOnceAndPutsItOnThePetWithYou() = runBlocking {
        earn(AccessoryId.BEANIE.price + 10)
        val redrawsBefore = app.widgetUpdates.count
        assertEquals(BuyResult.Bought, pets.buy(AccessoryId.BEANIE))
        assertEquals(10, app.balance())
        assertEquals(listOf(AccessoryId.BEANIE), wearing())
        assertEquals(setOf(AccessoryId.BEANIE), pets.ownedAccessories.first())

        // A double tap: nothing more is charged.
        assertEquals(BuyResult.AlreadyOwned, pets.buy(AccessoryId.BEANIE))
        assertEquals(10, app.balance())
        assertEquals(1, app.ledger().count { it.key == "accessory:beanie" })
        // The widget shows no accessories, so buying one never redraws it.
        assertEquals(redrawsBefore, app.widgetUpdates.count)
    }

    @Test
    fun notEnoughSparksChangesNothing() = runBlocking {
        earn(AccessoryId.KNIT_SCARF.price - 1)
        assertEquals(BuyResult.NotEnoughSparks(missing = 1), pets.buy(AccessoryId.KNIT_SCARF))
        assertTrue(pets.ownedAccessories.first().isEmpty())
        assertTrue(wearing().isEmpty())
    }

    @Test
    fun racingBuysChargeOnce() = runBlocking {
        earn(AccessoryId.FLOWER_CLIP.price)
        val results = (1..2).map { async(Dispatchers.IO) { pets.buy(AccessoryId.FLOWER_CLIP) } }.awaitAll()
        assertEquals(setOf(BuyResult.Bought, BuyResult.AlreadyOwned), results.toSet())
        assertEquals(0, app.balance())
    }

    @Test
    fun aSlotHoldsOneAccessory() = runBlocking {
        earn(AccessoryId.FLOWER_CLIP.price + AccessoryId.BEANIE.price + AccessoryId.ROUND_GLASSES.price)
        pets.buy(AccessoryId.FLOWER_CLIP)
        pets.buy(AccessoryId.ROUND_GLASSES)
        pets.buy(AccessoryId.BEANIE) // HEAD again: replaces the flower clip
        assertEquals(listOf(AccessoryId.ROUND_GLASSES, AccessoryId.BEANIE), wearing())

        assertTrue(pets.wear(AccessoryId.FLOWER_CLIP))
        assertEquals(listOf(AccessoryId.FLOWER_CLIP, AccessoryId.ROUND_GLASSES), wearing())
        pets.takeOff(AccessoryId.ROUND_GLASSES)
        assertEquals(listOf(AccessoryId.FLOWER_CLIP), wearing())
    }

    @Test
    fun anAccessoryNotBoughtCannotBeWorn() = runBlocking {
        assertFalse(pets.wear(AccessoryId.BEANIE))
        assertTrue(wearing().isEmpty())
    }

    /** Bought once, worn by any pet; each pet keeps its own outfit. */
    @Test
    fun eachPetHasItsOwnOutfitFromTheSameAccessories() = runBlocking {
        earn(PetPrices.of(PetId.PIP) + AccessoryId.BEANIE.price)
        pets.buy(AccessoryId.BEANIE) // on Bun
        pets.adopt(PetId.PIP)
        assertTrue(wearing().isEmpty()) // Pip has its own (empty) outfit
        assertTrue(pets.wear(AccessoryId.BEANIE)) // no second charge
        assertEquals(listOf(AccessoryId.BEANIE), wearing())
        assertEquals(0, app.balance())

        pets.choose(PetId.BUN)
        assertEquals(listOf(AccessoryId.BEANIE), wearing())
    }

    /** E.g. settings restored without the ledger: never show an accessory that wasn't paid for. */
    @Test
    fun aStoredOutfitOnlyShowsBoughtAccessories() = runBlocking {
        app.settings.setOutfit(PetId.BUN, Outfit(setOf(AccessoryId.BEANIE, AccessoryId.KNIT_SCARF)))
        assertTrue(wearing().isEmpty())
        earn(AccessoryId.KNIT_SCARF.price)
        pets.buy(AccessoryId.KNIT_SCARF)
        assertEquals(listOf(AccessoryId.KNIT_SCARF), wearing())
    }
}
