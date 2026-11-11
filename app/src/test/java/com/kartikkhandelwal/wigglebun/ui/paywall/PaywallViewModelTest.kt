package com.kartikkhandelwal.wigglebun.ui.paywall

import android.app.Activity
import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kartikkhandelwal.wigglebun.billing.BillingResult
import com.kartikkhandelwal.wigglebun.billing.FakePremiumRepository
import com.kartikkhandelwal.wigglebun.billing.OfferingsResult
import com.kartikkhandelwal.wigglebun.billing.PaywallOptions
import com.kartikkhandelwal.wigglebun.billing.PlanKind
import com.kartikkhandelwal.wigglebun.billing.PremiumState
import com.kartikkhandelwal.wigglebun.feedback.FeelEvent
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

/**
 * The fake repository never suspends and the ViewModel launches on the main thread's immediate
 * dispatcher, so every call below has finished by the time it returns.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class)
class PaywallViewModelTest {

    private val repo = FakePremiumRepository()
    private val activity: Activity = Robolectric.buildActivity(Activity::class.java).get()
    private lateinit var viewModel: PaywallViewModel

    private fun start() {
        viewModel = PaywallViewModel(repo)
    }

    private val state get() = viewModel.state.value

    /** The next feel event, or null when none was sent. */
    private fun feel(): FeelEvent? = runBlocking { withTimeoutOrNull(50) { viewModel.feel.first() } }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun yearlyIsSelectedWhenTheStoreOffersIt() {
        start()
        assertEquals(PlansState.Ready(FakePremiumRepository.SAMPLE_OPTIONS), state.plans)
        assertEquals(PlanKind.YEARLY, state.selected)
        assertEquals("\$29.99", state.selectedOption?.price)
    }

    @Test
    fun withoutAYearlyPlanTheFirstOneOfferedIsSelected() {
        val options = FakePremiumRepository.SAMPLE_OPTIONS
        repo.offeringsResult = OfferingsResult.Loaded(PaywallOptions(yearly = null, monthly = options.monthly, lifetime = options.lifetime))
        start()
        assertEquals(PlanKind.MONTHLY, state.selected)
    }

    @Test
    fun noPlansIsAProblemWithNothingSelected() {
        listOf(
            OfferingsResult.Empty,
            OfferingsResult.Unavailable,
            OfferingsResult.Failed(BillingResult.NetworkError),
        ).forEach { result ->
            repo.offeringsResult = result
            start()
            assertEquals(PlansState.Problem, state.plans)
            assertNull(state.selectedOption)
            // Nothing to buy: the button's call does nothing.
            viewModel.onPurchase(activity)
            assertTrue(repo.purchased.isEmpty())
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun tryAgainLoadsThePlansAndKeepsTheChoice() {
        repo.offeringsResult = OfferingsResult.Failed(BillingResult.NetworkError)
        start()
        repo.offeringsResult = OfferingsResult.Loaded(FakePremiumRepository.SAMPLE_OPTIONS)
        viewModel.loadPlans()
        assertEquals(PlanKind.YEARLY, state.selected)

        viewModel.onPick(PlanKind.LIFETIME)
        viewModel.loadPlans()
        assertEquals(PlanKind.LIFETIME, state.selected)
    }

    @Test
    fun buyingTheSelectedPlanEntitlesAndConfirmsOnce() {
        start()
        viewModel.onPick(PlanKind.MONTHLY)
        viewModel.onPurchase(activity)

        assertEquals(listOf(FakePremiumRepository.SAMPLE_OPTIONS.monthly), repo.purchased)
        assertFalse(state.busy)
        assertNull(state.notice)
        assertTrue(runBlocking { viewModel.entitled.first() })
        assertEquals(FeelEvent.Success, feel())
        assertNull(feel())
    }

    @Test
    fun backingOutOfPlaysSheetSaysNothing() {
        repo.purchaseResult = BillingResult.Cancelled
        start()
        viewModel.onPurchase(activity)
        assertNull(state.notice)
        assertFalse(state.busy)
        assertFalse(runBlocking { viewModel.entitled.first() })
        assertNull(feel())
    }

    @Test
    fun everyFailureHasItsOwnNotice() {
        mapOf(
            BillingResult.Pending to PaywallNotice.Pending,
            BillingResult.NotAllowed to PaywallNotice.NotAllowed,
            BillingResult.NetworkError to PaywallNotice.Network,
            BillingResult.StoreProblem to PaywallNotice.Store,
            BillingResult.Error("boom") to PaywallNotice.Error,
            // Play accepted the purchase but premium isn't active yet.
            BillingResult.Success(PremiumState.NotEntitled) to PaywallNotice.Pending,
        ).forEach { (result, notice) ->
            repo.purchaseResult = result
            start()
            viewModel.onPurchase(activity)
            assertEquals(notice, state.notice)
            assertFalse(state.busy)
            assertNull(feel())
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun pickingAnotherPlanClearsTheNotice() {
        repo.purchaseResult = BillingResult.NetworkError
        start()
        viewModel.onPurchase(activity)
        viewModel.onPick(PlanKind.LIFETIME)
        assertNull(state.notice)
        assertEquals(PlanKind.LIFETIME, state.selected)
    }

    @Test
    fun restoreThatFindsNothingSaysSo() {
        start()
        viewModel.onRestore()
        assertEquals(1, repo.restoreCalls)
        assertEquals(PaywallNotice.NothingToRestore, state.notice)
        assertNull(feel())
    }

    @Test
    fun restoreThatFindsAPurchaseEntitles() {
        repo.restoreResult = BillingResult.Success(FakePremiumRepository.SAMPLE_TRIAL)
        start()
        viewModel.onRestore()
        assertNull(state.notice)
        assertTrue(runBlocking { viewModel.entitled.first() })
        assertEquals(FeelEvent.Success, feel())
    }

    /** Premium already active when the paywall opens (a reinstall, a purchase on another phone). */
    @Test
    fun alreadyEntitledIsReportedAtOnce() {
        repo.premium.value = FakePremiumRepository.SAMPLE_TRIAL
        start()
        assertTrue(runBlocking { viewModel.entitled.first() })
    }
}
