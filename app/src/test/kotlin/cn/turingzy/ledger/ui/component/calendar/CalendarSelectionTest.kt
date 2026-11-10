package cn.turingzy.ledger.ui.component.calendar

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarSelectionTest {
    @Test
    fun defaultWeekCrossesYearWithMondayAndSundayBoundaries() {
        assertEquals(range("2025-12-29", "2026-01-04"), CalendarSelection.week(date("2026-01-01")))
    }

    @Test
    fun sameYearRangeShowsYearOnceAndPadsDates() {
        assertEquals("2026 11.01-12.01", range("2026-11-01", "2026-12-01").displayRange())
    }

    @Test
    fun crossYearRangeKeepsBothYears() {
        assertEquals("2026 12.28-2027 01.03", range("2026-12-28", "2027-01-03").displayRange())
    }

    @Test
    fun monthAndYearPresetsIncludeTheEntirePeriod() {
        assertEquals(range("2028-02-01", "2028-02-29"), CalendarSelection.month(YearMonth.of(2028, 2)))
        assertEquals(range("2028-01-01", "2028-12-31"), CalendarSelection.year(2028))
    }

    @Test
    fun dayRangeCanExtendAcrossMonthsAndCannotInvert() {
        val selection = range("2026-11-02", "2026-11-08")
        assertEquals(range("2026-10-25", "2026-11-08"), selection.resize(date("2026-10-25"), true, false))
        assertEquals(range("2026-11-02", "2026-12-09"), selection.resize(date("2026-12-09"), false, false))
        assertEquals(range("2026-11-08", "2026-11-08"), selection.resize(date("2026-12-01"), true, false))
    }

    @Test
    fun monthResizeSnapsBothEndpointsToWholeMonths() {
        val selection = range("2028-02-07", "2028-02-13")
        assertEquals(range("2028-02-01", "2028-04-30"), selection.resize(date("2028-04-10"), false, true))
        assertEquals(range("2027-12-01", "2028-02-29"), selection.resize(date("2027-12-10"), true, true))
    }

    @Test
    fun monthHandlesCannotCrossIntoPartialMonth() {
        val selection = range("2026-03-01", "2026-06-30")
        assertEquals(range("2026-06-01", "2026-06-30"), selection.resize(date("2026-08-01"), true, true))
        assertEquals(range("2026-03-01", "2026-03-31"), selection.resize(date("2026-01-01"), false, true))
    }

    @Test
    fun yearResizeSnapsToWholeYearsAndPreventsCrossedHandles() {
        val selection = range("2028-02-07", "2030-06-13")
        assertEquals(range("2027-01-01", "2030-12-31"), selection.resizeYears(date("2027-06-10"), true))
        assertEquals(range("2028-01-01", "2032-12-31"), selection.resizeYears(date("2032-02-29"), false))
        assertEquals(range("2030-01-01", "2030-12-31"), selection.resizeYears(date("2035-01-01"), true))
        assertEquals(range("2028-01-01", "2028-12-31"), selection.resizeYears(date("2025-01-01"), false))
    }

    private fun date(value: String) = LocalDate.parse(value)
    private fun range(start: String, end: String) = CalendarSelection(date(start), date(end))
}
