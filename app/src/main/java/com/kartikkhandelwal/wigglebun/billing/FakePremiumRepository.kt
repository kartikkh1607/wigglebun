package com.kartikkhandelwal.wigglebun.billing

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant

/**
 * A [PremiumRepository] for tests and previews: no SDK, no network. Set [premium], [offeringsResult],
 * [purchaseResult] and [restoreResult] to the case under test; a successful purchase or restore
 * also updates [premium], as the real one does.
 */
class FakePremiumRepository(
    initial: PremiumState = PremiumState.NotEntitled,
    var offeringsResult: OfferingsResult = OfferingsResult.Loaded(SAMPLE_OPTIONS),
    var purchaseResult: BillingResult = BillingResult.Success(SAMPLE_TRIAL),
    var restoreResult: BillingResult = BillingResult.Success(PremiumState.NotEntitled),
) : PremiumRepository {

    override val premium = MutableStateFlow(initial)

    /** Every plan [purchase] was called with, oldest first. */
    val purchased = mutableListOf<PlanOption>()
    var restoreCalls = 0
        private set

    override fun start() = Unit

    override suspend fun offerings(): OfferingsResult = offeringsResult

    override suspend fun purchase(activity: Activity, option: PlanOption): BillingResult {
        purchased += option
        return purchaseResult.also(::apply)
    }

    override suspend fun restore(): BillingResult {
        restoreCalls++
        return restoreResult.also(::apply)
    }

    private fun apply(result: BillingResult) {
        if (result is BillingResult.Success) premium.value = result.state
    }

    companion object {
        /** Prices as Play would format them for a US account (CLAUDE.md §5.5). */
        val SAMPLE_OPTIONS = PaywallOptions(
            yearly = PlanOption(
                kind = PlanKind.YEARLY,
                packageId = "\$rc_annual",
                price = "$29.99",
                period = BillingPeriod(1, PeriodUnit.YEAR),
                freeTrial = FreeTrial(BillingPeriod(Trial.FREE_TRIAL_DAYS, PeriodUnit.DAY), Trial.FREE_TRIAL_DAYS),
            ),
            monthly = PlanOption(
                kind = PlanKind.MONTHLY,
                packageId = "\$rc_monthly",
                price = "$4.99",
                period = BillingPeriod(1, PeriodUnit.MONTH),
                freeTrial = null,
            ),
            lifetime = PlanOption(
                kind = PlanKind.LIFETIME,
                packageId = "\$rc_lifetime",
                price = "$59.99",
                period = null,
                freeTrial = null,
            ),
        )

        val SAMPLE_TRIAL = PremiumState.Entitled(
            isTrial = true,
            expiresAt = Instant.parse("2026-11-18T09:00:00Z"),
            willRenew = true,
            isLifetime = false,
        )
    }
}
