package cn.turingzy.ledger.ui.feature.analytics

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal data class AnalyticsDailyAverage(val dayCount: Long, val expense: Long, val income: Long)

internal fun analyticsDailyAverage(
    totalExpense: Long,
    totalIncome: Long,
    start: LocalDate,
    end: LocalDate,
): AnalyticsDailyAverage {
    require(!end.isBefore(start))
    require(totalExpense >= 0L && totalIncome >= 0L)
    val days = ChronoUnit.DAYS.between(start, end) + 1
    fun average(total: Long) = BigDecimal.valueOf(total)
        .divide(BigDecimal.valueOf(days), 0, RoundingMode.HALF_UP).longValueExact()
    return AnalyticsDailyAverage(days, average(totalExpense), average(totalIncome))
}
