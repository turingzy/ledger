package cn.turingzy.ledger.ui.feature.analytics

import cn.turingzy.ledger.ui.component.calendar.CalendarSelection
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsDateRangeTest {
    @Test
    fun singleYearShowsOnlyTheYearIncludingLeapYears() {
        assertEquals("2024", CalendarSelection.year(2024).analyticsDisplayRange())
        assertEquals("2026", CalendarSelection.year(2026).analyticsDisplayRange())
    }

    @Test
    fun consecutiveYearsShowYearRange() {
        val range = CalendarSelection(LocalDate.of(2024, 1, 1), LocalDate.of(2026, 12, 31))
        assertEquals("2024–2026", range.analyticsDisplayRange())
    }

    @Test
    fun partialYearAndCrossYearWeekKeepExactDates() {
        val partial = CalendarSelection(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 11, 30))
        assertEquals("2024 01.01-11.30", partial.analyticsDisplayRange())
        val week = CalendarSelection.week(LocalDate.of(2026, 1, 1))
        assertEquals("2025 12.29-2026 01.04", week.analyticsDisplayRange())
    }
}
