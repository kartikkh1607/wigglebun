package com.kartikkhandelwal.wigglebun.billing

import org.junit.Assert.assertEquals
import org.junit.Test

/** The store's price text, shortened only when its minor part is all zeros. */
class DisplayPriceTest {

    @Test
    fun inrDropsDotZeroZero() {
        assertEquals("₹2,800", displayPrice("₹2,800.00"))
        assertEquals("₹4,999", displayPrice("₹4,999.00"))
        // Indian grouping has ",00" inside the number: only the decimal part goes.
        assertEquals("₹1,00,000", displayPrice("₹1,00,000.00"))
        assertEquals("₹1,00,000", displayPrice("₹1,00,000"))
    }

    @Test
    fun usdKeepsRealCentsAndDropsZeroCents() {
        assertEquals("$4.99", displayPrice("$4.99"))
        assertEquals("$29.99", displayPrice("$29.99"))
        assertEquals("$5", displayPrice("$5.00"))
        assertEquals("$4.90", displayPrice("$4.90"))
        assertEquals("$1,000", displayPrice("$1,000"))
        assertEquals("$1,000", displayPrice("$1,000.00"))
    }

    @Test
    fun eurWithCommaDecimals() {
        assertEquals("4,99 €", displayPrice("4,99 €"))
        assertEquals("29 €", displayPrice("29,00 €"))
        assertEquals("1.299 €", displayPrice("1.299,00 €"))
        assertEquals("1.299,50 €", displayPrice("1.299,50 €"))
        assertEquals("Rp 499.000", displayPrice("Rp 499.000,00"))
    }

    @Test
    fun jpyHasNoMinorPartAndStaysWhole() {
        assertEquals("¥1,200", displayPrice("¥1,200"))
        assertEquals("¥1,000", displayPrice("¥1,000"))
        assertEquals("¥10,000", displayPrice("¥10,000"))
        assertEquals("￥4,500", displayPrice("￥4,500"))
    }

    @Test
    fun textWithoutANumberIsUntouched() {
        assertEquals("", displayPrice(""))
        assertEquals("Free", displayPrice("Free"))
    }
}
