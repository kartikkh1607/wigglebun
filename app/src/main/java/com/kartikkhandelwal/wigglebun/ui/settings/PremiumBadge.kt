package com.kartikkhandelwal.wigglebun.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartikkhandelwal.wigglebun.billing.AccessGate
import com.kartikkhandelwal.wigglebun.billing.AppAccess
import com.kartikkhandelwal.wigglebun.billing.PremiumRepository
import com.kartikkhandelwal.wigglebun.billing.PremiumState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** The chip on Settings' "Bun & you" card (CLAUDE.md §6.7). */
enum class PremiumBadge {
    /** A paid plan or lifetime. */
    Premium,

    /** In the free trial. */
    Trial,

    /** The app is locked: the chip is the way to the plans. */
    Unlock,
}

/**
 * Null shows no chip: nothing known yet, or a build that neither sells nor locks (a debug build
 * without a RevenueCat key, the closed test before a tester buys).
 */
fun premiumBadge(premium: PremiumState, access: AppAccess): PremiumBadge? = when {
    premium is PremiumState.Entitled -> if (premium.isTrial) PremiumBadge.Trial else PremiumBadge.Premium
    access == AppAccess.Locked -> PremiumBadge.Unlock
    else -> null
}

/** Its own small ViewModel, so Settings' ViewModel keeps to settings. */
@HiltViewModel
class PremiumBadgeViewModel @Inject constructor(premium: PremiumRepository, gate: AccessGate) : ViewModel() {
    val badge: StateFlow<PremiumBadge?> = combine(premium.premium, gate.access, ::premiumBadge)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
