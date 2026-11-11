package cn.turingzy.ledger.ui.feature.analytics

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsAverageTest {
    @Test
    fun includesBothRangeEndpointsAndDaysWithoutTransactions() {
        val average = analyticsDailyAverage(7000L, 14000L, date("2026-11-09"), date("2026-11-15"))
        assertEquals(7L, average.dayCount)
        assertEquals(1000L, average.expense)
        assertEquals(2000L, average.income)
    }

    @Test
    fun roundsAverageToCentsWithoutFloatingPointMoney() {
        val average = analyticsDailyAverage(10L, 20L, date("2026-11-01"), date("2026-11-03"))
        assertEquals(3L, average.expense)
        assertEquals(7L, average.income)
    }

    @Test
    fun leapMonthsAndCrossYearRangesUseCalendarDays() {
        val leapMonth = analyticsDailyAverage(2900L, 5800L, date("2024-02-01"), date("2024-02-29"))
        assertEquals(29L, leapMonth.dayCount)
        assertEquals(100L, leapMonth.expense)
        val crossYear = analyticsDailyAverage(300L, 600L, date("2025-12-31"), date("2026-01-02"))
        assertEquals(3L, crossYear.dayCount)
        assertEquals(200L, crossYear.income)
    }

    @Test
    fun emptyPeriodHasZeroDailyAmounts() {
        val average = analyticsDailyAverage(0L, 0L, date("2026-11-11"), date("2026-11-11"))
        assertEquals(1L, average.dayCount)
        assertEquals(0L, average.expense)
        assertEquals(0L, average.income)
    }

    @Test
    fun singleDaySupportsMaximumLongAmount() {
        val average = analyticsDailyAverage(Long.MAX_VALUE, 0L, date("2026-11-11"), date("2026-11-11"))
        assertEquals(Long.MAX_VALUE, average.expense)
    }

    @Test(expected = IllegalArgumentException::class)
    fun reversedRangeIsRejected() {
        analyticsDailyAverage(0L, 0L, date("2026-11-12"), date("2026-11-11"))
    }

    private fun date(value: String) = LocalDate.parse(value)
}
