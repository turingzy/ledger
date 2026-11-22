package cn.turingzy.ledger.ui.feature.analytics.calculation

import cn.turingzy.ledger.ui.feature.analytics.AnalyticsTimeScope
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * 统计分析中的平均支出指标模型。
 *
 * @property label 平均维度的显示文案（如 "日均支出"、"月均支出"、"年均支出"）。
 * @property amount 对应的平均支出金额（分）。
 * @property direction 环比变动方向：1 为增长，-1 为下降，0 为持平或无基准。
 * @property change 环比变动描述文本（如 "环比增长 12.5%"、"环比持平"、"暂无环比"）。
 */
internal data class AnalyticsExpenseAverage(
    val label: String,
    val amount: Long,
    val direction: Int,
    val change: String,
)

/**
 * 根据时间粒度获取平均值计算的时间单位数。
 *
 * - 按年粒度：返回月数。
 * - 全部/自定义粒度：返回年数。
 * - 其他（周、月）：返回天数。
 *
 * @param scope 时间粒度。
 * @param start 起始日期。
 * @param end 结束日期。
 * @return 对应的单位跨度数。
 */
private fun averageUnitCount(scope: AnalyticsTimeScope, start: LocalDate, end: LocalDate): Long = when (scope) {
    AnalyticsTimeScope.YEAR -> ChronoUnit.MONTHS.between(YearMonth.from(start), YearMonth.from(end)) + 1
    AnalyticsTimeScope.ALL -> end.year.toLong() - start.year + 1
    else -> ChronoUnit.DAYS.between(start, end) + 1
}

/**
 * 计算当期的平均支出及相对上期的环比变化。
 *
 * @param scope 统计时间粒度。
 * @param expense 当期总支出（分）。
 * @param start 当期起始日期。
 * @param end 当期结束日期。
 * @param previousExpense 上期总支出（分）。
 * @param previousStart 上期起始日期。
 * @param previousEnd 上期结束日期。
 * @return 包含维度标签、均值金额与环比说明的 [AnalyticsExpenseAverage]。
 * @throws IllegalArgumentException 如果结束日期早于开始日期，或支出金额为负。
 */
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
