package cn.turingzy.ledger.ui.component.biz

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyDisplayTest {
    @Test fun onlyThePrimaryCurrencyUsesASymbol() {
        assertEquals("¥12.34", transactionAmount(1234L, "CNY", "CNY"))
        assertEquals("USD 12.34", transactionAmount(1234L, "USD", "CNY"))
        assertEquals("$12.34", transactionAmount(1234L, "USD", "USD"))
        assertEquals("CNY 12.34", transactionAmount(1234L, "CNY", "USD"))
    }

    @Test fun otherPrimaryCurrenciesUseTheirOwnSymbols() {
        assertEquals("€12.34", transactionAmount(1234L, "EUR", "EUR"))
        assertEquals("£12.34", transactionAmount(1234L, "GBP", "GBP"))
        assertEquals("¥12.34", transactionAmount(1234L, "JPY", "JPY"))
        assertEquals("EUR 12.34", transactionAmount(1234L, "EUR", "CNY"))
    }

}
