package cn.turingzy.ledger.data.transfer

import cn.turingzy.ledger.model.transaction.Transaction

/**
 * 账单导入规划结果。
 *
 * @property transactions 经过查重与筛选后建议导入的交易记录列表。
 * @property duplicates 确认为重复而被剔除的交易数量（例如基于外部导入唯一标识判断）。
 * @property possibleDuplicates 判定为疑似重复的交易数量（基于交易指纹匹配）。
 */
internal data class BillImportPlan(
    val transactions: List<Transaction>,
    val duplicates: Int,
    val possibleDuplicates: Int,
)

/**
 * 交易特征指纹，用于在缺少外部唯一导入键时检测内容完全重合的疑似重复项。
 */
private data class TransactionFingerprint(
    val accountId: Long, val toAccountId: Long?, val time: Long, val amount: Long,
    val type: cn.turingzy.ledger.model.transaction.TransactionType, val note: String,
)

/**
 * 提取交易的特征指纹。
 */
private fun Transaction.fingerprint() = TransactionFingerprint(accountId, toAccountId, transactionTime, amount, type, note.orEmpty().trim())

/**
 * 计算账单导入方案并执行去重检测。
 *
 * 结合外部导入键（`importKey`）和交易指纹（账户、转账目标、时间戳、金额、类型及备注）：
 * 1. 具有相同账户和 `importKey` 的交易判定为确定重复项，予以跳过。
 * 2. 匹配已有未标记导入键或重复指纹的记录判定为疑似重复项，根据 [includePossibleDuplicates] 参数决定是否保留。
 *
 * @param pending 待导入的候选交易列表。
 * @param existing 当前已存在的历史交易记录列表。
 * @param includePossibleDuplicates 当交易被判定为疑似重复时，是否仍将其纳入导入列表中。
 * @return 包含最终接收交易列表及去重统计信息的 [BillImportPlan]。
 */
internal fun planBillImport(pending: List<Transaction>, existing: List<Transaction>, includePossibleDuplicates: Boolean): BillImportPlan {
    val live = existing.filterNot { it.isDeleted }
    val knownKeys = live.mapNotNull { it.importKey?.let { key -> it.accountId to key } }.toMutableSet()
    val counts = live.groupingBy { it.fingerprint() }.eachCount().toMutableMap()
    val unknownCounts = live.filter { it.importKey == null }.groupingBy { it.fingerprint() }.eachCount().toMutableMap()
    var duplicates = 0
    var possibleDuplicates = 0
    val accepted = pending.filter { transaction ->
        val fingerprint = transaction.fingerprint()
        val count = counts[fingerprint] ?: 0
        val key = transaction.importKey?.let { transaction.accountId to it }
        when {
            key != null && !knownKeys.add(key) -> {
                duplicates++
                if (count > 0) counts[fingerprint] = count - 1
                false
            }
            count > 0 && (key == null || (unknownCounts[fingerprint] ?: 0) > 0) -> {
                // Consume matches one at a time so two identical purchases in one file remain two purchases.
                counts[fingerprint] = count - 1
                if ((unknownCounts[fingerprint] ?: 0) > 0) unknownCounts[fingerprint] = unknownCounts.getValue(fingerprint) - 1
                possibleDuplicates++
                includePossibleDuplicates
            }
            else -> true
        }
    }
    return BillImportPlan(accepted, duplicates, possibleDuplicates)
}
