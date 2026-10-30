package com.kartikkhandelwal.wigglebun.domain.model

/** Where an accessory sits on a pet; a pet wears at most one accessory per slot. */
enum class AccessorySlot { HEAD, FACE, NECK }

/**
 * Accessories (CLAUDE.md §6.5): bought once with sparks, worn by any pet. [key] is stored
 * (ledger keys, worn sets), never renamed. [price] in sparks. Art and anchors in
 * design/accessories/acc_<key>.svg and ui/pet/AccessorySpecs.
 */
enum class AccessoryId(val key: String, val slot: AccessorySlot, val price: Int) {
    FLOWER_CLIP("flower_clip", AccessorySlot.HEAD, price = 50),
    ROUND_GLASSES("round_glasses", AccessorySlot.FACE, price = 75),
    KNIT_SCARF("knit_scarf", AccessorySlot.NECK, price = 100),
    BEANIE("beanie", AccessorySlot.HEAD, price = 125),
    ;

    companion object {
        fun fromKey(key: String?): AccessoryId? = entries.firstOrNull { it.key == key }
    }
}

/** What a pet wears: at most one accessory per slot, in drawing-independent slot order. */
@JvmInline
value class Outfit(val items: Set<AccessoryId>) {
    /** Wearing [item] replaces whatever the pet had in the same slot. */
    fun wearing(item: AccessoryId): Outfit = Outfit(items.filterTo(mutableSetOf()) { it.slot != item.slot } + item)

    fun without(item: AccessoryId): Outfit = Outfit(items - item)

    /** Only what is owned, one per slot (a restore or a bad write can't show more). */
    fun owned(owned: Set<AccessoryId>): Outfit =
        Outfit(items.filter { it in owned }.groupBy { it.slot }.mapNotNullTo(mutableSetOf()) { (_, inSlot) -> inSlot.firstOrNull() })

    val list: List<AccessoryId> get() = items.sortedBy { it.ordinal }

    companion object {
        val None = Outfit(emptySet())
    }
}
