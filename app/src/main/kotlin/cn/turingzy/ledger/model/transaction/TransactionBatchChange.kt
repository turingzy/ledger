package cn.turingzy.ledger.model.transaction

sealed interface TransactionBatchChange {
    data class Account(val accountId: Long) : TransactionBatchChange
    data class Category(val categoryId: Long) : TransactionBatchChange
    data object Delete : TransactionBatchChange
}
