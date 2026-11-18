package cn.turingzy.ledger.ui.feature.bill.sheet

import cn.turingzy.ledger.model.account.AccountWithTemplate
import cn.turingzy.ledger.model.category.CategoryType
import cn.turingzy.ledger.model.category.CategoryWithIcon
import cn.turingzy.ledger.model.transaction.Transaction
import cn.turingzy.ledger.model.transaction.TransactionType
import cn.turingzy.ledger.util.AmountFormatter

internal data class RecordFormInput(
    val type: TransactionType,
    val expression: String,
    val accountId: Long,
    val toAccountId: Long?,
    val categoryId: Long,
    val transactionTime: Long,
    val note: String,
    val transferRate: String? = null,
) {
    fun toTransactionOrNull(accounts: List<AccountWithTemplate>, categories: List<CategoryWithIcon>): Transaction? {
        val amount = AmountFormatter.parseToCents(evaluateMathExpression(expression))?.takeIf { it > 0L } ?: return null
        val account = accounts.firstOrNull { it.account.id == accountId && !it.account.isDeleted }?.account ?: return null
        if (categories.none { it.category.id == categoryId && it.category.type == type.categoryType() && !it.category.isDeleted }) return null
        val destination = if (type == TransactionType.TRANSFER) {
            toAccountId?.takeIf { id -> id != accountId && accounts.any { it.account.id == id && !it.account.isDeleted } } ?: return null
        } else null
        val target = accounts.firstOrNull { it.account.id == destination }?.account
        val converted = if (target == null) null else if (target.currency == account.currency) amount else {
            val rate = transferRate ?: return null
            try { cn.turingzy.ledger.model.currency.convertCents(amount, rate).takeIf { it > 0L } ?: return null }
            catch (_: NumberFormatException) { return null }
            catch (_: ArithmeticException) { return null }
            catch (_: IllegalArgumentException) { return null }
        }
        return Transaction(
            type = type,
            amount = amount,
            accountId = accountId,
            toAccountId = destination,
            categoryId = categoryId,
            transactionTime = transactionTime,
            note = note.trim().takeIf { it.isNotEmpty() },
            toAmount = converted,
            transferRate = transferRate,
        )
    }
}

internal fun TransactionType.categoryType(): CategoryType = when (this) {
    TransactionType.EXPENSE -> CategoryType.EXPENSE
    TransactionType.INCOME -> CategoryType.INCOME
    TransactionType.TRANSFER -> CategoryType.TRANSFER
}

internal fun selectableRecordCategories(categories: List<CategoryWithIcon>, type: TransactionType): List<CategoryWithIcon> =
    categories.filter {
        it.category.type == type.categoryType() && !it.category.isDeleted &&
            (type == TransactionType.TRANSFER || it.category.name !in OtherRecordCategoryNames)
    }.sortedBy { it.category.sortOrder }

private val OtherRecordCategoryNames = setOf("其他", "其他分类", "其他支出", "其他收入")
