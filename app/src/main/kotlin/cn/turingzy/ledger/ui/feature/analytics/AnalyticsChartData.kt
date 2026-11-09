package cn.turingzy.ledger.ui.feature.analytics

import cn.turingzy.ledger.model.TransactionDetail
import cn.turingzy.ledger.model.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.time.temporal.ChronoUnit

internal fun analyticsRatio(amount: Long, total: Long): Float {
    if (total <= 0L) return 0f
    return BigDecimal.valueOf(amount)
        .divide(BigDecimal.valueOf(total), 6, RoundingMode.HALF_UP).toFloat()
}

internal fun buildAnalyticsTrend(
    transactions: List<TransactionDetail>,
    scope: AnalyticsTimeScope,
    anchor: LocalDate,
    zone: ZoneId,
    endDate: LocalDate? = null,
): List<AnalyticsTrendPoint> {
    val dates = if (endDate != null) {
        if (scope == AnalyticsTimeScope.YEAR) {
            val firstMonth = YearMonth.from(anchor)
            List(ChronoUnit.MONTHS.between(firstMonth, YearMonth.from(endDate)).toInt() + 1) {
                firstMonth.plusMonths(it.toLong()).atDay(1)
            }
        } else {
            List(ChronoUnit.DAYS.between(anchor, endDate).toInt() + 1) { anchor.plusDays(it.toLong()) }
        }
    } else when (scope) {
        AnalyticsTimeScope.WEEK -> {
            val monday = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            List(7) { monday.plusDays(it.toLong()) }
        }

        AnalyticsTimeScope.MONTH -> {
            val month = YearMonth.from(anchor)
            List(month.lengthOfMonth()) { month.atDay(it + 1) }
        }

        AnalyticsTimeScope.YEAR -> List(12) { LocalDate.of(anchor.year, it + 1, 1) }
    }
    val rangeStart = if (endDate != null) anchor else dates.first()
    val rangeEnd = endDate ?: if (scope == AnalyticsTimeScope.YEAR) YearMonth.from(dates.last()).atEndOfMonth() else dates.last()
    val grouped = transactions.filter {
        val date = Instant.ofEpochMilli(it.transaction.transactionTime).atZone(zone).toLocalDate()
        !it.transaction.isDeleted && !date.isBefore(rangeStart) && !date.isAfter(rangeEnd)
    }.groupBy {
        val date = Instant.ofEpochMilli(it.transaction.transactionTime).atZone(zone).toLocalDate()
        if (scope == AnalyticsTimeScope.YEAR) date.withDayOfMonth(1) else date
    }
    return dates.map { date ->
        val entries = grouped[date].orEmpty()
        AnalyticsTrendPoint(
            date = date,
            expense = entries.filter { it.transaction.type == TransactionType.EXPENSE }
                .sumOf { it.transaction.amount },
            income = entries.filter { it.transaction.type == TransactionType.INCOME }
                .sumOf { it.transaction.amount },
        )
    }
}
