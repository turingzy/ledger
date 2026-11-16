package cn.turingzy.ledger.ui.component

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.sp
import cn.turingzy.ledger.model.TransactionType
import cn.turingzy.ledger.ui.theme.IosSecondary
import cn.turingzy.ledger.ui.theme.LedgerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ForeignTransactionAmountTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun foreignExpenseHasASmallNeutralCurrencySuffix() {
        showAmount(TransactionType.EXPENSE)
        assertCurrencySuffix("−11.00 USD")
    }

    @Test
    fun foreignIncomeHasASmallNeutralCurrencySuffix() {
        showAmount(TransactionType.INCOME)
        assertCurrencySuffix("+11.00 USD")
    }

    @Test
    fun primaryCurrencyKeepsItsExistingSymbol() {
        showAmount(TransactionType.INCOME, primary = "USD")
        compose.onNodeWithText("+$11.00").assertIsDisplayed()
        compose.onNodeWithText("USD", substring = true).assertDoesNotExist()
    }

    @Test
    fun renminbiUsesTheSameCurrencySuffixWhenItIsNotPrimary() {
        showAmount(TransactionType.EXPENSE, primary = "USD", currency = "CNY")
        assertCurrencySuffix("−11.00 CNY", "CNY")
    }

    private fun showAmount(type: TransactionType, primary: String = "CNY", currency: String = "USD") {
        compose.setContent {
            LedgerTheme {
                CompositionLocalProvider(LocalLedgerCurrency provides primary) {
                    TransactionRow(
                        categoryName = "餐饮",
                        supportingText = "外币账户",
                        timeText = "12:00",
                        amount = 1100L,
                        type = type,
                        icon = MaterialSymbols.Restaurant,
                        currency = currency,
                    )
                }
            }
        }
    }

    private fun assertCurrencySuffix(expected: String, currency: String = "USD") {
        val amount = compose.onNodeWithText(expected).assertIsDisplayed()
            .fetchSemanticsNode().config[SemanticsProperties.Text].single()
        val suffix = amount.spanStyles.single()
        assertEquals(" $currency", amount.text.substring(suffix.start, suffix.end))
        assertEquals(11.sp, suffix.item.fontSize)
        assertEquals(IosSecondary, suffix.item.color)
    }
}
