package cn.turingzy.ledger.ui.feature.analytics

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

internal data class ExpensePaceData(
    val dates: List<LocalDate>,
    val current: List<Long>,
    val previous: List<Long>,
    val scale: Long,
)

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

internal fun expensePaceChangeLabel(current: Long, previous: Long): String {
    if (current == previous) return "持平"
    if (previous == 0L) return "暂无环比"
    val percentage = BigDecimal.valueOf(current).subtract(BigDecimal.valueOf(previous)).abs()
        .multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(previous), 1, RoundingMode.HALF_UP)
    val magnitude = if (percentage.signum() == 0) "<0.1" else percentage.stripTrailingZeros().toPlainString()
    return (if (current > previous) "增长 " else "下降 ") + magnitude + "%"
}
