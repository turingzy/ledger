package cn.turingzy.ledger.ui.feature.analytics.calculation

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 统计分析中的日均收支数据模型。
 *
 * @property dayCount 统计区间内的总天数。
 * @property expense 日均支出金额（分），四舍五入。
 * @property income 日均收入金额（分），四舍五入。
 */
internal data class AnalyticsDailyAverage(val dayCount: Long, val expense: Long, val income: Long)

/**
 * 计算指定日期区间内的日均收支金额。
 *
 * @param totalExpense 区间总支出金额（分），必须非负。
 * @param totalIncome 区间总收入金额（分），必须非负。
 * @param start 区间起始日期。
 * @param end 区间结束日期，不能早于起始日期。
 * @return 包含天数与日均收支的 [AnalyticsDailyAverage] 对象。
 * @throws IllegalArgumentException 如果 [end] 早于 [start]，或金额小于 0。
 */
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
