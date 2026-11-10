package cn.turingzy.ledger.ui.feature.analytics

import cn.turingzy.ledger.ui.component.calendar.CalendarSelection
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

internal fun previousAnalyticsRange(start: LocalDate, end: LocalDate): CalendarSelection {
    require(!end.isBefore(start))
    if (start.dayOfMonth == 1 && end == YearMonth.from(end).atEndOfMonth()) {
        val months = ChronoUnit.MONTHS.between(YearMonth.from(start), YearMonth.from(end)) + 1
        return CalendarSelection(start.minusMonths(months), start.minusDays(1))
    }
    val days = ChronoUnit.DAYS.between(start, end) + 1
    return CalendarSelection(start.minusDays(days), start.minusDays(1))
}

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
