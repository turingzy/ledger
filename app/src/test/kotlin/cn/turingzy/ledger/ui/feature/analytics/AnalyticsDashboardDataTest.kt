package cn.turingzy.ledger.ui.feature.analytics

import cn.turingzy.ledger.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class AnalyticsDashboardDataTest {
    private val date = LocalDate.of(2024, 2, 29)
    private val zone = ZoneId.of("Asia/Shanghai")

    @Test
    fun dailyTotalsKeepExactCentsAndExcludeTransfersAndDeletedRecords() {
        val days = analyticsDays(listOf(
            entry(1L, TransactionType.EXPENSE, 10L), entry(2L, TransactionType.EXPENSE, 20L),
            entry(3L, TransactionType.INCOME, 123L), entry(4L, TransactionType.TRANSFER, 9999L),
            entry(5L, TransactionType.EXPENSE, 8888L, deleted = true),
        ), zone)
        assertEquals(AnalyticsDay(30L, 123L), days[date])
        assertEquals(1, days.size)
    }

    @Test
    fun cumulativeSeriesIncludesLeapDayAndRetainsZeroSpendDays() {
        val values = cumulativeExpenses(mapOf(date.minusDays(2) to AnalyticsDay(10L), date to AnalyticsDay(20L)), YearMonth.from(date))
        assertEquals(29, values.size)
        assertEquals(10L, values[27])
        assertEquals(30L, values[28])
    }

    @Test
    fun monthHistoryIncludesMissingMonthsAndNegativeBalanceAcrossYearBoundary() {
        val months = monthlyAnalytics(mapOf(LocalDate.of(2023, 12, 31) to AnalyticsDay(1000L, 300L)), YearMonth.of(2024, 3))
        assertEquals(6, months.size)
        assertEquals(YearMonth.of(2023, 10), months.first().month)
        assertEquals(-700L, months.single { it.month == YearMonth.of(2023, 12) }.balance)
        assertEquals(0L, months.last().expense)
    }

    @Test
    fun predictionRoundsCentsAndZeroDaysDoNotDivideByZero() {
        assertEquals(31L, projectExpense(10L, 3, 9) + projectExpense(1L, 3, 3))
        assertEquals(0L, projectExpense(100L, 0, 31))
        assertEquals(33L, projectExpense(10L, 3, 10))
    }

    private fun entry(id: Long, type: TransactionType, amount: Long, deleted: Boolean = false) = TransactionDetail(
        Transaction(id = id, type = type, amount = amount, accountId = 1L, categoryId = 1L,
            transactionTime = date.atStartOfDay(zone).toInstant().toEpochMilli(), isDeleted = deleted),
        Account(id = 1L, name = "测试", type = AccountType.DEBIT),
        category = CategoryWithIcon(Category(id = 1L, name = "餐饮", type = CategoryType.EXPENSE, iconId = 1L),
            Icon(id = 1L, keyName = "food", displayName = "餐饮", iconUri = "res://")),
    )
}
