package cn.turingzy.ledger.ui.feature.bill

import cn.turingzy.ledger.model.currency.primaryAmount
import cn.turingzy.ledger.model.transaction.TransactionDetail
import cn.turingzy.ledger.model.transaction.TransactionType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Builds bill date groups from the supplied rows and totals only those rows. */
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
