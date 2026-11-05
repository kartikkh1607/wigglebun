package com.kartikkhandelwal.wigglebun.ui.collection

import com.kartikkhandelwal.wigglebun.domain.model.ActivePet
import com.kartikkhandelwal.wigglebun.domain.model.PetId
import com.kartikkhandelwal.wigglebun.domain.rewards.PetPrices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

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
        assertEquals(listOf(0, 75, 150, 300), s.pets.map { it.price })
        assertNull(s.sheet)
    }

    @Test
    fun sheetButtonFollowsStatusAndBalance() {
        val onlyBun = setOf(PetId.BUN)
        assertEquals(SheetAction.WithYou, state(onlyBun, PetId.BUN, 0, sheetFor = PetId.BUN).sheet?.action)
        assertEquals(SheetAction.Adopt(75), state(onlyBun, PetId.BUN, 75, sheetFor = PetId.PIP).sheet?.action)
        assertEquals(SheetAction.NeedMore(140), state(onlyBun, PetId.BUN, 160, sheetFor = PetId.NIMBUS).sheet?.action)
        assertEquals(SheetAction.Choose, state(setOf(PetId.BUN, PetId.PIP), PetId.PIP, 0, sheetFor = PetId.BUN).sheet?.action)
    }

    @Test
    fun pricesLiveInOnePlace() {
        assertEquals(listOf(0, 75, 150, 300), PetId.entries.map(PetPrices::of))
        assertEquals("unlock:pip", PetPrices.unlockKey(PetId.PIP))
        assertEquals(PetId.NIMBUS, PetPrices.petForUnlockKey("unlock:nimbus"))
        assertNull(PetPrices.petForUnlockKey("break:2026-10-28:WALK"))
        assertEquals(setOf(PetId.BUN, PetId.TOAST), PetPrices.ownedPets(listOf("unlock:toast", "unlock:unknown")))
    }

    @Test
    fun nextPetIsTheCheapestNotYetAdopted() {
        // The mockup: 30 sparks toward Pip, about 3 more days.
        assertEquals(NextPetUi(PetId.PIP, price = 75, saved = 30, daysLeft = 3), state(setOf(PetId.BUN), PetId.BUN, 30).nextPet)
        // Pip adopted: Toast is next, whoever is with you.
        assertEquals(PetId.TOAST, state(setOf(PetId.BUN, PetId.PIP), PetId.PIP, 0).nextPet?.id)
        // A pricier pet adopted first doesn't skip a cheaper one.
        assertEquals(PetId.PIP, state(setOf(PetId.BUN, PetId.NIMBUS), PetId.BUN, 0).nextPet?.id)
        assertNull(state(PetId.entries.toSet(), PetId.BUN, 500).nextPet)
    }

    @Test
    fun aNewUsersDaysEstimateUsesFifteenADay() {
        // No break day yet: 45 sparks still missing at 15 a day.
        val next = state(setOf(PetId.BUN), PetId.BUN, 30).nextPet!!
        assertEquals(3, next.daysLeft)
        assertFalse(next.aboutADay)
    }

    /** Install day: the first stretch paid 5 today. Pip shows about 5 days (fallback), not 14 (5 a day). */
    @Test
    fun installDayShowsAboutFiveDaysToPip() {
        val installDay = LocalDate.of(2026, 10, 26)
        val next = toCollectionUiState(
            setOf(PetId.BUN), ActivePet(PetId.BUN, null), sparks = 5, selection = null,
            breakRewardsByDay = mapOf(installDay to 5), today = installDay,
        ).nextPet!!
        assertEquals(5, next.daysLeft)
    }

    @Test
    fun theDaysEstimateFollowsTheUsersRecentBreakDays() {
        val monday = LocalDate.of(2026, 10, 26)
        // 40 sparks on each of the last three break days: Pip's missing 45 is 2 days, not 3.
        val heavy = (0L until 3L).associate { monday.plusDays(it) to 40 }
        val thursday = monday.plusDays(3)
        fun pace(sparks: Int) = toCollectionUiState(
            setOf(PetId.BUN), ActivePet(PetId.BUN, null), sparks, selection = null, breakRewardsByDay = heavy, today = thursday,
        ).nextPet!!
        val next = pace(30)
        assertEquals(2, next.daysLeft)
        assertFalse(next.aboutADay)

        // 40 missing at 40 a day: one day, so the card says "About a day".
        val close = pace(35)
        assertEquals(1, close.daysLeft)
        assertTrue(close.aboutADay)
        // Once affordable it says "Tap to meet", not "About a day".
        assertFalse(state(setOf(PetId.BUN), PetId.BUN, 75).nextPet!!.aboutADay)
    }

    @Test
    fun nextPetProgressStopsAtThePrice() {
        val rich = state(setOf(PetId.BUN), PetId.BUN, 200).nextPet!!
        assertEquals(75, rich.saved)
        assertEquals(0, rich.daysLeft)
        assertEquals(1f, rich.progress)
        assertTrue(rich.affordable)

        val saving = state(setOf(PetId.BUN), PetId.BUN, 18).nextPet!!
        assertFalse(saving.almostThere)
        assertFalse(saving.affordable)
        assertTrue(state(setOf(PetId.BUN), PetId.BUN, 19).nextPet!!.almostThere)
    }
}
