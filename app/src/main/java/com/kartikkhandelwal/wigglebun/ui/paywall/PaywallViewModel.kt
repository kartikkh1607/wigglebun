package com.kartikkhandelwal.wigglebun.ui.paywall

import android.app.Activity
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartikkhandelwal.wigglebun.R
import com.kartikkhandelwal.wigglebun.billing.BillingResult
import com.kartikkhandelwal.wigglebun.billing.OfferingsResult
import com.kartikkhandelwal.wigglebun.billing.PaywallOptions
import com.kartikkhandelwal.wigglebun.billing.PlanKind
import com.kartikkhandelwal.wigglebun.billing.PlanOption
import com.kartikkhandelwal.wigglebun.billing.PremiumRepository
import com.kartikkhandelwal.wigglebun.billing.PremiumState
import com.kartikkhandelwal.wigglebun.feedback.FeelEvent
import com.kartikkhandelwal.wigglebun.feedback.FeelEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The plans the store offers, as far as the paywall knows them. */
sealed interface PlansState {
    data object Loading : PlansState

    data class Ready(val options: PaywallOptions) : PlansState

    /** No plans to show: no connection, Play had a problem, or billing isn't set up in this build. */
    data object Problem : PlansState
}

/** One line above the button after a purchase or restore that didn't end in premium. */
enum class PaywallNotice(@StringRes val text: Int) {
    Pending(R.string.paywall_notice_pending),
    NotAllowed(R.string.paywall_notice_not_allowed),
    Network(R.string.paywall_notice_network),
    Store(R.string.paywall_notice_store),
    Error(R.string.paywall_notice_error),
    NothingToRestore(R.string.paywall_notice_nothing_to_restore),
}

data class PaywallUiState(
    val plans: PlansState = PlansState.Loading,
    /** The highlighted plan; yearly when the store offers it (CLAUDE.md §5.5). */
    val selected: PlanKind? = null,
    /** A purchase or restore is running: the buttons wait for it. */
    val busy: Boolean = false,
    val notice: PaywallNotice? = null,
) {
    val selectedOption: PlanOption?
        get() = (plans as? PlansState.Ready)?.options?.all?.firstOrNull { it.kind == selected }
}

/**
 * The paywall (CLAUDE.md §5.5, §6.8). Prices and the trial come from the store through
 * [PremiumRepository]; nothing here is hard-coded. Whether the app is locked afterwards is not
 * decided here (M10c).
 */
@HiltViewModel
class PaywallViewModel @Inject constructor(private val premium: PremiumRepository) : ViewModel() {

    private val _state = MutableStateFlow(PaywallUiState())
    val state: StateFlow<PaywallUiState> = _state.asStateFlow()

    /** The success tick after a purchase or restore, played once by the route. */
    private val feelEvents = FeelEvents()
    val feel = feelEvents.flow

    /** True once premium is active, however it got there (bought here, restored, or already owned). */
    val entitled: Flow<Boolean> = premium.premium.map { it is PremiumState.Entitled }.distinctUntilChanged()

    init {
        loadPlans()
    }

    fun loadPlans() {
        _state.update { it.copy(plans = PlansState.Loading, notice = null) }
        viewModelScope.launch {
            val result = premium.offerings()
            _state.update { state ->
                when (result) {
                    is OfferingsResult.Loaded -> {
                        val options = result.options
                        // Keep a choice already made (a reload after "Try again").
                        val kept = options.all.firstOrNull { it.kind == state.selected }
                        val default = options.yearly ?: options.monthly ?: options.lifetime
                        state.copy(plans = PlansState.Ready(options), selected = (kept ?: default)?.kind)
                    }
                    OfferingsResult.Empty, OfferingsResult.Unavailable, is OfferingsResult.Failed ->
                        state.copy(plans = PlansState.Problem, selected = null)
                }
            }
        }
    }

    fun onPick(kind: PlanKind) {
        _state.update { state ->
            if (state.busy || state.plans !is PlansState.Ready) state else state.copy(selected = kind, notice = null)
        }
    }

    /** Opens Play's purchase sheet for the selected plan. */
    fun onPurchase(activity: Activity) {
        val option = _state.value.selectedOption ?: return
        launchBilling(isRestore = false) { premium.purchase(activity, option) }
    }

    fun onRestore() = launchBilling(isRestore = true) { premium.restore() }

    private fun launchBilling(isRestore: Boolean, call: suspend () -> BillingResult) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, notice = null) }
        viewModelScope.launch {
            val notice = when (val result = call()) {
                is BillingResult.Success -> when {
                    result.state is PremiumState.Entitled -> {
                        feelEvents.send(FeelEvent.Success)
                        null
                    }
                    isRestore -> PaywallNotice.NothingToRestore
                    // Play said yes but the entitlement isn't active yet: it follows through `entitled`.
                    else -> PaywallNotice.Pending
                }
                // The user backed out of Play's sheet: say nothing.
                BillingResult.Cancelled -> null
                BillingResult.Pending -> PaywallNotice.Pending
                BillingResult.NotAllowed -> PaywallNotice.NotAllowed
                BillingResult.NetworkError -> PaywallNotice.Network
                BillingResult.StoreProblem -> PaywallNotice.Store
                is BillingResult.Error -> PaywallNotice.Error
            }
            _state.update { it.copy(busy = false, notice = notice) }
        }
    }
}
