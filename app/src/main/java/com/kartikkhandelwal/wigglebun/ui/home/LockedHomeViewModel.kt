package com.kartikkhandelwal.wigglebun.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartikkhandelwal.wigglebun.billing.OfferingsResult
import com.kartikkhandelwal.wigglebun.billing.PremiumRepository
import com.kartikkhandelwal.wigglebun.data.repo.PetRepository
import com.kartikkhandelwal.wigglebun.data.repo.StreakRepository
import com.kartikkhandelwal.wigglebun.domain.model.ActivePet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LockedHomeUiState(
    val pet: ActivePet = ActivePet.Default,
    /** Breaks done so far; the "saved" chip is left out at 0. */
    val totalBreaks: Int = 0,
    /** The yearly plan's offer as the store gives it; null until loaded or when there is none. */
    val offer: LockedOffer? = null,
)

/** The yearly plan in the store: its price text and the free trial's days, if the offer has one. */
data class LockedOffer(val price: String, val trialDays: Int?)

/**
 * Home while the app is locked (CLAUDE.md §5.5). The button never promises a free trial the
 * store's offer doesn't have: without one, or before the plans have loaded, it says "See plans".
 */
@HiltViewModel
class LockedHomeViewModel @Inject constructor(
    premium: PremiumRepository,
    pets: PetRepository,
    streak: StreakRepository,
) : ViewModel() {

    private val offer = MutableStateFlow<LockedOffer?>(null)

    val state: StateFlow<LockedHomeUiState> =
        combine(pets.activePet, streak.summary, offer) { pet, summary, offer ->
            LockedHomeUiState(pet = pet, totalBreaks = summary.totalBreaks, offer = offer)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), LockedHomeUiState())

    init {
        viewModelScope.launch {
            val yearly = (premium.offerings() as? OfferingsResult.Loaded)?.options?.yearly
            offer.value = yearly?.let { LockedOffer(price = it.price, trialDays = it.freeTrial?.days) }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
