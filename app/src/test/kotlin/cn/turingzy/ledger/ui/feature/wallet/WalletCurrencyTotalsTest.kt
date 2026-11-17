package cn.turingzy.ledger.ui.feature.wallet

import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.account.AccountWithTemplate
import cn.turingzy.ledger.model.category.Category
import cn.turingzy.ledger.model.category.CategoryType
import cn.turingzy.ledger.model.category.CategoryWithIcon
import cn.turingzy.ledger.model.category.Icon
import cn.turingzy.ledger.model.transaction.Transaction
import cn.turingzy.ledger.model.transaction.TransactionDetail
import cn.turingzy.ledger.model.transaction.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class WalletCurrencyTotalsTest {
    private val cash = Account(1, "现金", AccountType.CASH, initialBalance = 10000L, currentBalance = 9000L)
    private fun entry(type: TransactionType, amount: Long, account: Account = cash, deleted: Boolean = false) = TransactionDetail(
        transaction = Transaction(id = 1, type = type, amount = amount, accountId = account.id,
            toAccountId = if (type == TransactionType.TRANSFER) 2 else null, categoryId = 1, transactionTime = 1, isDeleted = deleted),
        account = account,
        category = CategoryWithIcon(Category(1, "分类", CategoryType.valueOf(type.name), 1), Icon(1, "test", "分类", "res://test")),
    )

    @Test fun summaryUsesActualIncomeAndExpenseRatherThanAccountBalances() {
        val totals = walletCurrencyTotals(listOf(AccountWithTemplate(cash)), listOf(
            entry(TransactionType.INCOME, 5000L), entry(TransactionType.EXPENSE, 700L), entry(TransactionType.TRANSFER, 5300L),
        )).single()
        assertEquals(9000L, totals.assets)
        assertEquals(5000L, totals.income)
        assertEquals(700L, totals.expense)
    }

    @Test fun initialBalanceDoesNotBecomeIncome() {
        val totals = walletCurrencyTotals(listOf(AccountWithTemplate(cash)), emptyList()).single()
        assertEquals(9000L, totals.assets)
        assertEquals(0L, totals.income)
        assertEquals(0L, totals.expense)
    }

    @Test fun foreignAccountsAndBillsAreConvertedIntoOnePrimaryCurrency() {
        val dollar = cash.copy(id = 2, currency = "USD", currentBalance = 1234L, baseCurrency = "CNY", baseBalance = 8885L)
        val income = entry(TransactionType.INCOME, 700L, dollar).let { it.copy(transaction = it.transaction.copy(baseAmount = 5040L, baseCurrency = "CNY")) }
        val totals = walletCurrencyTotals(listOf(AccountWithTemplate(dollar), AccountWithTemplate(cash)), listOf(income, entry(TransactionType.EXPENSE, 100L))).single()
        assertEquals("CNY", totals.currency)
        assertEquals(17885L, totals.assets)
        assertEquals(5040L, totals.income)
        assertEquals(100L, totals.expense)
    }

    @Test fun excludedAccountsDoNotAddToAssetsButTheirBillsRemainRealHistory() {
        val excluded = cash.copy(includeInTotal = false)
        val deleted = cash.copy(id = 3, isDeleted = true)
        val totals = walletCurrencyTotals(listOf(AccountWithTemplate(excluded), AccountWithTemplate(deleted)), listOf(
            entry(TransactionType.EXPENSE, 300L, excluded), entry(TransactionType.INCOME, 500L, deleted),
            entry(TransactionType.INCOME, 900L, deleted = true),
        )).single()
        assertEquals(0L, totals.assets)
        assertEquals(300L, totals.expense)
        assertEquals(500L, totals.income)
    }

    @Test fun deletedForeignAccountsKeepTheirHistoryInPrimaryCurrencyTotals() {
        val deleted = cash.copy(currency = "EUR", isDeleted = true)
        val original = entry(TransactionType.EXPENSE, 500L, deleted)
        val valued = original.copy(transaction = original.transaction.copy(baseAmount = 3900L, baseCurrency = "CNY"))
        val totals = walletCurrencyTotals(emptyList(), listOf(valued)).single()
        assertEquals("CNY", totals.currency)
        assertEquals(0L, totals.assets)
        assertEquals(3900L, totals.expense)
    }

    @Test fun emptyWalletHasZeroCnyTotalsAndOverflowIsRejected() {
        assertEquals(listOf(WalletCurrencyTotals("CNY")), walletCurrencyTotals(emptyList(), emptyList()))
        assertThrows(ArithmeticException::class.java) {
            walletCurrencyTotals(listOf(AccountWithTemplate(cash.copy(currentBalance = Long.MAX_VALUE)), AccountWithTemplate(cash.copy(id = 2, currentBalance = 1))), emptyList())
        }
    }
}
