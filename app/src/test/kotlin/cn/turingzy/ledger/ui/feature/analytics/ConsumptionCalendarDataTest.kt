package cn.turingzy.ledger.ui.feature.analytics

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class ConsumptionCalendarDataTest {
    private val today = LocalDate.of(2026, 11, 14)

    @Test fun leapMonthUsesMondayFirstAndIncludesLeapDay() {
        val data = consumptionCalendarData(emptyMap(), YearMonth.of(2024, 2), today)
        assertEquals(35, data.cells.size)
        assertTrue(data.cells.take(3).all { it == null })
        assertEquals(LocalDate.of(2024, 2, 1), data.cells[3])
        assertEquals(LocalDate.of(2024, 2, 29), data.cells.filterNotNull().last())
        assertEquals(29, data.cells.filterNotNull().size)
        val sixWeeks = consumptionCalendarData(emptyMap(), YearMonth.of(2026, 3), today)
        assertEquals(42, sixWeeks.cells.size)
        assertEquals(LocalDate.of(2026, 3, 1), sixWeeks.cells[6])
    }

    @Test fun summaryExcludesIncomeOtherMonthsAndFutureDates() {
        val month = YearMonth.from(today)
        val data = consumptionCalendarData(mapOf(
            month.atDay(1) to AnalyticsDay(100L, 99900L),
            month.atDay(2) to AnalyticsDay(300L),
            month.atDay(3) to AnalyticsDay(income = 12300L),
            today.plusDays(1) to AnalyticsDay(50000L),
            month.atDay(1).minusDays(1) to AnalyticsDay(70000L),
        ), month, today)
        assertEquals(400L, data.totalExpense)
        assertEquals(2, data.spendingDays)
        assertEquals(300L, data.highestExpense)
        assertFalse(data.levels.containsKey(today.plusDays(1)))
    }

    @Test fun heatLevelsKeepSmallExpensesVisibleAndRespectBoundaries() {
        val amounts = listOf(1L, 25L, 26L, 50L, 51L, 75L, 76L, 100L)
        val days = amounts.mapIndexed { index, amount -> today.minusDays(index.toLong()) to AnalyticsDay(amount) }.toMap()
        val data = consumptionCalendarData(days, YearMonth.from(today), today)
        assertEquals(listOf(1, 1, 2, 2, 3, 3, 4, 4), days.keys.map { data.levels[it] })
    }

    @Test fun emptyMonthAndMaximumCentAmountAreSafe() {
        val empty = consumptionCalendarData(emptyMap(), YearMonth.from(today), today)
        assertEquals(0L, empty.totalExpense)
        assertEquals(0, empty.spendingDays)
        assertEquals(0L, empty.highestExpense)
        assertTrue(empty.levels.isEmpty())
        val maximum = consumptionCalendarData(mapOf(today to AnalyticsDay(Long.MAX_VALUE)), YearMonth.from(today), today)
        assertEquals(Long.MAX_VALUE, maximum.totalExpense)
        assertEquals(4, maximum.levels[today])
    }
}
