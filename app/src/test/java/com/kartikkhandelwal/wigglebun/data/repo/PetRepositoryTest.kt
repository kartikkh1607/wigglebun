package com.kartikkhandelwal.wigglebun.data.repo

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kartikkhandelwal.wigglebun.data.prefs.PrefsKeys
import com.kartikkhandelwal.wigglebun.domain.model.ActivePet
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Instant

/** The collection (M8): owned and active pets, adopting with sparks, names. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class)
class PetRepositoryTest {

    private val app = TestDataLayer(ApplicationProvider.getApplicationContext())
    private val pets = app.pets

    @After
    fun tearDown() = app.close()

    private val pipPrice = PetPrices.of(PetId.PIP)

    private fun earn(amount: Int) = runBlocking {
        app.wallet.award("break:test:$amount", amount, SparkReason.BREAK, Instant.now())
    }

    @Test
    fun starterPetIsOwnedAndActiveFromTheStart() = runBlocking {
        assertEquals(setOf(PetId.BUN), pets.owned.first())
        assertEquals(ActivePet.Default, pets.activePet.first())
    }

    @Test
    fun adoptingChargesOnceAndMakesThePetActive() = runBlocking {
        earn(pipPrice + 50)
        val redrawsBefore = app.widgetUpdates.count
        assertEquals(AdoptResult.Adopted, pets.adopt(PetId.PIP))
        assertEquals(setOf(PetId.BUN, PetId.PIP), pets.owned.first())
        assertEquals(PetId.PIP, pets.activePet.first().id)
        assertEquals(50, app.balance())
        assertTrue("the widget must show the new pet", app.widgetUpdates.count > redrawsBefore)

        // Again (a double tap): nothing more is charged.
        assertEquals(AdoptResult.AlreadyOwned, pets.adopt(PetId.PIP))
        assertEquals(50, app.balance())
    }

    @Test
    fun adoptingWithoutEnoughSparksChangesNothing() = runBlocking {
        earn(pipPrice - 30)
        assertEquals(AdoptResult.NotEnoughSparks(missing = 30), pets.adopt(PetId.PIP))
        assertEquals(setOf(PetId.BUN), pets.owned.first())
        assertEquals(PetId.BUN, pets.activePet.first().id)
        assertEquals(pipPrice - 30, app.balance())
    }

    @Test
    fun theStarterPetIsFreeAndNeverCharged() = runBlocking {
        assertEquals(AdoptResult.AlreadyOwned, pets.adopt(PetId.BUN))
        assertTrue(app.ledger().isEmpty())
    }

    @Test
    fun choosingSwitchesOnlyBetweenOwnedPets() = runBlocking {
        earn(500)
        pets.adopt(PetId.TOAST)
        assertFalse("Nimbus isn't adopted", pets.choose(PetId.NIMBUS))
        assertEquals(PetId.TOAST, pets.activePet.first().id)

        val redrawsBefore = app.widgetUpdates.count
        assertTrue(pets.choose(PetId.BUN))
        assertEquals(PetId.BUN, pets.activePet.first().id)
        assertTrue(app.widgetUpdates.count > redrawsBefore)
    }

    /** E.g. settings restored without the ledger: never show a pet that wasn't paid for. */
    @Test
    fun aStoredPetThatIsNotOwnedFallsBackToTheStarter() = runBlocking {
        app.settings.setActivePet(PetId.NIMBUS)
        assertEquals(PetId.BUN, pets.activePet.first().id)
    }

    /** The name given in onboarding belongs to Bun; other pets keep their own names. */
    @Test
    fun theChosenNameOnlyAppliesToTheStarterPet() = runBlocking {
        app.dataStore.edit { it[PrefsKeys.PetName] = "Mochi" }
        assertEquals("Mochi", pets.activePet.first().customName)

        earn(pipPrice)
        pets.adopt(PetId.PIP)
        val pip = pets.activePet.first()
        assertEquals(PetId.PIP, pip.id)
        assertNull(pip.customName)
    }

    /** Two taps on "Adopt" racing through the repository: one charge, both end on Pip. */
    @Test
    fun racingAdoptsChargeOnce() = runBlocking {
        earn(pipPrice)
        val results = (1..2).map { async(Dispatchers.IO) { pets.adopt(PetId.PIP) } }.awaitAll()
        assertEquals(listOf(AdoptResult.Adopted, AdoptResult.AlreadyOwned).sortedBy { it.toString() }, results.sortedBy { it.toString() })
        assertEquals(0, app.balance())
        assertEquals(PetId.PIP, pets.activePet.first().id)
    }
}
