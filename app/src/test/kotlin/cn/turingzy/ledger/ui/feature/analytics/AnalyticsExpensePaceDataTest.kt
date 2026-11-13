package cn.turingzy.ledger.ui.feature.analytics

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class AnalyticsExpensePaceDataTest {
    private fun date(value: String) = LocalDate.parse(value)
    @Test fun weekComparesPreviousWeekAndExcludesFutureRecords() {
        val start = date("2026-11-09")
        val data = expensePaceData(mapOf(
            start.minusWeeks(1) to AnalyticsDay(100L), start.minusDays(1) to AnalyticsDay(200L),
            start to AnalyticsDay(10L), start.plusDays(2) to AnalyticsDay(20L), start.plusDays(4) to AnalyticsDay(999L),
        ), AnalyticsTimeScope.WEEK, start, start.plusDays(6), start.plusDays(2))
        assertEquals(7, data.dates.size)
        assertEquals(listOf(10L, 10L, 30L), data.current)
        assertEquals(300L, data.previous.last())
        assertEquals(300L, data.scale)
    }
    @Test fun marchHoldsPreviousFebruaryTotalOnDaysThirtyAndThirtyOne() {
        val data = expensePaceData(mapOf(date("2024-02-29") to AnalyticsDay(2900L), date("2024-03-31") to AnalyticsDay(3100L)),
            AnalyticsTimeScope.MONTH, date("2024-03-01"), date("2024-03-31"), date("2024-04-01"))
        assertEquals(31, data.current.size)
        assertEquals(listOf(2900L, 2900L, 2900L), data.previous.takeLast(3))
        assertEquals(3100L, data.current.last())
    }
    @Test fun yearUsesCumulativeMonthsAndAllUsesCumulativeCalendarYears() {
        val days = mapOf(date("2023-01-01") to AnalyticsDay(100L), date("2024-01-02") to AnalyticsDay(200L), date("2024-03-02") to AnalyticsDay(300L), date("2025-04-02") to AnalyticsDay(400L))
        val year = expensePaceData(days, AnalyticsTimeScope.YEAR, date("2024-01-01"), date("2024-12-31"), date("2026-01-01"))
        assertEquals(12, year.dates.size)
        assertEquals(200L, year.current[1])
        assertEquals(500L, year.current[2])
        val all = expensePaceData(days, AnalyticsTimeScope.ALL, date("2024-01-01"), date("2025-12-31"), date("2026-01-01"))
        assertEquals(listOf(500L, 900L), all.current)
        assertEquals(listOf(0L, 100L), all.previous)
    }
    @Test fun emptyFutureRangeHasNoActualSeriesAndHasUsableAxis() {
        val data = expensePaceData(emptyMap(), AnalyticsTimeScope.WEEK, date("2026-11-09"), date("2026-11-15"), date("2026-11-01"))
        assertTrue(data.current.isEmpty())
        assertTrue(data.previous.all { it == 0L })
        assertTrue(data.scale > 0L)
    }
    @Test fun maximumAmountProducesValidScaleWithoutOverflow() {
        val start = date("2026-11-09")
        val data = expensePaceData(mapOf(start to AnalyticsDay(Long.MAX_VALUE)), AnalyticsTimeScope.WEEK, start, start.plusDays(6), start)
        assertEquals(Long.MAX_VALUE, data.scale)
        assertEquals(Long.MAX_VALUE, data.current.last())
    }
    @Test fun axisChangesWithTheSelectedPeriodAndIgnoresFutureAndUnrelatedAmounts() {
        val start = date("2026-11-09")
        val days = mapOf(start to AnalyticsDay(20L), start.minusWeeks(1) to AnalyticsDay(100L),
            start.plusDays(1) to AnalyticsDay(10000000L), start.minusYears(1) to AnalyticsDay(99999999L))
        val current = expensePaceData(days, AnalyticsTimeScope.WEEK, start, start.plusDays(6), start)
        assertEquals(100L, current.scale)
        val nextDay = expensePaceData(days, AnalyticsTimeScope.WEEK, start, start.plusDays(6), start.plusDays(1))
        assertTrue(nextDay.scale > current.scale)
        assertTrue(nextDay.scale >= 10000020L)
    }

}
