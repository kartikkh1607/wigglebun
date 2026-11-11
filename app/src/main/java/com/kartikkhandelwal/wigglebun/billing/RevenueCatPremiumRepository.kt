package com.kartikkhandelwal.wigglebun.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.kartikkhandelwal.wigglebun.BuildConfig
import com.kartikkhandelwal.wigglebun.data.ApplicationScope
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "Billing"

/**
 * Premium through RevenueCat (CLAUDE.md §5.5): it verifies and acknowledges Play purchases
 * server-side, so the app has no backend. Users are anonymous (no login); RevenueCat makes up an
 * app user id per install, and Restore links the Play account's purchases to it.
 *
 * Times here are the real clock ([Instant.now]), not AppClock: the debug Pet lab's fast clock must
 * not expire a subscription.
 */
@Singleton
class RevenueCatPremiumRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cache: PremiumCache,
    @ApplicationScope private val scope: CoroutineScope,
) : PremiumRepository {

    /** null until [start]; false when this build has no usable key. */
    private val configured = MutableStateFlow<Boolean?>(null)

    /** The latest answer from RevenueCat in this process; null until the first one. */
    private val live = MutableStateFlow<PremiumState?>(null)

    /** The current offering's packages by id, from the last [offerings] call, for [purchase]. */
    @Volatile
    private var packages: Map<String, Package> = emptyMap()

    override val premium: Flow<PremiumState> =
        combine(configured, live, cache.cached) { isConfigured, liveState, cached ->
            when (isConfigured) {
                null -> PremiumState.Unknown
                false -> PremiumState.Unavailable
                true -> liveState ?: premiumStateFromCache(cached, Instant.now())
            }
        }.distinctUntilChanged()

    override fun start() {
        if (configured.value != null) return
        val ok = configure(BuildConfig.REVENUECAT_API_KEY)
        configured.value = ok
        if (!ok) return
        val purchases = Purchases.sharedInstance
        // Called with the SDK's cached info right after configure and on every change (purchase,
        // renewal, expiry found on the next fetch).
        purchases.updatedCustomerInfoListener = object : UpdatedCustomerInfoListener {
            override fun onReceived(customerInfo: CustomerInfo) {
                scope.launch { remember(customerInfo) }
            }
        }
        scope.launch {
            try {
                remember(purchases.awaitCustomerInfo())
            } catch (e: PurchasesException) {
                // Offline or RevenueCat down: the cached state stands until the next answer.
                Log.w(TAG, "Couldn't fetch customer info: ${e.code}")
            }
        }
    }

    /** Never throws: without a usable key, billing is just unavailable. */
    private fun configure(key: String): Boolean {
        if (key.isEmpty()) {
            Log.i(TAG, "No RevenueCat key in this build: billing is unavailable")
            return false
        }
        // The SDK refuses a Test Store key outside a debug build with a dialog that then crashes
        // the app. The build only gives debug that key; this is the backstop.
        if (!BuildConfig.DEBUG && key.startsWith("test_")) {
            Log.e(TAG, "Test Store key in a non-debug build: billing is unavailable")
            return false
        }
        return try {
            if (!Purchases.isConfigured) {
                Purchases.logLevel = if (BuildConfig.DEBUG) LogLevel.VERBOSE else LogLevel.WARN
                // No appUserID: RevenueCat uses an anonymous id for this install.
                Purchases.configure(PurchasesConfiguration.Builder(context, key).build())
            }
            true
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "RevenueCat refused its configuration", e)
            false
        }
    }

    private suspend fun remember(info: CustomerInfo): PremiumState {
        val state = info.toPremiumState()
        live.value = state
        cache.save(state, Instant.now())
        return state
    }

    private fun sdk(): Purchases? = if (configured.value == true) Purchases.sharedInstance else null

    override suspend fun offerings(): OfferingsResult {
        val purchases = sdk() ?: return OfferingsResult.Unavailable
        val offerings = try {
            purchases.awaitOfferings()
        } catch (e: PurchasesException) {
            Log.w(TAG, "Couldn't load offerings: ${e.code}")
            return OfferingsResult.Failed(billingFailureOf(e.code, userCancelled = false, message = e.message))
        }
        val current = offerings.current
        packages = current?.availablePackages.orEmpty().associateBy { it.identifier }
        val options = current.toPaywallOptions()
        if (options == null) {
            Log.w(TAG, "No current offering with an ANNUAL, MONTHLY or LIFETIME package")
            return OfferingsResult.Empty
        }
        options.trialMismatch()?.let { Log.w(TAG, it) }
        return OfferingsResult.Loaded(options)
    }

    override suspend fun purchase(activity: Activity, option: PlanOption): BillingResult {
        val purchases = sdk() ?: return BillingResult.Error(NOT_CONFIGURED)
        val pkg = packages[option.packageId]
            ?: run {
                offerings()
                packages[option.packageId]
            }
            ?: return BillingResult.Error("The ${option.kind} plan is no longer offered")
        return try {
            val result = purchases.awaitPurchase(PurchaseParams.Builder(activity, pkg).build())
            succeeded(remember(result.customerInfo))
        } catch (e: PurchasesTransactionException) {
            if (e.code == PurchasesErrorCode.ProductAlreadyPurchasedError) {
                // Play says this account already owns it (e.g. bought on another phone): link it here.
                restore()
            } else {
                failed("Purchase", e, e.userCancelled)
            }
        } catch (e: PurchasesException) {
            failed("Purchase", e, userCancelled = false)
        }
    }

    override suspend fun restore(): BillingResult {
        val purchases = sdk() ?: return BillingResult.Error(NOT_CONFIGURED)
        return try {
            succeeded(remember(purchases.awaitRestore()))
        } catch (e: PurchasesException) {
            failed("Restore", e, userCancelled = false)
        }
    }

    private fun succeeded(state: PremiumState): BillingResult {
        if (state !is PremiumState.Entitled) {
            // A completed purchase that doesn't unlock means the product isn't attached to the
            // "premium" entitlement in the RevenueCat dashboard. (After a restore it just means
            // there was nothing to restore.)
            Log.w(TAG, "Entitlement '${Entitlements.PREMIUM}' is not active after the call")
        }
        return BillingResult.Success(state)
    }

    private fun failed(what: String, e: PurchasesException, userCancelled: Boolean): BillingResult.Failure {
        Log.w(TAG, "$what failed: ${e.code}")
        return billingFailureOf(e.code, userCancelled, e.message)
    }

    private companion object {
        const val NOT_CONFIGURED = "Billing isn't set up in this build"
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class BillingModule {
    @Binds
    abstract fun bindPremiumRepository(impl: RevenueCatPremiumRepository): PremiumRepository
}
