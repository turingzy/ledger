package cn.turingzy.ledger.ui.feature.analytics

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsTrendLayoutTest {
    private val start = LocalDate.of(2026, 11, 1)

    @Test
    fun scaleIncludesNegativeBalanceAndSharedZeroBaseline() {
        val scale = trendValueScale(listOf(AnalyticsTrendPoint(start, 20000L, 5000L)))
        assertEquals(-15000L, scale.minimum)
        assertEquals(20000L, scale.maximum)
    }

    @Test
    fun scaleHandlesLongAmountsWithoutSubtractionOverflow() {
        val scale = trendValueScale(listOf(AnalyticsTrendPoint(start, Long.MAX_VALUE, 0L)))
        assertEquals(Long.MAX_VALUE, scale.maximum)
        assertEquals(-Long.MAX_VALUE, scale.minimum)
        assertEquals(TrendAmountUnit.TRILLION_YUAN, trendAmountUnit(scale))
    }

    @Test
    fun emptyAndZeroSeriesHaveUsableScale() {
        listOf(emptyList(), listOf(AnalyticsTrendPoint(start, 0L, 0L))).forEach { points ->
            val scale = trendValueScale(points)
            assertEquals(0L, scale.minimum)
            assertTrue(scale.maximum > 0L)
        }
        assertTrue(trendTimeTicks(emptyList(), start, start, AnalyticsTimeScope.MONTH).isEmpty())
    }

    @Test
    fun weeklyTicksShowEveryWeekdayFromMondayToSunday() {
        val monday = LocalDate.of(2026, 11, 9)
        val points = List(7) { AnalyticsTrendPoint(monday.plusDays(it.toLong()), 0L, 0L) }
        val ticks = trendTimeTicks(points, monday, monday.plusDays(6), AnalyticsTimeScope.WEEK)
        assertEquals(listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日"), ticks.map { it.label })
        assertEquals(0f, ticks.first().fraction)
        assertEquals(1f, ticks.last().fraction)
    }

    @Test
    fun partialWeekUsesActualWeekdays() {
        val friday = LocalDate.of(2026, 11, 13)
        val points = List(3) { AnalyticsTrendPoint(friday.plusDays(it.toLong()), 0L, 0L) }
        val ticks = trendTimeTicks(points, friday, friday.plusDays(2), AnalyticsTimeScope.WEEK)
        assertEquals(listOf("周五", "周六", "周日"), ticks.map { it.label })
    }

    @Test
    fun amountAxisUsesStandardPrefixesAndLeavesSmallAmountsUnabbreviated() {
        assertEquals(TrendAmountUnit.YUAN, trendAmountUnit(TrendValueScale(-50000L, 99999L)))
        assertEquals("-500", TrendAmountUnit.YUAN.format(-50000L))
        assertEquals("1.01", TrendAmountUnit.YUAN.format(101L))
        assertEquals(TrendAmountUnit.THOUSAND_YUAN, trendAmountUnit(TrendValueScale(-100_000L, 0L)))
        assertEquals("-1k", TrendAmountUnit.THOUSAND_YUAN.format(-100_000L))
        assertEquals("5k", TrendAmountUnit.THOUSAND_YUAN.format(500_000L))
        assertEquals(TrendAmountUnit.MILLION_YUAN, trendAmountUnit(TrendValueScale(-100_000_000L, 0L)))
        assertEquals("-1M", TrendAmountUnit.MILLION_YUAN.format(-100_000_000L))
        assertEquals(TrendAmountUnit.BILLION_YUAN, trendAmountUnit(TrendValueScale(-100_000_000_000L, 0L)))
        assertEquals("-1G", TrendAmountUnit.BILLION_YUAN.format(-100_000_000_000L))
        assertEquals(TrendAmountUnit.TRILLION_YUAN, trendAmountUnit(TrendValueScale(-Long.MAX_VALUE, Long.MAX_VALUE)))
        assertEquals("1T", TrendAmountUnit.TRILLION_YUAN.format(100_000_000_000_000L))
        assertEquals("0", TrendAmountUnit.YUAN.format(0L))
        assertEquals("0", TrendAmountUnit.THOUSAND_YUAN.format(0L))
        assertEquals("99", TrendAmountUnit.MILLION_YUAN.format(9900L))
    }

    @Test
    fun dailyTicksIncludeSelectedRangeBoundaries() {
        val points = List(30) { AnalyticsTrendPoint(start.plusDays(it.toLong()), 0L, 0L) }
        val ticks = trendTimeTicks(points, start, start.plusDays(29), AnalyticsTimeScope.MONTH)
        assertEquals("11/1", ticks.first().label)
        assertEquals("11/30", ticks.last().label)
        assertEquals(0f, ticks.first().fraction)
        assertEquals(1f, ticks.last().fraction)
        assertTrue(ticks.size <= 5)
    }

    @Test
    fun monthlyTicksDisambiguateCrossYearRanges() {
        val first = LocalDate.of(2025, 12, 1)
        val points = List(3) { AnalyticsTrendPoint(first.plusMonths(it.toLong()), 0L, 0L) }
        val ticks = trendTimeTicks(points, first, LocalDate.of(2026, 2, 28), AnalyticsTimeScope.THREE_MONTHS)
        assertEquals(listOf("2025/12", "2026/1", "2026/2"), ticks.map { it.label })
        assertEquals(listOf(0f, 0.5f, 1f), ticks.map { it.fraction })
    }

    @Test
    fun onePointIsCenteredAndYearlyTicksUseMonths() {
        val single = trendTimeTicks(listOf(AnalyticsTrendPoint(start, 0L, 0L)), start, start, AnalyticsTimeScope.WEEK)
        assertEquals(0.5f, single.single().fraction)
        val first = LocalDate.of(2026, 1, 1)
        val points = List(12) { AnalyticsTrendPoint(first.plusMonths(it.toLong()), 0L, 0L) }
        val yearly = trendTimeTicks(points, first, LocalDate.of(2026, 12, 31), AnalyticsTimeScope.YEAR)
        assertEquals("1月", yearly.first().label)
        assertEquals("12月", yearly.last().label)
    }

    @Test
    fun anomalyDetectionHighlightsSpikesAndIgnoresSteadySpending() {
        val spikes = listOf(1000L, 1100L, 900L, 100000L, 1050L, 950L, 1000L)
        assertEquals(setOf(3), expenseAnomalyIndices(spikes.mapIndexed { i, value -> AnalyticsTrendPoint(start.plusDays(i.toLong()), value, 0L) }))
        assertTrue(expenseAnomalyIndices(List(7) { AnalyticsTrendPoint(start.plusDays(it.toLong()), 1000L, 0L) }).isEmpty())
        assertTrue(expenseAnomalyIndices(emptyList()).isEmpty())
    }

    @Test
    fun anomalyDetectionHandlesSparsePeriodsAndLargeAmounts() {
        val amounts = listOf(0L, 0L, Long.MAX_VALUE, 0L, 0L)
        assertEquals(setOf(2), expenseAnomalyIndices(amounts.mapIndexed { i, value -> AnalyticsTrendPoint(start.plusDays(i.toLong()), value, 0L) }))
    }
}
