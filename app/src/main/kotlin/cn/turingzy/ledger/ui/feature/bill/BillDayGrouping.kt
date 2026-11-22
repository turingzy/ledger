package cn.turingzy.ledger.ui.feature.bill

import cn.turingzy.ledger.model.currency.primaryAmount
import cn.turingzy.ledger.model.transaction.TransactionDetail
import cn.turingzy.ledger.model.transaction.TransactionType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 将交易记录列表按天进行分组，并计算每日在主币种及各币种下的收支汇总。
 *
 * @param transactions 待分组的交易详情列表。
 * @param zone 时区信息，用于转换交易时间戳为本地日期。
 * @param primary 主货币代码，用于折算每日汇总收支。
 * @param newestFirst 是否按时间倒序排列（日期及组内交易），默认为 true（最新日期排在前面）。
 * @return 按日期分组后的 [DayTransactionGroup] 列表。
 */
internal fun groupTransactionsByDay(
    transactions: List<TransactionDetail>,
    zone: ZoneId,
    primary: String,
    newestFirst: Boolean = true,
): List<DayTransactionGroup> {
    val dateFormatter = DateTimeFormatter.ofPattern("MM/dd E", Locale.CHINESE)

    return transactions
        .groupBy { tx ->
            Instant.ofEpochMilli(tx.transaction.transactionTime).atZone(zone).toLocalDate()
        }
        .map { (date, txList) ->
            val dayExpense = txList
                .filter { it.transaction.type == TransactionType.EXPENSE }
                .sumOf { it.primaryAmount(primary) ?: 0L }
            val dayIncome = txList
                .filter { it.transaction.type == TransactionType.INCOME }
                .sumOf { it.primaryAmount(primary) ?: 0L }

            DayTransactionGroup(
                dateEpochDay = date.toEpochDay(),
                displayDate = date.format(dateFormatter),
                totalExpense = dayExpense,
                totalIncome = dayIncome,
                transactions = if (newestFirst) txList.sortedByDescending { it.transaction.transactionTime }
                    else txList.sortedBy { it.transaction.transactionTime },
                currencyAmounts = txList.groupBy { it.account.currency }.map { (code, entries) ->
                    DayCurrencyAmounts(code,
                        entries.filter { it.transaction.type == TransactionType.EXPENSE }.sumOf { it.transaction.amount },
                        entries.filter { it.transaction.type == TransactionType.INCOME }.sumOf { it.transaction.amount })
                },
            )
        }
        .let { groups -> if (newestFirst) groups.sortedByDescending { it.dateEpochDay } else groups.sortedBy { it.dateEpochDay } }
}
