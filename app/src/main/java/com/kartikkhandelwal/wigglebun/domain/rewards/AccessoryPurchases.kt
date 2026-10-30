package com.kartikkhandelwal.wigglebun.domain.rewards

import com.kartikkhandelwal.wigglebun.domain.model.AccessoryId

/** Ledger keys for accessories (prices live on [AccessoryId]); like pets, each is paid for once. */
object AccessoryPurchases {
    const val PREFIX = "accessory:"

    fun key(item: AccessoryId): String = "$PREFIX${item.key}"

    fun owned(keys: Collection<String>): Set<AccessoryId> =
        keys.mapNotNullTo(mutableSetOf()) { if (it.startsWith(PREFIX)) AccessoryId.fromKey(it.removePrefix(PREFIX)) else null }
}
