package cn.turingzy.ledger.ui.feature.analytics

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

internal data class AnalyticsExpenseAverage(
    val label: String,
    val amount: Long,
    val direction: Int,
    val change: String,
)

private fun averageUnitCount(scope: AnalyticsTimeScope, start: LocalDate, end: LocalDate): Long = when (scope) {
    AnalyticsTimeScope.YEAR -> ChronoUnit.MONTHS.between(YearMonth.from(start), YearMonth.from(end)) + 1
    AnalyticsTimeScope.ALL -> end.year.toLong() - start.year + 1
    else -> ChronoUnit.DAYS.between(start, end) + 1
}

internal fun analyticsExpenseAverage(
    scope: AnalyticsTimeScope,
    expense: Long,
    start: LocalDate,
    end: LocalDate,
    previousExpense: Long,
    previousStart: LocalDate,
    previousEnd: LocalDate,
): AnalyticsExpenseAverage {
    require(end >= start && previousEnd >= previousStart)
    require(expense >= 0L && previousExpense >= 0L)
    val units = averageUnitCount(scope, start, end)
    val previousUnits = averageUnitCount(scope, previousStart, previousEnd)
    val current = BigDecimal.valueOf(expense).multiply(BigDecimal.valueOf(previousUnits))
    val previous = BigDecimal.valueOf(previousExpense).multiply(BigDecimal.valueOf(units))
    val direction = current.compareTo(previous)
    val magnitude = if (previous.signum() == 0) null else current.subtract(previous).abs()
        .multiply(BigDecimal.valueOf(100)).divide(previous, 1, RoundingMode.HALF_UP)
    val change = when {
        direction == 0 -> "环比持平"
        magnitude == null -> "暂无环比"
        else -> (if (direction > 0) "环比增长 " else "环比下降 ") +
            (if (magnitude.signum() == 0) "<0.1" else magnitude.stripTrailingZeros().toPlainString()) + "%"
    }
    return AnalyticsExpenseAverage(
        label = when (scope) { AnalyticsTimeScope.YEAR -> "月均支出"; AnalyticsTimeScope.ALL -> "年均支出"; else -> "日均支出" },
        amount = BigDecimal.valueOf(expense).divide(BigDecimal.valueOf(units), 0, RoundingMode.HALF_UP).longValueExact(),
        direction = if (magnitude == null) 0 else direction,
        change = change,
    )
}
