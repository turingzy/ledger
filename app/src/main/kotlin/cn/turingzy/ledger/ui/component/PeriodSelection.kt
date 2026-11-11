package cn.turingzy.ledger.ui.component

import cn.turingzy.ledger.model.DateFilterType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

internal fun weekStart(date: LocalDate): LocalDate = date.with(DayOfWeek.MONDAY)

internal fun weekLabel(date: LocalDate): String {
    val start = weekStart(date)
    val end = start.plusDays(6)
    val first = start.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
    val last = end.format(DateTimeFormatter.ofPattern(if (start.year == end.year) "MM.dd" else "yyyy.MM.dd"))
    return "$first–$last"
}

internal fun periodStart(type: DateFilterType, date: LocalDate): LocalDate = when (type) {
    DateFilterType.WEEK -> weekStart(date)
    DateFilterType.MONTH -> date.withDayOfMonth(1)
    DateFilterType.YEAR -> date.withDayOfYear(1)
    DateFilterType.RANGE -> date
}

internal fun periodLabel(type: DateFilterType, date: LocalDate): String = when (type) {
    DateFilterType.WEEK -> weekLabel(date)
    DateFilterType.MONTH -> date.format(DateTimeFormatter.ofPattern("yyyy.MM"))
    DateFilterType.YEAR -> date.year.toString()
    DateFilterType.RANGE -> date.toString()
}

internal val DateFilterType.currentPeriodLabel: String
    get() = when (this) {
        DateFilterType.WEEK -> "本周"
        DateFilterType.MONTH -> "本月"
        DateFilterType.YEAR -> "本年"
        DateFilterType.RANGE -> "本月"
    }

internal val DateFilterType.returnLabel: String
    get() = "返回$currentPeriodLabel"

internal class PeriodSeries(private val type: DateFilterType, reference: LocalDate) {
    init { require(type != DateFilterType.RANGE) }

    private val first = periodStart(type, LocalDate.of(minOf(1900, reference.year), 1, 1))
    private val last = periodStart(type, LocalDate.of(maxOf(2100, reference.year), 12, 31))
    private val unit = when (type) {
        DateFilterType.WEEK -> ChronoUnit.WEEKS
        DateFilterType.MONTH -> ChronoUnit.MONTHS
        DateFilterType.YEAR -> ChronoUnit.YEARS
        DateFilterType.RANGE -> error("Range is not a scrollable period")
    }
    val size: Int = Math.toIntExact(unit.between(first, last) + 1)

    fun dateAt(index: Int): LocalDate {
        require(index in 0 until size)
        return first.plus(index.toLong(), unit)
    }

    fun indexOf(date: LocalDate): Int = unit.between(first, periodStart(type, date))
        .coerceIn(0L, size.toLong() - 1).toInt()
}
