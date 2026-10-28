package com.kartikkhandelwal.wigglebun.domain.rewards

import com.kartikkhandelwal.wigglebun.domain.model.PetId

/**
 * Adoption prices in sparks (CLAUDE.md §5.4) and their ledger keys. The prices are placeholders:
 * final prices are an open decision (CLAUDE.md §15).
 */
object PetPrices {
    fun of(pet: PetId): Int = when (pet) {
        PetId.BUN -> 0
        PetId.PIP -> 150
        PetId.TOAST -> 200
        PetId.NIMBUS -> 300
    }

    /** One unlock row per pet: the UNIQUE ledger key means a pet can never be paid for twice. */
    fun unlockKey(pet: PetId): String = "$UNLOCK_PREFIX${pet.key}"

    /** The pet an unlock key paid for, or null for any other ledger key. */
    fun petForUnlockKey(key: String): PetId? =
        if (key.startsWith(UNLOCK_PREFIX)) PetId.fromKey(key.removePrefix(UNLOCK_PREFIX)) else null

    /** Owned pets: the starter pet plus every pet with an unlock row. */
    fun ownedPets(unlockKeys: Collection<String>): Set<PetId> =
        setOf(PetId.Starter) + unlockKeys.mapNotNull(::petForUnlockKey)

    const val UNLOCK_PREFIX = "unlock:"
}
