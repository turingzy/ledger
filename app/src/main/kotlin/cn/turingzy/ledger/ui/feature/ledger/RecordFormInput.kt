package cn.turingzy.ledger.ui.feature.ledger

import cn.turingzy.ledger.model.AccountWithTemplate
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.model.CategoryWithIcon
import cn.turingzy.ledger.model.Transaction
import cn.turingzy.ledger.model.TransactionType
import cn.turingzy.ledger.util.AmountFormatter
import java.time.LocalDate
import java.time.ZoneId

internal data class RecordFormInput(
    val type: TransactionType,
    val expression: String,
    val accountId: Long,
    val toAccountId: Long?,
    val categoryId: Long,
    val transactionTime: Long,
    val note: String,
) {
    fun toTransactionOrNull(accounts: List<AccountWithTemplate>, categories: List<CategoryWithIcon>): Transaction? {
        val amount = AmountFormatter.parseToCents(evaluateMathExpression(expression))?.takeIf { it > 0L } ?: return null
        if (accounts.none { it.account.id == accountId && !it.account.isDeleted }) return null
        if (categories.none { it.category.id == categoryId && it.category.type == type.categoryType() && !it.category.isDeleted }) return null
        val destination = if (type == TransactionType.TRANSFER) {
            toAccountId?.takeIf { id -> id != accountId && accounts.any { it.account.id == id && !it.account.isDeleted } } ?: return null
        } else null
        return Transaction(
            type = type,
            amount = amount,
            accountId = accountId,
            toAccountId = destination,
            categoryId = categoryId,
            transactionTime = transactionTime,
            note = note.trim().takeIf { it.isNotEmpty() },
        )
    }
}

internal fun TransactionType.categoryType(): CategoryType = when (this) {
    TransactionType.EXPENSE -> CategoryType.EXPENSE
    TransactionType.INCOME -> CategoryType.INCOME
    TransactionType.TRANSFER -> CategoryType.TRANSFER
}

internal fun recordTimeMillis(date: LocalDate, hour: String, minute: String, zone: ZoneId): Long? {
    val validHour = hour.toIntOrNull()?.takeIf { it in 0..23 } ?: return null
    val validMinute = minute.toIntOrNull()?.takeIf { it in 0..59 } ?: return null
    return date.atTime(validHour, validMinute).atZone(zone).toInstant().toEpochMilli()
}
