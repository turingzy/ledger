package cn.turingzy.ledger.ui.feature.wallet.activity

import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.transaction.DateFilterType
import cn.turingzy.ledger.model.transaction.Transaction
import cn.turingzy.ledger.model.transaction.TransactionDetail
import cn.turingzy.ledger.model.transaction.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

internal data class AccountBalancePoint(val date: LocalDate, val balance: Long?)

/** 所选期间的收支、余额点和按日期分组的账户交易。 */
internal data class AccountPeriodActivity(
    val expense: Long,
    val income: Long,
    val balances: List<AccountBalancePoint>,
    val groupedTransactions: Map<LocalDate, List<TransactionDetail>>,
)

/** 从当前余额逐笔倒推每个日结或月结余额，转入按实际到账金额回推。 */
internal fun accountPeriodActivity(
    account: Account,
    history: List<TransactionDetail>,
    type: DateFilterType,
    date: LocalDate,
    today: LocalDate,
    zone: ZoneId,
): AccountPeriodActivity {
    val month = YearMonth.from(date)
    val yearly = type == DateFilterType.YEAR
    val first = if (yearly) date.withDayOfYear(1) else month.atDay(1)
    val periodEnds = if (yearly) List(12) { YearMonth.from(first).plusMonths(it.toLong()).atEndOfMonth() }
        else List(month.lengthOfMonth()) { month.atDay(it + 1) }
    val dated = history.asSequence()
        .filter { !it.transaction.isDeleted }
        .sortedWith(compareByDescending<TransactionDetail> { it.transaction.transactionTime }.thenByDescending { it.transaction.id })
        .map { Instant.ofEpochMilli(it.transaction.transactionTime).atZone(zone).toLocalDate() to it }
        .toList()
    // 本月的月结点在今天取值，未来的日或月保持空白。
    val effectiveEnds = periodEnds.map { end ->
        if (yearly && YearMonth.from(end) == YearMonth.from(today)) minOf(end, today) else end
    }
    var balance = account.currentBalance
    var row = 0
    val balances = periodEnds.indices.reversed().map { index ->
        val effectiveEnd = effectiveEnds[index]
        while (effectiveEnd <= today && row < dated.size && dated[row].first > effectiveEnd) {
            balance -= dated[row++].second.transaction.accountDelta(account.id)
        }
        AccountBalancePoint(periodEnds[index], balance.takeIf { effectiveEnd <= today })
    }.asReversed()
    val shown = dated.filter { (localDate) -> localDate in first..periodEnds.last() }
    val expense = shown.sumOf { (_, detail) -> detail.transaction.amount.takeIf { detail.transaction.type == TransactionType.EXPENSE } ?: 0L }
    val income = shown.sumOf { (_, detail) -> detail.transaction.amount.takeIf { detail.transaction.type == TransactionType.INCOME } ?: 0L }
    return AccountPeriodActivity(expense, income, balances, shown.groupBy({ it.first }, { it.second }))
}

/** 表示单笔交易在指定账户中的原币增减。 */
internal fun Transaction.accountDelta(accountId: Long): Long = when {
    type == TransactionType.TRANSFER && toAccountId == accountId -> toAmount ?: amount
    this.accountId != accountId -> 0L
    type == TransactionType.INCOME -> amount
    else -> -amount
}
