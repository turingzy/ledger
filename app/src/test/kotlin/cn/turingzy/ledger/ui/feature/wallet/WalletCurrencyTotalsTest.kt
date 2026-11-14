package cn.turingzy.ledger.ui.feature.wallet

import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.model.AccountType
import cn.turingzy.ledger.model.AccountWithTemplate
import cn.turingzy.ledger.model.Category
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.model.CategoryWithIcon
import cn.turingzy.ledger.model.Icon
import cn.turingzy.ledger.model.Transaction
import cn.turingzy.ledger.model.TransactionDetail
import cn.turingzy.ledger.model.TransactionType
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

    @Test fun currenciesStaySeparateAndCnyComesFirst() {
        val dollarAccount = cash.copy(id = 2, currency = "USD", currentBalance = 1234)
        val totals = walletCurrencyTotals(listOf(AccountWithTemplate(dollarAccount), AccountWithTemplate(cash)), listOf(
            entry(TransactionType.INCOME, 700L, dollarAccount), entry(TransactionType.EXPENSE, 100L),
        ))
        assertEquals(listOf("CNY", "USD"), totals.map { it.currency })
        assertEquals(listOf(9000L, 1234L), totals.map { it.assets })
        assertEquals(listOf(0L, 700L), totals.map { it.income })
        assertEquals(listOf(100L, 0L), totals.map { it.expense })
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

    @Test fun deletedForeignAccountKeepsItsTransactionCurrency() {
        val deleted = cash.copy(currency = "EUR", isDeleted = true)
        val totals = walletCurrencyTotals(emptyList(), listOf(entry(TransactionType.EXPENSE, 500L, deleted))).single()
        assertEquals("EUR", totals.currency)
        assertEquals(0L, totals.assets)
        assertEquals(500L, totals.expense)
    }

    @Test fun emptyWalletHasZeroCnyTotalsAndOverflowIsRejected() {
        assertEquals(listOf(WalletCurrencyTotals("CNY")), walletCurrencyTotals(emptyList(), emptyList()))
        assertThrows(ArithmeticException::class.java) {
            walletCurrencyTotals(listOf(AccountWithTemplate(cash.copy(currentBalance = Long.MAX_VALUE)), AccountWithTemplate(cash.copy(id = 2, currentBalance = 1))), emptyList())
        }
    }
}
