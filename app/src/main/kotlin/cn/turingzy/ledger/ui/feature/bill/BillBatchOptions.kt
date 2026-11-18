package cn.turingzy.ledger.ui.feature.bill

import cn.turingzy.ledger.model.account.AccountWithTemplate
import cn.turingzy.ledger.model.category.CategoryWithIcon
import cn.turingzy.ledger.model.transaction.TransactionDetail
import cn.turingzy.ledger.ui.feature.bill.sheet.categoryType

internal data class BillBatchOptions(
    val accounts: Map<Long, Set<Long>>,
    val categories: Map<Long, Set<Long>>,
)

internal fun billBatchOptions(
    details: List<TransactionDetail>,
    accounts: List<AccountWithTemplate>,
    categories: List<CategoryWithIcon>,
): BillBatchOptions {
    val transactionsByCurrency = details.groupBy { it.account.currency }
    val accountMatches = accounts.filterNot { it.account.isDeleted }.associate { item ->
        val accountId = item.account.id
        accountId to transactionsByCurrency[item.account.currency].orEmpty()
            .filterNot { it.transaction.toAccountId == accountId }
            .map { it.transaction.id }.toSet()
    }.filterValues { it.isNotEmpty() }
    val transactionsByType = details.groupBy { it.transaction.type.categoryType() }
        .mapValues { (_, entries) -> entries.map { it.transaction.id }.toSet() }
    val categoryMatches = categories.filterNot { it.category.isDeleted }
        .associate { it.category.id to transactionsByType[it.category.type].orEmpty() }
        .filterValues { it.isNotEmpty() }
    return BillBatchOptions(accountMatches, categoryMatches)
}
