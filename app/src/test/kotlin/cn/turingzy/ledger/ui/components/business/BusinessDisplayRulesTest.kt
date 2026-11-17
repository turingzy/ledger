package cn.turingzy.ledger.ui.components.business

import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.model.AccountType
import cn.turingzy.ledger.model.Category
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.model.CategoryWithIcon
import cn.turingzy.ledger.model.Icon
import cn.turingzy.ledger.model.Transaction
import cn.turingzy.ledger.model.TransactionDetail
import cn.turingzy.ledger.model.TransactionType
import cn.turingzy.ledger.model.primaryBalance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import cn.turingzy.ledger.util.AmountFormatter

class BusinessDisplayRulesTest {
    @Test
    fun primaryCurrencyUsesItsOwnSymbolAndForeignCurrencyUsesAnExplicitCode() {
        mapOf("CNY" to "¥", "USD" to "$", "EUR" to "€", "GBP" to "£", "JPY" to "¥").forEach { (code, symbol) ->
            assertEquals("${symbol}12.34", businessAmountText(1234L, code, code))
            val other = if (code == "CNY") "USD" else "CNY"
            assertEquals("12.34 $code", businessAmountText(1234L, code, other))
        }
        assertEquals("12.34 ZZZ", businessAmountText(1234L, "ZZZ", "CNY"))
        assertEquals("ZZZ 12.34", businessAmountText(1234L, "ZZZ", "ZZZ"))
    }

    @Test
    fun typesChooseSignsAndNegativeValuesNeverAcquireDuplicateSigns() {
        assertEquals("−¥12.34", businessAmountText(1234L, "CNY", "CNY", TransactionType.EXPENSE))
        assertEquals("+¥12.34", businessAmountText(1234L, "CNY", "CNY", TransactionType.INCOME))
        assertEquals("¥12.34", businessAmountText(1234L, "CNY", "CNY", TransactionType.TRANSFER))
        TransactionType.entries.forEach {
            assertEquals("−12.34 USD", businessAmountText(-1234L, "USD", "CNY", it))
        }
        assertEquals("¥0.00", businessAmountText(0L, "CNY", "CNY"))
    }

    @Test
    fun missingAndHiddenAmountsCannotBeMistakenForZeroOrRevealTheCurrencyValue() {
        assertEquals("—", businessAmountText(null, "USD", "CNY"))
        assertEquals("****", businessAmountText(1234L, "USD", "CNY", hidden = true))
        assertEquals("****", businessAmountText(null, "USD", "CNY", hidden = true))
    }

    @Test
    fun fullLongRangeIsFormattedWithoutOverflowOrFloatingPointRounding() {
        assertEquals("−¥92233720368547758.08", businessAmountText(Long.MIN_VALUE, "CNY", "CNY"))
        assertEquals("¥92233720368547758.07", businessAmountText(Long.MAX_VALUE, "CNY", "CNY"))
    }

    @Test
    fun groupingUsesTheExistingAmountFormatterForOrdinaryValues() {
        assertEquals(
            "¥" + AmountFormatter.formatCents(12345678L, useGrouping = true),
            businessAmountText(12345678L, "CNY", "CNY", useGrouping = true),
        )
    }

    @Test
    fun accountGlyphAccentPreservesBrandPriorityAndEveryAccountType() {
        mapOf(AccountType.DEBIT to AccountAccent.Primary, AccountType.CREDIT to AccountAccent.Credit,
            AccountType.WALLET to AccountAccent.Income, AccountType.CASH to AccountAccent.Cash).forEach { (type, accent) ->
            assertEquals(accent, Account(name = "账户", type = type).displayAccent())
        }
        assertEquals(AccountAccent.Primary, Account(name = "支付宝", type = AccountType.CASH).displayAccent())
        assertEquals(AccountAccent.Income, Account(name = "微信", type = AccountType.CREDIT).displayAccent())
    }

    @Test
    fun accountValuationDoesNotUseASnapshotOfTheWrongCurrency() {
        val account = Account(name = "账户", type = AccountType.DEBIT, currency = "USD", currentBalance = 10000L,
            baseCurrency = "EUR", baseBalance = 9000L)
        assertNull(account.primaryBalance("CNY"))
        assertEquals(9000L, account.primaryBalance("EUR"))
        assertEquals(10000L, account.primaryBalance("USD"))
    }
}
