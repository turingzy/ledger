package cn.turingzy.ledger.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmountFormatterTest {

    @Test
    fun formatCents_standardPositiveAmount_formatsCorrectly() {
        assertEquals("100.50", AmountFormatter.formatCents(10050L))
        assertEquals("0.05", AmountFormatter.formatCents(5L))
        assertEquals("0.00", AmountFormatter.formatCents(0L))
    }

    @Test
    fun formatCents_withSymbol_formatsWithCurrencyPrefix() {
        assertEquals("¥100.50", AmountFormatter.formatCents(10050L, showSymbol = true))
        assertEquals("-¥50.00", AmountFormatter.formatCents(-5000L, showSymbol = true))
    }

    @Test
    fun formatCents_negativeAmount_formatsWithMinusSign() {
        assertEquals("-50.00", AmountFormatter.formatCents(-5000L))
        assertEquals("-0.99", AmountFormatter.formatCents(-99L))
    }

    @Test
    fun parseToCents_validStrings_returnsExactCents() {
        assertEquals(10050L, AmountFormatter.parseToCents("100.50"))
        assertEquals(10050L, AmountFormatter.parseToCents("¥100.50"))
        assertEquals(10050L, AmountFormatter.parseToCents("100.5"))
        assertEquals(5000L, AmountFormatter.parseToCents("50"))
        assertEquals(-5000L, AmountFormatter.parseToCents("-50.00"))
    }

    @Test
    fun parseToCents_invalidStrings_returnsNull() {
        assertNull(AmountFormatter.parseToCents(""))
        assertNull(AmountFormatter.parseToCents("abc"))
    }
}

