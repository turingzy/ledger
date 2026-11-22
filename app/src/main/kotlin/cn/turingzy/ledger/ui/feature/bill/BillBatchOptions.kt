package cn.turingzy.ledger.ui.feature.bill

import cn.turingzy.ledger.model.account.AccountWithTemplate
import cn.turingzy.ledger.model.category.CategoryWithIcon
import cn.turingzy.ledger.model.transaction.TransactionDetail
import cn.turingzy.ledger.ui.feature.bill.sheet.categoryType

/**
 * 账单批量操作的可选目标配置。
 *
 * @property accounts 可迁移的目标账户 ID 到匹配交易 ID 集合的映射（仅币种匹配且非转账目标的交易有效）。
 * @property categories 可迁移的目标分类 ID 到匹配交易 ID 集合的映射（仅交易类型与分类类型匹配的交易有效）。
 */
internal data class BillBatchOptions(
    val accounts: Map<Long, Set<Long>>,
    val categories: Map<Long, Set<Long>>,
)

/**
 * 根据选中的交易详情列表及可用的账户、分类列表，计算出支持批量迁移的账户与分类候选映射。
 *
 * @param details 当前选中的交易详情列表。
 * @param accounts 当前系统中未删除的账户模板列表。
 * @param categories 当前系统中未删除的分类列表。
 * @return 包含合法迁移目标的 [BillBatchOptions]。
 */
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
