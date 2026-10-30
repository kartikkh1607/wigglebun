package com.kartikkhandelwal.wigglebun.domain.model

/** The pets in the collection (CLAUDE.md §5.4). [key] is stored (settings, ledger keys), never renamed. */
enum class PetId(val key: String) {
    BUN("bun"),
    PIP("pip"),
    TOAST("toast"),
    NIMBUS("nimbus"),
    ;

    companion object {
        /** Bun is the starter pet: free and owned from the start. */
        val Starter = BUN

        fun fromKey(key: String?): PetId? = entries.firstOrNull { it.key == key }
    }
}

/**
 * Longest name the user can give their pet (M9 onboarding limits the field to it). At the default
 * font size on a 390 dp phone, a 12-letter name fits the Home timer card's mood chip on one line
 * ("Wigglebottom is getting tired"). Longer names, narrower phones and larger fonts still work:
 * sentences shorten the name with "…" rather than grow (see PetNameSentence).
 */
const val MAX_PET_NAME_LENGTH = 12

/**
 * The pet that is "with you": shown on Home, in break sessions, notifications and the widget.
 * [chosenName] is the name the user gave in onboarding (M9); it belongs to Bun, the pet they named.
 * Every other pet keeps its own name.
 */
data class ActivePet(
    val id: PetId,
    val chosenName: String?,
    /** What this pet wears (owned accessories only); drawn on Home, in sessions and the collection, not on the widget. */
    val wearing: List<AccessoryId> = emptyList(),
) {
    /** The user's name for this pet, or null to use the pet's own name. */
    val customName: String? get() = chosenName.takeIf { id == PetId.Starter }

    companion object {
        val Default = ActivePet(PetId.Starter, chosenName = null)
    }
}
