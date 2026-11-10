package cn.turingzy.ledger.ui.feature.analytics

import cn.turingzy.ledger.ui.component.calendar.CalendarSelection
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsComparisonTest {
    @Test
    fun weeklyComparisonCrossesYearWithoutOverlap() {
        assertEquals(range("2025-12-22", "2025-12-28"), previousAnalyticsRange(date("2025-12-29"), date("2026-01-04")))
    }

    @Test
    fun monthlyComparisonUsesPreviousCalendarMonthRatherThanDayCount() {
        assertEquals(range("2024-01-01", "2024-01-31"), previousAnalyticsRange(date("2024-02-01"), date("2024-02-29")))
        assertEquals(range("2024-02-01", "2024-02-29"), previousAnalyticsRange(date("2024-03-01"), date("2024-03-31")))
    }

    @Test
    fun yearlyAndThreeMonthComparisonsPreserveCalendarBoundaries() {
        assertEquals(range("2023-01-01", "2023-12-31"), previousAnalyticsRange(date("2024-01-01"), date("2024-12-31")))
        assertEquals(range("2023-09-01", "2023-11-30"), previousAnalyticsRange(date("2023-12-01"), date("2024-02-29")))
        assertEquals(range("2020-01-01", "2022-12-31"), previousAnalyticsRange(date("2023-01-01"), date("2025-12-31")))
    }

    @Test
    fun customComparisonUsesImmediatelyPrecedingEqualNumberOfDays() {
        assertEquals(range("2024-02-20", "2024-02-26"), previousAnalyticsRange(date("2024-02-27"), date("2024-03-04")))
        assertEquals(range("2024-02-28", "2024-02-28"), previousAnalyticsRange(date("2024-02-29"), date("2024-02-29")))
    }

    @Test
    fun changesHandleGrowthDeclineAndZeroBaseline() {
        assertEquals("↑25%", comparisonChangeLabel(12500L, 10000L))
        assertEquals("↓25%", comparisonChangeLabel(7500L, 10000L))
        assertEquals("0%", comparisonChangeLabel(0L, 0L))
        assertEquals("0%", comparisonChangeLabel(100L, 100L))
        assertEquals("100%", comparisonChangeLabel(100L, 0L))
        assertEquals("100%", comparisonChangeLabel(-100L, 0L))
        assertEquals("↓100%", comparisonChangeLabel(0L, 100L))
        assertEquals("↑<0.1%", comparisonChangeLabel(1000001L, 1000000L))
    }

    @Test
    fun negativeBalancesUseAbsoluteBaselineAndAvoidLongOverflow() {
        assertEquals("↑50%", comparisonChangeLabel(-5000L, -10000L))
        assertEquals("↓50%", comparisonChangeLabel(-15000L, -10000L))
        assertEquals("↑200%", comparisonChangeLabel(10000L, -10000L))
        assertEquals("↑200%", comparisonChangeLabel(Long.MAX_VALUE, -Long.MAX_VALUE))
    }

    private fun date(value: String) = LocalDate.parse(value)
    private fun range(start: String, end: String) = CalendarSelection(date(start), date(end))
}
