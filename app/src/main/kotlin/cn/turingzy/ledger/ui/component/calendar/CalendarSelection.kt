package cn.turingzy.ledger.ui.component.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

internal data class CalendarSelection(val start: LocalDate, val end: LocalDate) {
    init {
        require(!end.isBefore(start))
    }

    operator fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(end)

    fun displayRange(): String {
        val startLabel = start.format(RANGE_START_FORMAT)
        val endLabel = end.format(if (start.year == end.year) RANGE_END_FORMAT else RANGE_START_FORMAT)
        return "$startLabel-$endLabel"
    }

    fun resize(date: LocalDate, startHandle: Boolean, wholeMonths: Boolean): CalendarSelection {
        val candidate = if (wholeMonths) {
            if (startHandle) date.withDayOfMonth(1) else YearMonth.from(date).atEndOfMonth()
        } else {
            date
        }
        return if (startHandle) {
            val boundary = if (wholeMonths) YearMonth.from(end).atEndOfMonth() else end
            CalendarSelection(minOf(candidate, if (wholeMonths) boundary.withDayOfMonth(1) else boundary), boundary)
        } else {
            val boundary = if (wholeMonths) start.withDayOfMonth(1) else start
            CalendarSelection(boundary, maxOf(candidate, if (wholeMonths) YearMonth.from(boundary).atEndOfMonth() else boundary))
        }
    }

    fun resizeYears(date: LocalDate, startHandle: Boolean): CalendarSelection {
        val firstYear = if (startHandle) minOf(date.year, end.year) else start.year
        val lastYear = if (startHandle) end.year else maxOf(date.year, start.year)
        return CalendarSelection(LocalDate.of(firstYear, 1, 1), LocalDate.of(lastYear, 12, 31))
    }

    companion object {
        private val RANGE_START_FORMAT = DateTimeFormatter.ofPattern("yyyy MM.dd")
        private val RANGE_END_FORMAT = DateTimeFormatter.ofPattern("MM.dd")

        fun year(year: Int): CalendarSelection =
            CalendarSelection(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))

        fun week(date: LocalDate): CalendarSelection =
            CalendarSelection(date.with(DayOfWeek.MONDAY), date.with(DayOfWeek.SUNDAY))

        fun month(month: YearMonth): CalendarSelection = CalendarSelection(month.atDay(1), month.atEndOfMonth())
    }
}
