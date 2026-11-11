package com.kartikkhandelwal.wigglebun.billing

import com.revenuecat.purchases.PurchasesErrorCode
import org.junit.Assert.assertEquals
import org.junit.Test

class BillingErrorMappingTest {

    private fun map(code: PurchasesErrorCode, userCancelled: Boolean = false) = billingFailureOf(code, userCancelled, "msg")

    @Test
    fun cancelled() {
        assertEquals(BillingResult.Cancelled, map(PurchasesErrorCode.PurchaseCancelledError))
        // RevenueCat's userCancelled flag wins over the code.
        assertEquals(BillingResult.Cancelled, map(PurchasesErrorCode.StoreProblemError, userCancelled = true))
    }

    @Test
    fun pending() = assertEquals(BillingResult.Pending, map(PurchasesErrorCode.PaymentPendingError))

    @Test
    fun notAllowed() {
        assertEquals(BillingResult.NotAllowed, map(PurchasesErrorCode.PurchaseNotAllowedError))
        assertEquals(BillingResult.NotAllowed, map(PurchasesErrorCode.InsufficientPermissionsError))
    }

    @Test
    fun network() = assertEquals(BillingResult.NetworkError, map(PurchasesErrorCode.NetworkError))

    @Test
    fun storeProblem() = assertEquals(BillingResult.StoreProblem, map(PurchasesErrorCode.StoreProblemError))

    @Test
    fun everythingElseIsAnErrorWithItsMessage() {
        val special = setOf(
            PurchasesErrorCode.PurchaseCancelledError,
            PurchasesErrorCode.PaymentPendingError,
            PurchasesErrorCode.PurchaseNotAllowedError,
            PurchasesErrorCode.InsufficientPermissionsError,
            PurchasesErrorCode.NetworkError,
            PurchasesErrorCode.StoreProblemError,
        )
        PurchasesErrorCode.entries.filterNot { it in special }.forEach { code ->
            assertEquals(code.name, BillingResult.Error("msg"), map(code))
        }
    }
}
