package cn.turingzy.ledger.ui.feature.ledger

import cn.turingzy.ledger.ui.component.weekLabel
import cn.turingzy.ledger.ui.component.weekStart
import cn.turingzy.ledger.ui.component.periodLabel
import cn.turingzy.ledger.ui.component.periodStart
import cn.turingzy.ledger.ui.component.PeriodSeries
import cn.turingzy.ledger.model.DateFilterType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class LedgerPeriodTest {
    @Test
    fun weekLabelIncludesBothYearsWhenWeekCrossesNewYear() {
        val anchor = LocalDate.of(2021, 1, 1)
        assertEquals(LocalDate.of(2020, 12, 28), weekStart(anchor))
        assertEquals("2020.12.28–2021.01.03", weekLabel(anchor))
        assertEquals("2024.02.26–03.03", weekLabel(LocalDate.of(2024, 2, 29)))
    }

    @Test
    fun scrollingAdvancesWholePeriodsAcrossCalendarBoundaries() {
        val anchor = LocalDate.of(2024, 2, 29)
        val cases = listOf(
            Triple(DateFilterType.WEEK, LocalDate.of(2024, 2, 26), LocalDate.of(2024, 3, 4)),
            Triple(DateFilterType.MONTH, LocalDate.of(2024, 2, 1), LocalDate.of(2024, 3, 1)),
            Triple(DateFilterType.YEAR, LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1)),
        )
        cases.forEach { (type, current, next) ->
            val series = PeriodSeries(type, anchor)
            val index = series.indexOf(anchor)
            assertEquals(current, series.dateAt(index))
            assertEquals(next, series.dateAt(index + 1))
            assertEquals(index + 1, series.indexOf(next))
        }
        val weeks = PeriodSeries(DateFilterType.WEEK, anchor)
        assertEquals(LocalDate.of(2020, 12, 28), weeks.dateAt(weeks.indexOf(LocalDate.of(2021, 1, 1))))
    }

    @Test
    fun scrollRangeIncludesTheAnchorAndClampsOutsideDates() {
        listOf(1800, 2024, 2200).forEach { year ->
            listOf(DateFilterType.WEEK, DateFilterType.MONTH, DateFilterType.YEAR).forEach { type ->
                val anchor = LocalDate.of(year, 6, 15)
                val series = PeriodSeries(type, anchor)
                assertEquals(periodStart(type, anchor), series.dateAt(series.indexOf(anchor)))
                assertEquals(0, series.indexOf(LocalDate.of(1700, 1, 1)))
                assertEquals(series.size - 1, series.indexOf(LocalDate.of(2300, 1, 1)))
            }
        }
    }

    @Test
    fun calendarAnchorMapsToTheSelectedPeriod() {
        val leapDay = LocalDate.of(2024, 2, 29)
        assertEquals(LocalDate.of(2024, 2, 26), periodStart(DateFilterType.WEEK, leapDay))
        assertEquals(LocalDate.of(2024, 2, 1), periodStart(DateFilterType.MONTH, leapDay))
        assertEquals(LocalDate.of(2024, 1, 1), periodStart(DateFilterType.YEAR, leapDay))
        assertEquals("2024.02", periodLabel(DateFilterType.MONTH, leapDay))
        assertEquals("2024", periodLabel(DateFilterType.YEAR, leapDay))
    }
}
