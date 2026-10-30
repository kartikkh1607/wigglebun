package com.kartikkhandelwal.wigglebun.data.repo

import com.kartikkhandelwal.wigglebun.data.db.SpendOutcome
import com.kartikkhandelwal.wigglebun.data.prefs.SettingsRepository
import com.kartikkhandelwal.wigglebun.data.time.AppClock
import com.kartikkhandelwal.wigglebun.domain.model.AccessoryId
import com.kartikkhandelwal.wigglebun.domain.model.ActivePet
import com.kartikkhandelwal.wigglebun.domain.model.Outfit
import com.kartikkhandelwal.wigglebun.domain.model.PetId
import com.kartikkhandelwal.wigglebun.domain.model.SparkReason
import com.kartikkhandelwal.wigglebun.domain.rewards.AccessoryPurchases
import com.kartikkhandelwal.wigglebun.domain.rewards.PetPrices
import com.kartikkhandelwal.wigglebun.widget.WidgetRefresher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AdoptResult {
    data object Adopted : AdoptResult

    /** Already paid for (a double tap, or the starter pet): nothing charged, it is chosen. */
    data object AlreadyOwned : AdoptResult

    data class NotEnoughSparks(val missing: Int) : AdoptResult
}

sealed interface BuyResult {
    /** Paid for and now worn by the active pet. */
    data object Bought : BuyResult

    /** Already paid for (a double tap): nothing charged, now worn. */
    data object AlreadyOwned : BuyResult

    data class NotEnoughSparks(val missing: Int) : BuyResult
}

/**
 * The collection (CLAUDE.md §5.4, §6.5): which pets and accessories are owned, which pet is
 * "with you" and what each pet wears. Owned = the starter pet + every pet or accessory with a
 * purchase row in the ledger (backed up with Room); the active pet id and outfits live in the
 * backed-up settings DataStore. An accessory is bought once and can be worn by every pet.
 */
@Singleton
class PetRepository @Inject constructor(
    private val wallet: WalletRepository,
    private val settings: SettingsRepository,
    private val clock: AppClock,
    private val widget: WidgetRefresher,
) {
    val owned: Flow<Set<PetId>> = wallet.keysStartingWith(PetPrices.UNLOCK_PREFIX)
        .map(PetPrices::ownedPets)
        .distinctUntilChanged()

    val ownedAccessories: Flow<Set<AccessoryId>> = wallet.keysStartingWith(AccessoryPurchases.PREFIX)
        .map(AccessoryPurchases::owned)
        .distinctUntilChanged()

    /** What each pet wears, limited to owned accessories. */
    val outfits: Flow<Map<PetId, Outfit>> = combine(settings.outfits, ownedAccessories) { outfits, owned ->
        outfits.mapValues { (_, outfit) -> outfit.owned(owned) }
    }.distinctUntilChanged()

    /** The one pet every screen, the notification and the widget show, with what it wears. */
    val activePet: Flow<ActivePet> = combine(settings.activePetId, settings.petName, owned, outfits) { id, name, owned, outfits ->
        // A stored pet that isn't owned (e.g. a partial restore) falls back to the starter pet.
        val pet = if (id in owned) id else PetId.Starter
        ActivePet(pet, chosenName = name, wearing = outfits[pet]?.list.orEmpty())
    }.distinctUntilChanged()

    /** Makes an owned pet the active one. Returns false (and changes nothing) for a locked pet. */
    suspend fun choose(pet: PetId): Boolean {
        if (pet !in owned.first()) return false
        makeActive(pet)
        return true
    }

    /** Pays for [pet] once and makes it the active pet; refuses if the sparks don't cover it. */
    suspend fun adopt(pet: PetId): AdoptResult {
        val result = if (pet == PetId.Starter) {
            AdoptResult.AlreadyOwned
        } else {
            when (val outcome = wallet.spend(PetPrices.unlockKey(pet), PetPrices.of(pet), SparkReason.UNLOCK, clock.instant())) {
                SpendOutcome.Spent -> AdoptResult.Adopted
                SpendOutcome.AlreadyPaid -> AdoptResult.AlreadyOwned
                is SpendOutcome.Insufficient -> return AdoptResult.NotEnoughSparks(outcome.missing)
            }
        }
        makeActive(pet)
        return result
    }

    /** Pays for [item] once and puts it on the active pet; refuses if the sparks don't cover it. */
    suspend fun buy(item: AccessoryId): BuyResult {
        val result = when (
            val outcome = wallet.spend(AccessoryPurchases.key(item), item.price, SparkReason.UNLOCK, clock.instant())
        ) {
            SpendOutcome.Spent -> BuyResult.Bought
            SpendOutcome.AlreadyPaid -> BuyResult.AlreadyOwned
            is SpendOutcome.Insufficient -> return BuyResult.NotEnoughSparks(outcome.missing)
        }
        wear(item)
        return result
    }

    /** Puts an owned [item] on the active pet (replacing its slot). Returns false for one not bought. */
    suspend fun wear(item: AccessoryId): Boolean {
        if (item !in ownedAccessories.first()) return false
        val pet = activePet.first().id
        settings.setOutfit(pet, outfitOf(pet).wearing(item))
        return true
    }

    suspend fun takeOff(item: AccessoryId) {
        val pet = activePet.first().id
        settings.setOutfit(pet, outfitOf(pet).without(item))
    }

    private suspend fun outfitOf(pet: PetId): Outfit = outfits.first()[pet] ?: Outfit.None

    private suspend fun makeActive(pet: PetId) {
        settings.setActivePet(pet)
        widget.requestUpdate() // the widget shows the active pet's images
    }
}
