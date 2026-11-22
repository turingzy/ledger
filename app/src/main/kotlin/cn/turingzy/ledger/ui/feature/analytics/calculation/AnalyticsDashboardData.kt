package cn.turingzy.ledger.ui.feature.analytics.calculation

import cn.turingzy.ledger.ui.feature.analytics.AnalyticsTransaction
import cn.turingzy.ledger.model.transaction.TransactionDetail
import cn.turingzy.ledger.model.transaction.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.Instant

/**
 * 每日汇总收支金额模型。
 *
 * @property expense 当日总支出（分）。
 * @property income 当日总收入（分）。
 */
data class AnalyticsDay(val expense: Long = 0L, val income: Long = 0L)

/**
 * 月度汇总收支模型。
 *
 * @property month 所属年月。
 * @property expense 该月总支出（分）。
 * @property income 该月总收入（分）。
 */
internal data class AnalyticsMonth(val month: YearMonth, val expense: Long, val income: Long) {
    /** 月度结余（收入 - 支出）。 */
    val balance: Long get() = income - expense
}

/**
 * 将交易列表按日期聚合为每日收支明细映射。
 *
 * @param entries 交易明细列表。
 * @param zone 时区。
 * @return 日期到 [AnalyticsDay] 的映射。
 */
internal fun analyticsDays(entries: List<TransactionDetail>, zone: ZoneId): Map<LocalDate, AnalyticsDay> =
    entries.filter { !it.transaction.isDeleted && it.transaction.type != TransactionType.TRANSFER }
        .groupBy { Instant.ofEpochMilli(it.transaction.transactionTime).atZone(zone).toLocalDate() }
        .mapValues { (_, daily) -> AnalyticsDay(
            daily.filter { it.transaction.type == TransactionType.EXPENSE }.sumOf { it.transaction.baseAmount ?: it.transaction.amount },
            daily.filter { it.transaction.type == TransactionType.INCOME }.sumOf { it.transaction.baseAmount ?: it.transaction.amount },
        ) }

/**
 * 根据每日统计数据汇总生成连续月份的月度收支数据列表（至少包含 6 个月）。
 *
 * @param days 按日映射的统计数据。
 * @param end 截止的目标年月。
 * @return 按月份先后排列的 [AnalyticsMonth] 列表。
 */
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

/**
 * 计算指定月份内每一天的累计支出金额列表。
 *
 * @param days 按日映射的统计数据。
 * @param month 目标月份。
 * @return 长度为当月天数的列表，第 i 项代表从 1 号累计到第 (i + 1) 号的支出总额（分）。
 */
internal fun cumulativeExpenses(days: Map<LocalDate, AnalyticsDay>, month: YearMonth): List<Long> {
    var total = 0L
    return (1..month.lengthOfMonth()).map { day -> total += days[month.atDay(day)]?.expense ?: 0L; total }
}

/**
 * 基于已过去的实际天数与支出，按线性推算预估到目标天数的支出总额。
 *
 * @param amount 已累计支出（分）。
 * @param elapsedDays 已过去天数。
 * @param targetDay 目标天数（如月末天数）。
 * @return 预估的支出总额（分）。
 */
internal fun projectExpense(amount: Long, elapsedDays: Int, targetDay: Int): Long =
    if (elapsedDays <= 0) 0L else BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(targetDay.toLong()))
        .divide(BigDecimal.valueOf(elapsedDays.toLong()), 0, RoundingMode.HALF_UP)
        .min(BigDecimal.valueOf(Long.MAX_VALUE)).longValueExact()

/**
 * 将交易详情列表过滤并转换为按时间倒序排列的 [AnalyticsTransaction] 列表。
 *
 * @param entries 交易明细列表。
 * @param zone 时区。
 * @return 统计分析展示用的交易列表。
 */
internal fun analyticsTransactions(entries: List<TransactionDetail>, zone: ZoneId): List<AnalyticsTransaction> =
    entries.filter { !it.transaction.isDeleted && it.transaction.type != TransactionType.TRANSFER }
        .sortedByDescending { it.transaction.transactionTime }
        .map { analyticsTransaction(it, zone) }

/**
 * 将单个 [TransactionDetail] 转换为统计展示用的 [AnalyticsTransaction]。
 *
 * @param detail 交易明细。
 * @param zone 时区。
 * @return 转换后的 [AnalyticsTransaction]。
 */
internal fun analyticsTransaction(detail: TransactionDetail, zone: ZoneId): AnalyticsTransaction {
    val time = Instant.ofEpochMilli(detail.transaction.transactionTime).atZone(zone)
    return AnalyticsTransaction(
        id = detail.transaction.id,
        date = time.toLocalDate(),
        note = detail.transaction.note.orEmpty(),
        amount = detail.transaction.amount,
        primaryAmount = detail.transaction.baseAmount ?: detail.transaction.amount,
        currency = detail.account.currency,
        type = detail.transaction.type,
        category = detail.category,
        time = time.toLocalTime(),
        detail = detail,
    )
}
