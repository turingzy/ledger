package cn.turingzy.ledger.ui.feature.analytics

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal data class TrendValueScale(val minimum: Long, val maximum: Long)

internal fun trendValueScale(points: List<AnalyticsTrendPoint>): TrendValueScale {
    val values = points.flatMap { listOf(it.income, it.expense, it.income - it.expense) }
    val minimum = minOf(0L, values.minOrNull() ?: 0L)
    val maximum = maxOf(0L, values.maxOrNull() ?: 0L)
    return TrendValueScale(minimum, if (minimum == maximum) 100L else maximum)
}

internal data class TrendTimeTick(val fraction: Float, val label: String)

internal enum class TrendAmountUnit(val label: String, private val cents: Long) {
    YUAN("", 100L),
    THOUSAND_YUAN("k", 100_000L),
    MILLION_YUAN("M", 100_000_000L),
    BILLION_YUAN("G", 100_000_000_000L),
    TRILLION_YUAN("T", 100_000_000_000_000L);

    fun format(amount: Long): String {
        val value = BigDecimal.valueOf(amount)
        val smallAmount = value.abs() < BigDecimal.valueOf(100_000L)
        return value.divide(BigDecimal.valueOf(if (smallAmount) 100L else cents), 2, RoundingMode.HALF_UP)
            .stripTrailingZeros().toPlainString() + if (smallAmount) "" else label
    }
}

internal fun trendAmountUnit(scale: TrendValueScale): TrendAmountUnit {
    val magnitude = maxOf(BigDecimal.valueOf(scale.minimum).abs(), BigDecimal.valueOf(scale.maximum).abs())
    return when {
        magnitude >= BigDecimal.valueOf(100_000_000_000_000L) -> TrendAmountUnit.TRILLION_YUAN
        magnitude >= BigDecimal.valueOf(100_000_000_000L) -> TrendAmountUnit.BILLION_YUAN
        magnitude >= BigDecimal.valueOf(100_000_000L) -> TrendAmountUnit.MILLION_YUAN
        magnitude >= BigDecimal.valueOf(100_000L) -> TrendAmountUnit.THOUSAND_YUAN
        else -> TrendAmountUnit.YUAN
    }
}

internal fun trendTimeTicks(
    points: List<AnalyticsTrendPoint>,
    start: LocalDate,
    end: LocalDate,
    scope: AnalyticsTimeScope,
): List<TrendTimeTick> {
    if (points.isEmpty()) return emptyList()
    if (scope == AnalyticsTimeScope.WEEK) {
        val weekdays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        return points.mapIndexed { index, point ->
            TrendTimeTick(
                fraction = if (points.size == 1) 0.5f else index.toFloat() / points.lastIndex,
                label = weekdays[point.date.dayOfWeek.value - 1],
            )
        }
    }
    val monthly = scope == AnalyticsTimeScope.THREE_MONTHS || scope == AnalyticsTimeScope.YEAR || scope == AnalyticsTimeScope.ALL
    val pattern = when {
        monthly && start.year != end.year -> "yyyy/M"
        monthly -> "M月"
        start.year != end.year -> "yy/M/d"
        else -> "M/d"
    }
    val formatter = DateTimeFormatter.ofPattern(pattern)
    return (0..4).map { it * points.lastIndex / 4 }.distinct().map { index ->
        val date = when (index) {
            0 -> start
            points.lastIndex -> end
            else -> points[index].date
        }
        TrendTimeTick(
            fraction = if (points.size == 1) 0.5f else index.toFloat() / points.lastIndex,
            label = date.format(formatter),
        )
    }
}

internal fun expenseAnomalyIndices(points: List<AnalyticsTrendPoint>): Set<Int> {
    if (points.size < 3) return emptySet()
    val expenses = points.map { BigDecimal.valueOf(it.expense) }.sorted()
    fun median(values: List<BigDecimal>): BigDecimal {
        val middle = values.size / 2
        return if (values.size % 2 == 1) values[middle] else
            values[middle - 1].add(values[middle]).divide(BigDecimal.valueOf(2))
    }
    val baseline = median(expenses)
    val deviation = median(expenses.map { it.subtract(baseline).abs() }.sorted())
    val threshold = maxOf(baseline.multiply(BigDecimal.valueOf(3)), baseline.add(deviation.multiply(BigDecimal.valueOf(3))))
    return points.indices.filter { BigDecimal.valueOf(points[it].expense) > threshold }.toSet()
}
