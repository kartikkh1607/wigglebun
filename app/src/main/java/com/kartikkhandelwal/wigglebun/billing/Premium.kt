package com.kartikkhandelwal.wigglebun.billing

import android.app.Activity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** RevenueCat entitlement identifiers. The dashboard's entitlement must use exactly this id. */
object Entitlements {
    const val PREMIUM = "premium"
}

/** Whether the user may use the app's core loop (CLAUDE.md §5.5). */
sealed interface PremiumState {
    /** Not known yet: no answer from RevenueCat and nothing cached on this phone. */
    data object Unknown : PremiumState

    data class Entitled(
        /** In the yearly plan's free trial. */
        val isTrial: Boolean,
        /** When the current period ends (trial end, renewal date); null for lifetime. */
        val expiresAt: Instant?,
        /** False once the user cancelled: it still runs until [expiresAt]. */
        val willRenew: Boolean,
        val isLifetime: Boolean,
    ) : PremiumState

    data object NotEntitled : PremiumState

    /** Billing isn't set up in this build (no RevenueCat key), so purchases can't be made. */
    data object Unavailable : PremiumState
}

enum class PlanKind { YEARLY, MONTHLY, LIFETIME }

enum class PeriodUnit { DAY, WEEK, MONTH, YEAR }

data class BillingPeriod(val value: Int, val unit: PeriodUnit) {
    /** Exact for days and weeks (Play's trials are P3D, P7D, P1W, …); months count 30 days, years 365. */
    val approximateDays: Int
        get() = value * when (unit) {
            PeriodUnit.DAY -> 1
            PeriodUnit.WEEK -> 7
            PeriodUnit.MONTH -> 30
            PeriodUnit.YEAR -> 365
        }
}

data class FreeTrial(val period: BillingPeriod, val days: Int)

/** One plan on the paywall, with what the store says about it. */
data class PlanOption(
    val kind: PlanKind,
    /** RevenueCat's package identifier, to find the package again when buying. */
    val packageId: String,
    /** The store's localised price, e.g. "$29.99" or "₹2,499.00"; never hard-coded. */
    val price: String,
    /** Billing period; null for lifetime. */
    val period: BillingPeriod?,
    /** The free phase the purchase would start with; null when the offer has none. */
    val freeTrial: FreeTrial?,
)

/** The current offering, split by package type. A missing package is null. */
data class PaywallOptions(
    val yearly: PlanOption?,
    val monthly: PlanOption?,
    val lifetime: PlanOption?,
) {
    val all: List<PlanOption> get() = listOfNotNull(yearly, monthly, lifetime)
}

sealed interface OfferingsResult {
    data class Loaded(val options: PaywallOptions) : OfferingsResult

    /** No current offering, or it has none of the three package types. */
    data object Empty : OfferingsResult

    /** Billing isn't set up in this build. */
    data object Unavailable : OfferingsResult

    data class Failed(val failure: BillingResult.Failure) : OfferingsResult
}

/** Outcome of a purchase or a restore. */
sealed interface BillingResult {
    /** Done; [state] is the entitlement afterwards (a restore that found nothing is NotEntitled). */
    data class Success(val state: PremiumState) : BillingResult

    sealed interface Failure : BillingResult

    /** The user backed out of Play's purchase sheet. Not an error: say nothing. */
    data object Cancelled : Failure

    /** Paid with a slow method (e.g. cash): premium starts once Play confirms it. */
    data object Pending : Failure

    /** This phone or Play account can't buy (billing unavailable, parental controls, …). */
    data object NotAllowed : Failure

    data object NetworkError : Failure

    /** Play itself had a problem; trying again later usually works. */
    data object StoreProblem : Failure

    data class Error(val message: String) : Failure
}

/**
 * The app's view of premium. All calls go through RevenueCat ([RevenueCatPremiumRepository]);
 * [FakePremiumRepository] stands in for tests and previews.
 */
interface PremiumRepository {
    /** Updates whenever RevenueCat reports new customer info; a cold start offline shows the cache. */
    val premium: Flow<PremiumState>

    /** Configures billing and starts listening. Called once, from Application.onCreate. */
    fun start()

    suspend fun offerings(): OfferingsResult

    suspend fun purchase(activity: Activity, option: PlanOption): BillingResult

    suspend fun restore(): BillingResult
}
