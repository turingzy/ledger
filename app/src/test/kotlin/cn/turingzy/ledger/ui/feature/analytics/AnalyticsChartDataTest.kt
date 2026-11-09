package cn.turingzy.ledger.ui.feature.analytics

import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.model.AccountType
import cn.turingzy.ledger.model.Category
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.model.CategoryWithIcon
import cn.turingzy.ledger.model.Icon
import cn.turingzy.ledger.model.Transaction
import cn.turingzy.ledger.model.TransactionDetail
import cn.turingzy.ledger.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class AnalyticsChartDataTest {
    private val zone = ZoneId.of("Asia/Shanghai")

    @Test
    fun monthlyTrend_includesLeapDayAndFillsMissingDaysWithZero() {
        val date = LocalDate.of(2024, 2, 29)
        val result = buildAnalyticsTrend(
            listOf(
                transaction(date, TransactionType.EXPENSE, 101L),
                transaction(date, TransactionType.EXPENSE, 299L),
                transaction(date, TransactionType.INCOME, 12345L)
            ),
            AnalyticsTimeScope.MONTH, date, zone,
        )
        assertEquals(29, result.size)
        assertEquals(AnalyticsTrendPoint(date, 400L, 12345L), result.last())
        assertTrue(result.dropLast(1).all { it.expense == 0L && it.income == 0L })
    }

    @Test
    fun weeklyTrend_crossesYearBoundaryAndExcludesTransfersAndDeletedEntries() {
        val date = LocalDate.of(2021, 1, 1)
        val entries = listOf(
            transaction(date, TransactionType.EXPENSE, 9_007_199_254_740_993L),
            transaction(date, TransactionType.TRANSFER, 8000L),
            transaction(date, TransactionType.INCOME, 9000L, deleted = true),
            transaction(date.minusWeeks(2), TransactionType.EXPENSE, 5000L),
        )
        val result = buildAnalyticsTrend(entries, AnalyticsTimeScope.WEEK, date, zone)
        assertEquals(LocalDate.of(2020, 12, 28), result.first().date)
        assertEquals(LocalDate.of(2021, 1, 3), result.last().date)
        assertEquals(9_007_199_254_740_993L, result.sumOf { it.expense })
        assertEquals(0L, result.sumOf { it.income })
    }

    @Test
    fun yearlyTrend_aggregatesDailyTransactionsIntoTwelveMonths() {
        val first = LocalDate.of(2026, 1, 1)
        val entries = listOf(
            transaction(first, TransactionType.INCOME, 10001L),
            transaction(first.plusDays(30), TransactionType.INCOME, 9999L),
            transaction(LocalDate.of(2026, 12, 31), TransactionType.EXPENSE, 200L),
        )
        val result = buildAnalyticsTrend(entries, AnalyticsTimeScope.YEAR, first, zone)
        assertEquals(12, result.size)
        assertEquals(20000L, result.first().income)
        assertEquals(200L, result.last().expense)
    }

    @Test
    fun emptyPeriod_containsZeroValuesAndZeroRatio() {
        val result = buildAnalyticsTrend(
            emptyList(),
            AnalyticsTimeScope.WEEK,
            LocalDate.of(2026, 11, 9),
            zone
        )
        assertEquals(7, result.size)
        assertTrue(result.all { it.expense == 0L && it.income == 0L })
        assertEquals(0f, analyticsRatio(0L, 0L), 0f)
        assertEquals(0.25f, analyticsRatio(1L, 4L), 0.000001f)
    }

    private fun transaction(
        date: LocalDate,
        type: TransactionType,
        amount: Long,
        deleted: Boolean = false
    ): TransactionDetail = TransactionDetail(
        transaction = Transaction(
            type = type,
            amount = amount,
            accountId = 1L,
            categoryId = 1L,
            transactionTime = date.atStartOfDay(zone).toInstant().toEpochMilli(),
            isDeleted = deleted,
        ),
        account = Account(id = 1L, name = "银行卡", type = AccountType.DEBIT),
        category = CategoryWithIcon(
            category = Category(id = 1L, name = "餐饮", type = CategoryType.EXPENSE, iconId = 1L),
            icon = Icon(id = 1L, keyName = "food", displayName = "餐饮", iconUri = "res://"),
        ),
    )
}
