package com.kartikkhandelwal.wigglebun.domain.model

/** Where an accessory sits on a pet; a pet wears at most one accessory per slot. */
enum class AccessorySlot { HEAD, FACE, NECK }

/**
 * Accessories (CLAUDE.md §6.5): bought once with sparks, worn by any pet. Art and anchors in
 * design/accessories/acc_<key>.svg and ui/pet/AccessorySpecs.
 */
enum class AccessoryId(val key: String, val slot: AccessorySlot) {
    FLOWER_CLIP("flower_clip", AccessorySlot.HEAD),
    ROUND_GLASSES("round_glasses", AccessorySlot.FACE),
    KNIT_SCARF("knit_scarf", AccessorySlot.NECK),
    BEANIE("beanie", AccessorySlot.HEAD),
}
