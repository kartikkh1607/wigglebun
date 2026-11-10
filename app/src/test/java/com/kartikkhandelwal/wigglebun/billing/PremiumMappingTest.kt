package com.kartikkhandelwal.wigglebun.billing

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.EntitlementInfos
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.OwnershipType
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PeriodType
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.RecurrenceMode
import com.revenuecat.purchases.models.TestStoreProduct
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.Date

/** RevenueCat's own CustomerInfo / Offering objects → ours (real SDK classes, so Robolectric). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class)
class PremiumMappingTest {

    private val now = Instant.parse("2026-11-11T09:00:00Z")

    // --- CustomerInfo → PremiumState ---

    @Test
    fun trialIsEntitledAndMarkedAsTrial() {
        val ends = now.plusSeconds(7 * DAY)
        val state = customerInfo(premium(PeriodType.TRIAL, expires = ends)).toPremiumState()
        assertEquals(PremiumState.Entitled(isTrial = true, expiresAt = ends, willRenew = true, isLifetime = false), state)
    }

    @Test
    fun monthlyIsEntitled() {
        val renews = now.plusSeconds(30 * DAY)
        val state = customerInfo(premium(PeriodType.NORMAL, expires = renews, product = "wigglebun_monthly")).toPremiumState()
        assertEquals(PremiumState.Entitled(isTrial = false, expiresAt = renews, willRenew = true, isLifetime = false), state)
    }

    @Test
    fun cancelledYearlyStaysEntitledUntilItEndsButWontRenew() {
        val ends = now.plusSeconds(200 * DAY)
        val state = customerInfo(premium(PeriodType.NORMAL, expires = ends, willRenew = false)).toPremiumState()
        assertEquals(PremiumState.Entitled(isTrial = false, expiresAt = ends, willRenew = false, isLifetime = false), state)
    }

    @Test
    fun lifetimeHasNoExpiry() {
        val state = customerInfo(premium(PeriodType.NORMAL, expires = null, willRenew = false, product = "wigglebun_lifetime"))
            .toPremiumState()
        assertEquals(PremiumState.Entitled(isTrial = false, expiresAt = null, willRenew = false, isLifetime = true), state)
    }

    @Test
    fun expiredIsNotEntitled() {
        val state = customerInfo(premium(PeriodType.NORMAL, expires = now.minusSeconds(DAY), active = false, willRenew = false))
            .toPremiumState()
        assertEquals(PremiumState.NotEntitled, state)
    }

    @Test
    fun noPremiumEntitlementIsNotEntitled() {
        assertEquals(PremiumState.NotEntitled, customerInfo().toPremiumState())
    }

    @Test
    fun anotherEntitlementDoesNotUnlock() {
        val other = premium(PeriodType.NORMAL, expires = now.plusSeconds(DAY), id = "pro")
        assertEquals(PremiumState.NotEntitled, customerInfo(other).toPremiumState())
    }

    // --- Offering → PaywallOptions ---

    @Test
    fun plansArePickedByPackageTypeWithStorePricesAndTrial() {
        val options = offering(
            // Custom ids and a shuffled order: only the package type decides.
            pkg("lifetime_custom", PackageType.LIFETIME, product("wb_life", "$59.99", period = null)),
            pkg("monthly_custom", PackageType.MONTHLY, product("wb_month", "$4.99", period = MONTH)),
            pkg("annual_custom", PackageType.ANNUAL, product("wb_year", "$29.99", period = YEAR, trial = days(7))),
        ).toPaywallOptions()!!

        assertEquals(
            PlanOption(PlanKind.YEARLY, "annual_custom", "$29.99", BillingPeriod(1, PeriodUnit.YEAR), FreeTrial(BillingPeriod(7, PeriodUnit.DAY), 7)),
            options.yearly,
        )
        assertEquals(PlanOption(PlanKind.MONTHLY, "monthly_custom", "$4.99", BillingPeriod(1, PeriodUnit.MONTH), null), options.monthly)
        assertEquals(PlanOption(PlanKind.LIFETIME, "lifetime_custom", "$59.99", null, null), options.lifetime)
        assertNull(options.trialMismatch())
    }

    @Test
    fun weeklyTrialCountsSevenDays() {
        val options = offering(pkg("y", PackageType.ANNUAL, product("y", "$29.99", period = YEAR, trial = Period(1, Period.Unit.WEEK, "P1W"))))
            .toPaywallOptions()!!
        assertEquals(FreeTrial(BillingPeriod(1, PeriodUnit.WEEK), 7), options.yearly!!.freeTrial)
        assertNull(options.trialMismatch())
    }

    @Test
    fun missingPackageIsNull() {
        val options = offering(pkg("y", PackageType.ANNUAL, product("y", "$29.99", period = YEAR, trial = days(7))))
            .toPaywallOptions()!!
        assertNull(options.monthly)
        assertNull(options.lifetime)
        assertEquals(listOf(PlanKind.YEARLY), options.all.map { it.kind })
    }

    @Test
    fun otherPackageTypesAreIgnored() {
        val weekly = offering(pkg("w", PackageType.WEEKLY, product("w", "$1.99", period = Period(1, Period.Unit.WEEK, "P1W"))))
        assertNull(weekly.toPaywallOptions())
    }

    @Test
    fun noCurrentOfferingGivesNothing() {
        assertNull((null as Offering?).toPaywallOptions())
        assertNull(offering().toPaywallOptions())
    }

    @Test
    fun yearlyWithoutFreeTrialSaysSo() {
        val options = offering(pkg("y", PackageType.ANNUAL, product("y", "$29.99", period = YEAR))).toPaywallOptions()!!
        assertNull(options.yearly!!.freeTrial)
        assertEquals("The yearly plan has no free-trial phase", options.trialMismatch())
    }

    @Test
    fun trialOfTheWrongLengthIsReported() {
        val options = offering(pkg("y", PackageType.ANNUAL, product("y", "$29.99", period = YEAR, trial = days(3)))).toPaywallOptions()!!
        assertEquals(3, options.yearly!!.freeTrial!!.days)
        assertEquals("The yearly free trial is 3 days; Trial.FREE_TRIAL_DAYS is 7", options.trialMismatch())
    }

    @Test
    fun noYearlyIsReportedAsMismatch() {
        val options = offering(pkg("m", PackageType.MONTHLY, product("m", "$4.99", period = MONTH))).toPaywallOptions()!!
        assertEquals("The current offering has no ANNUAL package", options.trialMismatch())
    }

    // --- fixtures ---

    private fun premium(
        periodType: PeriodType,
        expires: Instant?,
        willRenew: Boolean = true,
        active: Boolean = true,
        product: String = "wigglebun_yearly",
        id: String = Entitlements.PREMIUM,
    ) = EntitlementInfo(
        identifier = id,
        isActive = active,
        willRenew = willRenew,
        periodType = periodType,
        latestPurchaseDate = Date.from(now.minusSeconds(DAY)),
        originalPurchaseDate = Date.from(now.minusSeconds(DAY)),
        expirationDate = expires?.let(Date::from),
        store = Store.PLAY_STORE,
        productIdentifier = product,
        productPlanIdentifier = null,
        isSandbox = true,
        unsubscribeDetectedAt = null,
        billingIssueDetectedAt = null,
        ownershipType = OwnershipType.PURCHASED,
        jsonObject = JSONObject(),
        verification = VerificationResult.NOT_REQUESTED,
    )

    private fun customerInfo(vararg entitlements: EntitlementInfo) = CustomerInfo(
        entitlements = EntitlementInfos(entitlements.associateBy { it.identifier }, VerificationResult.NOT_REQUESTED),
        allExpirationDatesByProduct = emptyMap(),
        allPurchaseDatesByProduct = emptyMap(),
        requestDate = Date.from(now),
        schemaVersion = 3,
        firstSeen = Date.from(now),
        originalAppUserId = "\$RCAnonymousID:test",
        managementURL = null,
        originalPurchaseDate = null,
        // CustomerInfo reads its "subscriber" object eagerly; the entitlements above are what we map.
        jsonObject = JSONObject("""{"subscriber":{"non_subscriptions":{},"subscriptions":{},"entitlements":{}}}"""),
    )

    private fun product(id: String, price: String, period: Period?, trial: Period? = null) = TestStoreProduct(
        id = id,
        name = id,
        title = id,
        description = id,
        price = Price(formatted = price, amountMicros = 1_000_000, currencyCode = "USD"),
        period = period,
        freeTrialPricingPhase = trial?.let {
            PricingPhase(it, RecurrenceMode.FINITE_RECURRING, 1, Price(formatted = "Free", amountMicros = 0, currencyCode = "USD"))
        },
        introPricePricingPhase = null,
    )

    private fun pkg(id: String, type: PackageType, product: TestStoreProduct) =
        Package(id, type, product, PresentedOfferingContext("default"))

    private fun offering(vararg packages: Package) = Offering("default", "Default", emptyMap(), packages.toList())

    private fun days(n: Int) = Period(n, Period.Unit.DAY, "P${n}D")

    private companion object {
        const val DAY = 24 * 60 * 60L
        val YEAR = Period(1, Period.Unit.YEAR, "P1Y")
        val MONTH = Period(1, Period.Unit.MONTH, "P1M")
    }
}
