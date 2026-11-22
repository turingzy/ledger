package cn.turingzy.ledger.ui.feature.analytics.calculation

import cn.turingzy.ledger.ui.component.picker.CalendarSelection
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * 计算给定日期范围对应的上一对比周期的日期区间。
 *
 * 若当前范围覆盖整月（起始为月初第一天且结束为月末最后一天），则按月份跨度向前推移等长月数；
 * 否则按天数向前推移等长天数。
 *
 * @param start 当前周期的起始日期。
 * @param end 当前周期的结束日期，不能早于 [start]。
 * @return 上一对比周期的 [CalendarSelection]。
 * @throws IllegalArgumentException 如果 [end] 早于 [start]。
 */
internal fun previousAnalyticsRange(start: LocalDate, end: LocalDate): CalendarSelection {
    require(!end.isBefore(start))
    if (start.dayOfMonth == 1 && end == YearMonth.from(end).atEndOfMonth()) {
        val months = ChronoUnit.MONTHS.between(YearMonth.from(start), YearMonth.from(end)) + 1
        return CalendarSelection(start.minusMonths(months), start.minusDays(1))
    }
    val days = ChronoUnit.DAYS.between(start, end) + 1
    return CalendarSelection(start.minusDays(days), start.minusDays(1))
}

/**
 * 计算当期数值对比上一期数值的变化率格式化标签（带升降箭头）。
 *
 * @param current 当期数值。
 * @param previous 上一期数值。
 * @return 格式化后的百分比变化标签，例如 "↑15.2%"、"↓8%"、"0%" 或 "↑<0.1%"。
 */
internal fun comparisonChangeLabel(current: Long, previous: Long): String {
    if (current == previous) return "0%"
    if (previous == 0L) return "100%"
    val percentage = BigDecimal.valueOf(current).subtract(BigDecimal.valueOf(previous))
        .multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(previous).abs(), 1, RoundingMode.HALF_UP)
    val prefix = if (current > previous) "↑" else "↓"
    val magnitude = percentage.abs()
    return if (magnitude.signum() == 0) "$prefix<0.1%" else "$prefix${magnitude.stripTrailingZeros().toPlainString()}%"
}
