package cn.turingzy.ledger.ui.feature.analytics

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class AnalyticsStatisticsTest {
    @Test fun weekAndMonthUseCalendarDaysIncludingLeapDay() {
        val week = average(AnalyticsTimeScope.WEEK, 700L, "2024-02-26", "2024-03-03", 350L, "2024-02-19", "2024-02-25")
        assertEquals("日均支出", week.label)
        assertEquals(100L, week.amount)
        assertEquals("环比增长 100%", week.change)
        val month = average(AnalyticsTimeScope.MONTH, 2900L, "2024-02-01", "2024-02-29", 3100L, "2024-01-01", "2024-01-31")
        assertEquals(100L, month.amount)
        assertEquals("环比持平", month.change)
    }
    @Test fun annualViewUsesTwelveMonthsAndAllViewCountsCalendarYears() {
        val year = average(AnalyticsTimeScope.YEAR, 12000L, "2024-01-01", "2024-12-31", 24000L, "2023-01-01", "2023-12-31")
        assertEquals("月均支出", year.label)
        assertEquals(1000L, year.amount)
        assertEquals("环比下降 50%", year.change)
        assertTrue(year.direction < 0)
        val all = average(AnalyticsTimeScope.ALL, 9000L, "2023-12-31", "2025-01-01", 0L, "2020-12-28", "2022-12-30")
        assertEquals("年均支出", all.label)
        assertEquals(3000L, all.amount)
        assertEquals("暂无环比", all.change)
        assertEquals(0, all.direction)
    }
    @Test fun roundsMoneyOnlyForDisplayAndRetainsTinyChanges() {
        val result = average(AnalyticsTimeScope.MONTH, 100001L, "2024-04-01", "2024-04-30", 100000L, "2024-03-02", "2024-03-31")
        assertEquals(3333L, result.amount)
        assertEquals("环比增长 <0.1%", result.change)
        assertTrue(result.direction > 0)
    }
    @Test fun maximumAmountDoesNotOverflowComparisonProducts() {
        val result = average(AnalyticsTimeScope.WEEK, Long.MAX_VALUE, "2024-01-01", "2024-01-07", Long.MAX_VALUE, "2023-12-25", "2023-12-31")
        assertEquals("环比持平", result.change)
    }
    private fun average(scope: AnalyticsTimeScope, expense: Long, start: String, end: String, previous: Long, previousStart: String, previousEnd: String) =
        analyticsExpenseAverage(scope, expense, LocalDate.parse(start), LocalDate.parse(end), previous, LocalDate.parse(previousStart), LocalDate.parse(previousEnd))
}
