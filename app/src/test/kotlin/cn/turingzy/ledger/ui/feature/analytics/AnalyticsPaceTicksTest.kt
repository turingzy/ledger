package cn.turingzy.ledger.ui.feature.analytics

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class AnalyticsPaceTicksTest {
    @Test fun weekIncludesEveryDayWithEqualStep() {
        assertEquals((0..6).toList(), expensePaceTickIndices(7))
    }
    @Test fun sparseTicksUseUniformStepsWithoutAddingAnUnevenLastInterval() {
        listOf(28, 29, 30, 31, 12, 20).forEach { count ->
            val ticks = expensePaceTickIndices(count)
            assertEquals(0, ticks.first())
            assertTrue(ticks.last() < count)
            assertEquals(1, ticks.zipWithNext { first, second -> second - first }.distinct().size)
        }
        assertEquals(listOf(0, 7, 14, 21, 28), expensePaceTickIndices(31))
        assertEquals(listOf(0, 3, 6, 9), expensePaceTickIndices(12))
    }
    @Test fun yearIncludesAllTwelveMonthsWithEqualStep() {
        val ticks = expensePaceTickIndices(12, AnalyticsTimeScope.YEAR)
        assertEquals((0..11).toList(), ticks)
        val start = LocalDate.of(2026, 1, 1)
        assertEquals((1..12).map { "${it}月" }, ticks.map { expensePaceDateLabel(AnalyticsTimeScope.YEAR, start.plusMonths(it.toLong())) })
    }
    @Test fun labelsUseMonthAndDayAndKeepCalendarMeaningsInOtherScopes() {
        val date = LocalDate.of(2026, 11, 1)
        assertEquals("11.1", expensePaceDateLabel(AnalyticsTimeScope.MONTH, date))
        assertEquals("周日", expensePaceDateLabel(AnalyticsTimeScope.WEEK, date))
        assertEquals("11月", expensePaceDateLabel(AnalyticsTimeScope.YEAR, date))
        assertEquals("2026", expensePaceDateLabel(AnalyticsTimeScope.ALL, date))
    }
    @Test fun singlePointHasOnlyOneTick() {
        assertEquals(listOf(0), expensePaceTickIndices(1))
        assertTrue(expensePaceTickIndices(0).isEmpty())
    }
}
