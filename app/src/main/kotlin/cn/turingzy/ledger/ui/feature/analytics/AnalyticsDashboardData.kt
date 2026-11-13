package cn.turingzy.ledger.ui.feature.analytics

import cn.turingzy.ledger.model.TransactionDetail
import cn.turingzy.ledger.model.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.Instant

data class AnalyticsDay(val expense: Long = 0L, val income: Long = 0L)
internal data class AnalyticsMonth(val month: YearMonth, val expense: Long, val income: Long) {
    val balance: Long get() = income - expense
}

internal fun analyticsDays(entries: List<TransactionDetail>, zone: ZoneId): Map<LocalDate, AnalyticsDay> =
    entries.filter { !it.transaction.isDeleted && it.transaction.type != TransactionType.TRANSFER }
        .groupBy { Instant.ofEpochMilli(it.transaction.transactionTime).atZone(zone).toLocalDate() }
        .mapValues { (_, daily) -> AnalyticsDay(
            daily.filter { it.transaction.type == TransactionType.EXPENSE }.sumOf { it.transaction.amount },
            daily.filter { it.transaction.type == TransactionType.INCOME }.sumOf { it.transaction.amount },
        ) }

internal fun monthlyAnalytics(days: Map<LocalDate, AnalyticsDay>, end: YearMonth): List<AnalyticsMonth> {
    val first = days.keys.minOrNull()?.let(YearMonth::from) ?: end.minusMonths(5)
    val start = minOf(first, end.minusMonths(5))
    val count = java.time.temporal.ChronoUnit.MONTHS.between(start, end).toInt() + 1
    val totals = days.entries.groupBy { YearMonth.from(it.key) }
    return List(count.coerceAtLeast(6)) { offset ->
        val month = start.plusMonths(offset.toLong())
        val entries = totals[month].orEmpty()
        AnalyticsMonth(month, entries.sumOf { it.value.expense }, entries.sumOf { it.value.income })
    }
}

internal fun cumulativeExpenses(days: Map<LocalDate, AnalyticsDay>, month: YearMonth): List<Long> {
    var total = 0L
    return (1..month.lengthOfMonth()).map { day -> total += days[month.atDay(day)]?.expense ?: 0L; total }
}

internal fun projectExpense(amount: Long, elapsedDays: Int, targetDay: Int): Long =
    if (elapsedDays <= 0) 0L else BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(targetDay.toLong()))
        .divide(BigDecimal.valueOf(elapsedDays.toLong()), 0, RoundingMode.HALF_UP)
        .min(BigDecimal.valueOf(Long.MAX_VALUE)).longValueExact()
