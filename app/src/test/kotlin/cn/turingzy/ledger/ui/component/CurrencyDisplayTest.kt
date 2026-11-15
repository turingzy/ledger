package cn.turingzy.ledger.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyDisplayTest {
    @Test fun primaryCurrencyKeepsTheOriginalPrefixAndForeignCurrencyUsesALeadingCode() {
        assertEquals("¥12.34", transactionAmount(1234L, "CNY", "CNY"))
        assertEquals("USD 12.34", transactionAmount(1234L, "USD", "CNY"))
        assertEquals("$12.34", transactionAmount(1234L, "USD", "USD"))
        assertEquals("CNY 12.34", transactionAmount(1234L, "CNY", "USD"))
    }
}
