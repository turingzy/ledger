package cn.turingzy.ledger.model

import kotlinx.serialization.Serializable

@Serializable
data class Transaction(
    val id: Long = 0L,
    val type: TransactionType,
    val amount: Long,
    val accountId: Long,
    val toAccountId: Long? = null,
    val categoryId: Long,
    val transactionTime: Long,
    val note: String? = null,
    val isDeleted: Boolean = false,
)

@Serializable
data class TransactionDetail(
    val transaction: Transaction,
    val account: Account,
    val toAccount: Account? = null,
    val category: CategoryWithIcon,
)

