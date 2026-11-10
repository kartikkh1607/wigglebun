package com.kartikkhandelwal.wigglebun.billing

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PeriodType
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.models.Period
import java.time.Instant

// Pure translations between RevenueCat's types and ours, so the rules can be unit-tested without
// the SDK running.

/**
 * What RevenueCat says right now. Its own `isActive` is trusted as it stands: it was judged against
 * the server's clock, which a wrong phone clock can't move.
 */
internal fun CustomerInfo.toPremiumState(): PremiumState {
    val premium = entitlements.all[Entitlements.PREMIUM]
    if (premium == null || !premium.isActive) return PremiumState.NotEntitled
    val expiresAt = premium.expirationDate?.toInstant()
    return PremiumState.Entitled(
        isTrial = premium.periodType == PeriodType.TRIAL,
        expiresAt = expiresAt,
        willRenew = premium.willRenew,
        // Only a non-subscription purchase (or a lifetime grant) has no expiry date.
        isLifetime = expiresAt == null,
    )
}

/**
 * What a cold start shows before RevenueCat answers. A cached subscription counts only until it
 * expires: offline we can't tell whether it renewed, and an expired one must not unlock the app.
 * If the phone's clock is now earlier than when the cache was saved, the save time is used, so
 * winding the clock back can't stretch a trial offline.
 */
internal fun premiumStateFromCache(cached: CachedPremium?, now: Instant): PremiumState {
    if (cached == null) return PremiumState.Unknown
    val state = cached.state
    if (state !is PremiumState.Entitled) return state
    val expiresAt = state.expiresAt ?: return state
    val effectiveNow = maxOf(now, cached.savedAt)
    return if (expiresAt.isAfter(effectiveNow)) state else PremiumState.NotEntitled
}

/** Picks the three plans by package type, never by product id; null when none of them is there. */
internal fun Offering?.toPaywallOptions(): PaywallOptions? {
    if (this == null) return null
    fun pick(type: PackageType, kind: PlanKind) =
        availablePackages.firstOrNull { it.packageType == type }?.toPlanOption(kind)
    val options = PaywallOptions(
        yearly = pick(PackageType.ANNUAL, PlanKind.YEARLY),
        monthly = pick(PackageType.MONTHLY, PlanKind.MONTHLY),
        lifetime = pick(PackageType.LIFETIME, PlanKind.LIFETIME),
    )
    return options.takeIf { it.all.isNotEmpty() }
}

internal fun Package.toPlanOption(kind: PlanKind): PlanOption {
    // PurchaseParams.Builder(activity, package) buys the product's defaultOption (RevenueCat picks
    // the offer with the longest free trial), so describe that one: its free phase is the trial the
    // user would really get, and its last phase is the price after it.
    val option = product.defaultOption
    val subscription = kind != PlanKind.LIFETIME
    val freePhase = if (subscription) option?.freePhase?.billingPeriod else null
    return PlanOption(
        kind = kind,
        packageId = identifier,
        price = option?.fullPricePhase?.price?.formatted ?: product.price.formatted,
        period = if (subscription) (option?.billingPeriod ?: product.period)?.toBillingPeriod() else null,
        freeTrial = freePhase?.let { period ->
            period.toBillingPeriod()?.let { FreeTrial(it, it.approximateDays) }
        },
    )
}

private fun Period.toBillingPeriod(): BillingPeriod? {
    val unit = when (unit) {
        Period.Unit.DAY -> PeriodUnit.DAY
        Period.Unit.WEEK -> PeriodUnit.WEEK
        Period.Unit.MONTH -> PeriodUnit.MONTH
        Period.Unit.YEAR -> PeriodUnit.YEAR
        Period.Unit.UNKNOWN -> return null
    }
    return BillingPeriod(value, unit)
}

/**
 * Why the yearly plan's trial doesn't match [Trial.FREE_TRIAL_DAYS], or null when it does. The
 * Play Console offer's free phase must match the app's copy ("Try 7 days free").
 */
internal fun PaywallOptions.trialMismatch(): String? {
    val yearly = yearly ?: return "The current offering has no ANNUAL package"
    val trial = yearly.freeTrial ?: return "The yearly plan has no free-trial phase"
    return if (trial.days == Trial.FREE_TRIAL_DAYS) null
    else "The yearly free trial is ${trial.days} days; Trial.FREE_TRIAL_DAYS is ${Trial.FREE_TRIAL_DAYS}"
}

/**
 * Every RevenueCat error code, named one by one so an SDK update that adds a code fails to compile
 * here instead of silently landing in the wrong bucket.
 */
internal fun billingFailureOf(code: PurchasesErrorCode, userCancelled: Boolean, message: String): BillingResult.Failure {
    if (userCancelled) return BillingResult.Cancelled
    return when (code) {
        PurchasesErrorCode.PurchaseCancelledError -> BillingResult.Cancelled
        PurchasesErrorCode.PaymentPendingError -> BillingResult.Pending
        PurchasesErrorCode.PurchaseNotAllowedError,
        PurchasesErrorCode.InsufficientPermissionsError,
        -> BillingResult.NotAllowed
        PurchasesErrorCode.NetworkError -> BillingResult.NetworkError
        PurchasesErrorCode.StoreProblemError -> BillingResult.StoreProblem
        PurchasesErrorCode.UnknownError,
        PurchasesErrorCode.IneligibleError,
        PurchasesErrorCode.PurchaseInvalidError,
        PurchasesErrorCode.ProductNotAvailableForPurchaseError,
        PurchasesErrorCode.ProductAlreadyPurchasedError,
        PurchasesErrorCode.ReceiptAlreadyInUseError,
        PurchasesErrorCode.InvalidReceiptError,
        PurchasesErrorCode.MissingReceiptFileError,
        PurchasesErrorCode.InvalidCredentialsError,
        PurchasesErrorCode.UnexpectedBackendResponseError,
        PurchasesErrorCode.InvalidAppUserIdError,
        PurchasesErrorCode.OperationAlreadyInProgressError,
        PurchasesErrorCode.UnknownBackendError,
        PurchasesErrorCode.InvalidAppleSubscriptionKeyError,
        PurchasesErrorCode.InvalidSubscriberAttributesError,
        PurchasesErrorCode.LogOutWithAnonymousUserError,
        PurchasesErrorCode.ConfigurationError,
        PurchasesErrorCode.UnsupportedError,
        PurchasesErrorCode.EmptySubscriberAttributesError,
        PurchasesErrorCode.CustomerInfoError,
        PurchasesErrorCode.SignatureVerificationError,
        PurchasesErrorCode.TestStoreSimulatedPurchaseError,
        -> BillingResult.Error(message)
    }
}
