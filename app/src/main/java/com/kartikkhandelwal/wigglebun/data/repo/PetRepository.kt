package com.kartikkhandelwal.wigglebun.data.repo

import com.kartikkhandelwal.wigglebun.data.db.SpendOutcome
import com.kartikkhandelwal.wigglebun.data.prefs.SettingsRepository
import com.kartikkhandelwal.wigglebun.data.time.AppClock
import com.kartikkhandelwal.wigglebun.domain.model.ActivePet
import com.kartikkhandelwal.wigglebun.domain.model.PetId
import com.kartikkhandelwal.wigglebun.domain.model.SparkReason
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

/**
 * The collection (CLAUDE.md §5.4): which pets are owned and which one is "with you".
 * Owned = the starter pet + every pet with an unlock row in the ledger (backed up with Room);
 * the active pet id lives in the backed-up settings DataStore.
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

    /** The one pet every screen, the notification and the widget show. */
    val activePet: Flow<ActivePet> = combine(settings.activePetId, settings.petName, owned) { id, name, owned ->
        // A stored pet that isn't owned (e.g. a partial restore) falls back to the starter pet.
        ActivePet(if (id in owned) id else PetId.Starter, chosenName = name)
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

    private suspend fun makeActive(pet: PetId) {
        settings.setActivePet(pet)
        widget.requestUpdate() // the widget shows the active pet's images
    }
}
