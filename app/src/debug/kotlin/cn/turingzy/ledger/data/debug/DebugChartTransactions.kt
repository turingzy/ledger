package cn.turingzy.ledger.data.debug

import cn.turingzy.ledger.model.Transaction
import cn.turingzy.ledger.model.TransactionType
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

internal fun debugChartTransactions(
    now: Instant,
    zone: ZoneId,
    accountIds: List<Long>,
    expenseCategoryIds: List<Long>,
    incomeCategoryIds: List<Long>,
): List<Transaction> {
    require(accountIds.isNotEmpty() && expenseCategoryIds.isNotEmpty() && incomeCategoryIds.isNotEmpty())
    val today = now.atZone(zone).toLocalDate()
    val expenseAmounts = listOf(1200L, 2850L, 3990L, 6800L, 15800L, 42800L, 99900L, 168000L)
    val incomeAmounts = listOf(850000L, 320000L, 68000L, 128800L)
    return List(100) { index ->
        val income = index % 5 == 0
        val date = when {
            index < 28 -> today.minusDays((index % 7).toLong())
            index < 60 -> today.withDayOfMonth(1).plusDays(((index * 7) % today.dayOfMonth).toLong())
            else -> {
                val month = YearMonth.from(today).minusMonths(1L + (index - 60) % 18)
                month.atDay(1 + (index * 7 % month.lengthOfMonth()))
            }
        }
        val time = date.atTime(8 + index % 12, index * 7 % 60).atZone(zone).toInstant()
        val categories = if (income) incomeCategoryIds else expenseCategoryIds
        Transaction(
            type = if (income) TransactionType.INCOME else TransactionType.EXPENSE,
            amount = if (income) incomeAmounts[index / 5 % incomeAmounts.size] else expenseAmounts[index % expenseAmounts.size],
            accountId = accountIds[index % accountIds.size],
            categoryId = categories[if (income) index / 5 % categories.size else index % categories.size],
            transactionTime = minOf(time.toEpochMilli(), now.toEpochMilli() - 60_000L),
            note = "[图表测试] ${index + 1}/100",
        )
    }
}
