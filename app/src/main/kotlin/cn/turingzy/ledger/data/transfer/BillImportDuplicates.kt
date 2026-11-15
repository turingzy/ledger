package cn.turingzy.ledger.data.transfer

import cn.turingzy.ledger.model.Transaction

internal data class BillImportPlan(
    val transactions: List<Transaction>,
    val duplicates: Int,
    val possibleDuplicates: Int,
)

private data class TransactionFingerprint(
    val accountId: Long, val toAccountId: Long?, val time: Long, val amount: Long,
    val type: cn.turingzy.ledger.model.TransactionType, val note: String,
)

private fun Transaction.fingerprint() = TransactionFingerprint(accountId, toAccountId, transactionTime, amount, type, note.orEmpty().trim())

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
