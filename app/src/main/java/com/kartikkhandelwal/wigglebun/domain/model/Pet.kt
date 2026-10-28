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
 * The pet that is "with you": shown on Home, in break sessions, notifications and the widget.
 * [chosenName] is the name the user gave in onboarding (M9); it belongs to Bun, the pet they named.
 * Every other pet keeps its own name.
 */
data class ActivePet(val id: PetId, val chosenName: String?) {
    /** The user's name for this pet, or null to use the pet's own name. */
    val customName: String? get() = chosenName.takeIf { id == PetId.Starter }

    companion object {
        val Default = ActivePet(PetId.Starter, chosenName = null)
    }
}
