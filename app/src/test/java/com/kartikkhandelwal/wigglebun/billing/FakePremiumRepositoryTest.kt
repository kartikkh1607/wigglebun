package com.kartikkhandelwal.wigglebun.billing

import android.app.Activity
import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class)
class FakePremiumRepositoryTest {

    private val activity = Robolectric.buildActivity(Activity::class.java).get()

    @Test
    fun successfulPurchaseUnlocks() = runBlocking {
        val repo = FakePremiumRepository()
        val yearly = FakePremiumRepository.SAMPLE_OPTIONS.yearly!!
        assertEquals(BillingResult.Success(FakePremiumRepository.SAMPLE_TRIAL), repo.purchase(activity, yearly))
        assertEquals(FakePremiumRepository.SAMPLE_TRIAL, repo.premium.value)
        assertEquals(listOf(yearly), repo.purchased)
    }

    @Test
    fun failedPurchaseLeavesTheStateAlone() = runBlocking {
        val repo = FakePremiumRepository(purchaseResult = BillingResult.Cancelled)
        assertEquals(BillingResult.Cancelled, repo.purchase(activity, FakePremiumRepository.SAMPLE_OPTIONS.monthly!!))
        assertEquals(PremiumState.NotEntitled, repo.premium.value)
    }

    /** The sample's trial must say what the app says. */
    @Test
    fun sampleOptionsMatchTheAppsTrial() {
        assertEquals(null, FakePremiumRepository.SAMPLE_OPTIONS.trialMismatch())
    }
}
