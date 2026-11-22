package cn.turingzy.ledger.ui.feature.analytics.calculation

import cn.turingzy.ledger.ui.feature.analytics.AnalyticsTimeScope
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * 支出进度曲线图表数据模型。
 *
 * @property dates 每个分段桶的起始日期列表（X 轴刻度）。
 * @property current 当期截止到当前节点的累计支出列表。
 * @property previous 上一对比周期对应节点的累计支出列表（已对齐当期长度）。
 * @property scale Y 轴顶格刻度上限值（分）。
 */
internal data class ExpensePaceData(
    val dates: List<LocalDate>,
    val current: List<Long>,
    val previous: List<Long>,
    val scale: Long,
)

/**
 * 根据时间粒度将起止日期拆分为一组时间区间桶。
 *
 * - 按年粒度：按月份拆分。
 * - 全部/自定义粒度：按年份拆分。
 * - 其他（周、月）：按每日拆分。
 *
 * @param scope 统计时间粒度。
 * @param start 起始日期。
 * @param end 结束日期。
 * @return 每一段子区间的起止日期对列表。
 */
private fun paceBuckets(scope: AnalyticsTimeScope, start: LocalDate, end: LocalDate): List<Pair<LocalDate, LocalDate>> {
    require(end >= start)
    return when (scope) {
        AnalyticsTimeScope.YEAR -> List(ChronoUnit.MONTHS.between(YearMonth.from(start), YearMonth.from(end)).toInt() + 1) { index ->
            val month = YearMonth.from(start).plusMonths(index.toLong())
            maxOf(start, month.atDay(1)) to minOf(end, month.atEndOfMonth())
        }
        AnalyticsTimeScope.ALL -> (start.year..end.year).map { year ->
            maxOf(start, LocalDate.of(year, 1, 1)) to minOf(end, LocalDate.of(year, 12, 31))
        }
        else -> List(ChronoUnit.DAYS.between(start, end).toInt() + 1) { index -> start.plusDays(index.toLong()).let { it to it } }
    }
}

/**
 * 计算当期与上期的支出进度走势数据及坐标轴刻度。
 *
 * @param days 按日映射的统计收支数据。
 * @param scope 统计时间粒度。
 * @param start 当期起始日期。
 * @param end 当期结束日期。
 * @param today 今天（用作当前周期累计的截止截断点）。
 * @return 组装好的 [ExpensePaceData]。
 */
internal fun expensePaceData(days: Map<LocalDate, AnalyticsDay>, scope: AnalyticsTimeScope, start: LocalDate, end: LocalDate, today: LocalDate): ExpensePaceData {
    val buckets = paceBuckets(scope, start, end)
    val preceding = previousAnalyticsRange(start, end)
    val previousBuckets = paceBuckets(scope, preceding.start, preceding.end)
    fun cumulative(ranges: List<Pair<LocalDate, LocalDate>>, cutoff: LocalDate): List<Long> {
        var total = 0L
        return ranges.takeWhile { it.first <= cutoff }.map { (from, to) ->
            total += days.filterKeys { it >= from && it <= minOf(to, cutoff) }.values.sumOf { it.expense }
            total
        }
    }
    val current = cumulative(buckets, today)
    val previous = cumulative(previousBuckets, preceding.end)
    val alignedPrevious = buckets.indices.map { previous[minOf(it, previous.lastIndex)] }
    val observedMaximum = (alignedPrevious + current).maxOrNull() ?: 0L
    val maximum = if (observedMaximum == 0L) 100L else observedMaximum.coerceAtLeast(4L)
    val roughStep = BigDecimal.valueOf(maximum).divide(BigDecimal.valueOf(4))
    val magnitude = BigDecimal.TEN.pow((roughStep.precision() - roughStep.scale() - 1).coerceAtLeast(0))
    val unit = roughStep.divide(magnitude)
    val multiplier = listOf("1", "2", "2.5", "3", "4", "5", "7.5", "10").map(::BigDecimal).first { unit <= it }
    val step = magnitude.multiply(multiplier).setScale(0, RoundingMode.CEILING)
    val scale = step.multiply(BigDecimal.valueOf(4)).min(BigDecimal.valueOf(Long.MAX_VALUE)).longValueExact()
    return ExpensePaceData(buckets.map { it.first }, current, alignedPrevious, scale)
}

/**
 * 格式化支出进度环比变动文案。
 *
 * @param current 当期支出金额。
 * @param previous 上期支出金额。
 * @return 环比文案，如 "持平"、"暂无环比"、"增长 12.5%" 或 "下降 3%"。
 */
internal fun expensePaceChangeLabel(current: Long, previous: Long): String {
    if (current == previous) return "持平"
    if (previous == 0L) return "暂无环比"
    val percentage = BigDecimal.valueOf(current).subtract(BigDecimal.valueOf(previous)).abs()
        .multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(previous), 1, RoundingMode.HALF_UP)
    val magnitude = if (percentage.signum() == 0) "<0.1" else percentage.stripTrailingZeros().toPlainString()
    return (if (current > previous) "增长 " else "下降 ") + magnitude + "%"
}
