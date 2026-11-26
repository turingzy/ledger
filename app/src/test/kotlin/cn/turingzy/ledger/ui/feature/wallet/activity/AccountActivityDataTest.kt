package cn.turingzy.ledger.ui.feature.wallet.activity

import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.category.Category
import cn.turingzy.ledger.model.category.CategoryType
import cn.turingzy.ledger.model.category.CategoryWithIcon
import cn.turingzy.ledger.model.category.Icon
import cn.turingzy.ledger.model.transaction.DateFilterType
import cn.turingzy.ledger.model.transaction.Transaction
import cn.turingzy.ledger.model.transaction.TransactionDetail
import cn.turingzy.ledger.model.transaction.TransactionType
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccountActivityDataTest {
    private val account = Account(id = 1L, name = "Wallet", type = AccountType.DEBIT, currentBalance = 1920L)
    private val other = account.copy(id = 2L, currency = "USD")
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.parse("2026-03-12")
    private val history = listOf(
        entry("2026-03-04", TransactionType.TRANSFER, 400L, other, account, 800L),
        entry("2026-03-01", TransactionType.INCOME, 300L),
        entry("2026-03-25", TransactionType.INCOME, 100L),
        entry("2026-03-03", TransactionType.EXPENSE, 200L),
        entry("2026-03-10", TransactionType.TRANSFER, 80L, account, other, 40L),
    )

    @Test fun monthUsesActualTransferCreditAndLeavesFutureDaysEmpty() {
        val month = accountPeriodActivity(account, history, DateFilterType.MONTH, today, today, zone)
        assertEquals(200L, month.expense)
        assertEquals(400L, month.income)
        assertEquals(listOf(1300L, 1300L, 1100L, 1900L), month.balances.take(4).map { it.balance })
        assertEquals(1820L, month.balances[11].balance)
        assertNull(month.balances[12].balance)
        assertEquals(5, month.groupedTransactions.size)
    }

    @Test fun yearUsesMonthClosingBalanceAndShowsTwelveMonths() {
        val year = accountPeriodActivity(account, history, DateFilterType.YEAR, today, today, zone)
        assertEquals((1..12).toList(), year.balances.map { it.date.monthValue })
        assertEquals(listOf(1000L, 1000L, 1820L, null), year.balances.take(4).map { it.balance })
    }

    private fun entry(
        date: String,
        type: TransactionType,
        amount: Long,
        source: Account = account,
        target: Account? = null,
        received: Long? = null,
    ): TransactionDetail = TransactionDetail(
        transaction = Transaction(
            type = type, amount = amount, accountId = source.id, toAccountId = target?.id, toAmount = received,
            categoryId = 1, transactionTime = LocalDate.parse(date).atStartOfDay(zone).toInstant().toEpochMilli(),
        ),
        account = source,
        toAccount = target,
        category = CategoryWithIcon(Category(1L, "Category", CategoryType.valueOf(type.name), 1L), Icon(1L, "icon", "Icon", "")),
    )
}
