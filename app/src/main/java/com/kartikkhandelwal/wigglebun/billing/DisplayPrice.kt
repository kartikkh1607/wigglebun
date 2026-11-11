package com.kartikkhandelwal.wigglebun.billing

/**
 * A zero minor part: the decimal separator ("." or ",") and exactly two zeros, after a digit and
 * not followed by another digit or by another separator and digit. That keeps "$4.99" (not zero),
 * "¥1,000" and "$1,000" (three zeros: a thousands group) and "₹1,00,000" (Indian grouping: a
 * separator and a digit follow) whole.
 */
private val ZeroMinorPart = Regex("""(?<=\d)[.,]00(?!\d|[.,]\d)""")

/**
 * The store's formatted price for display: "₹2,800.00" reads "₹2,800", "29,00 €" reads "29 €".
 * Prices with a real minor part ("$4.99", "4,99 €") and currencies without one ("¥1,200") are
 * left alone. Only the text changes; the price itself still comes from the store.
 */
fun displayPrice(formatted: String): String = formatted.replace(ZeroMinorPart, "")
