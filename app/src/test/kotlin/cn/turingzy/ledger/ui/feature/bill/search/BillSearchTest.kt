package cn.turingzy.ledger.ui.feature.bill.search

import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.category.Category
import cn.turingzy.ledger.model.category.CategoryType
import cn.turingzy.ledger.model.category.CategoryWithIcon
import cn.turingzy.ledger.model.category.Icon
import cn.turingzy.ledger.model.transaction.Transaction
import cn.turingzy.ledger.model.transaction.TransactionDetail
import cn.turingzy.ledger.model.transaction.TransactionType
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class BillSearchTest {
    private val zone = ZoneId.of("America/New_York")
    private val day = LocalDate.of(2026, 11, 1)

    @Test fun filtersByInclusiveLocalDatesPrimaryAmountAndEitherTransferAccount() {
        val request = BillSearchRequest(
            filters = BillSearchFilters(day, day, minimum = 500, maximum = 900),
            accountId = 2,
        )
        val rows = listOf(
            entry(1, offset = 0, amount = 500, target = 2),
            entry(2, offset = 0, amount = 900, account = 2, endOfDay = true),
            entry(3, offset = 1, amount = 700, account = 2),
            entry(4, offset = 0, amount = 901, account = 2),
            entry(5, offset = 0, amount = 700),
            entry(6, offset = 0, amount = 700, account = 2, currency = "USD"),
        )

        val result = buildBillSearchResults(rows, request, "CNY", zone)
        assertEquals(listOf(2L, 1L), result.groups.single().transactions.map { it.transaction.id })
        assertEquals(900L, result.groups.single().totalExpense)
    }

    @Test fun amountAndTimeSortingWorkAcrossDates() {
        val rows = listOf(
            entry(1, offset = 0, amount = 100),
            entry(2, offset = 1, amount = 400),
            entry(3, offset = 2, amount = 200, currency = "USD"),
            entry(4, offset = 3, amount = 10, currency = "USD", primaryAmount = 700),
        )
        fun order(sort: BillSearchSort): List<Long> {
            val result = buildBillSearchResults(rows, BillSearchRequest(sort = sort), "CNY", zone)
            return (result.ungrouped + result.groups.flatMap { it.transactions }).map { it.transaction.id }
        }

        assertEquals(listOf(1L, 2L, 4L, 3L), order(BillSearchSort.AMOUNT_ASC))
        assertEquals(listOf(4L, 2L, 1L, 3L), order(BillSearchSort.AMOUNT_DESC))
        assertEquals(listOf(1L, 2L, 3L, 4L), order(BillSearchSort.TIME_ASC))
        assertEquals(listOf(4L, 3L, 2L, 1L), order(BillSearchSort.TIME_DESC))
        assertEquals(order(BillSearchSort.TIME_DESC), order(BillSearchSort.DEFAULT))
    }

    @Test fun quickDateRangesUseBillWeekAndCalendarMonths() {
        val original = BillSearchFilters(minimum = 500)
        assertEquals(original.copy(start = day.minusDays(6), end = day),
            SearchDatePreset.THIS_WEEK.applyTo(original, day))
        assertEquals(original.copy(start = day, end = day.withDayOfMonth(30)),
            SearchDatePreset.THIS_MONTH.applyTo(original, day))
        assertEquals(original.copy(start = LocalDate.of(2026, 10, 1), end = day.minusDays(1)),
            SearchDatePreset.LAST_MONTH.applyTo(original, day))
        assertEquals(original.copy(start = day.minusMonths(3), end = day),
            SearchDatePreset.LAST_THREE_MONTHS.applyTo(original, day))
        assertEquals(original.copy(start = LocalDate.of(2026, 1, 1), end = LocalDate.of(2026, 12, 31)),
            SearchDatePreset.THIS_YEAR.applyTo(original, day))
        assertEquals(original.copy(start = LocalDate.of(2025, 1, 1), end = LocalDate.of(2025, 12, 31)),
            SearchDatePreset.LAST_YEAR.applyTo(original, day))
        assertEquals(original, SearchDatePreset.ALL.applyTo(original.copy(start = day), day))
    }

    @Test fun quickAmountRangesCoverEveryCentWithoutOverlapping() {
        val ranges = SearchAmountPreset.entries.drop(1)
        assertEquals(null, ranges.first().minimum)
        assertEquals(null, ranges.last().maximum)
        ranges.zipWithNext { lower, upper ->
            assertEquals(checkNotNull(lower.maximum) + 1, checkNotNull(upper.minimum))
        }
        val original = BillSearchFilters(start = day, minimum = 500, maximum = 1_000)
        assertEquals(BillSearchFilters(start = day), SearchAmountPreset.ALL.applyTo(original))
    }

    private fun entry(
        id: Long,
        offset: Long,
        amount: Long,
        account: Long = 1,
        target: Long? = null,
        currency: String = "CNY",
        primaryAmount: Long? = null,
        endOfDay: Boolean = false,
    ): TransactionDetail {
        val start = day.plusDays(offset).atStartOfDay(zone)
        val time = (if (endOfDay) start.plusDays(1).minusNanos(1_000_000) else start).toInstant().toEpochMilli()
        return TransactionDetail(
            Transaction(id = id, type = if (target == null) TransactionType.EXPENSE else TransactionType.TRANSFER,
                amount = amount, accountId = account, categoryId = 1, transactionTime = time, toAccountId = target,
                baseCurrency = primaryAmount?.let { "CNY" }, baseAmount = primaryAmount),
            Account(id = account, name = "Account", type = AccountType.CASH, currency = currency),
            category = CategoryWithIcon(Category(id = 1, name = "Food", type = CategoryType.EXPENSE, iconId = 1),
                Icon(id = 1, keyName = "food", displayName = "Food", iconUri = "res://")),
        )
    }
}
