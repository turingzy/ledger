package cn.turingzy.ledger.model.transaction

import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.category.CategoryWithIcon
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
    val importKey: String? = null,
    val baseAmount: Long? = null,
    val baseCurrency: String? = null,
    val baseRate: String? = null,
    val baseRateDate: String? = null,
    val toAmount: Long? = null,
    val transferRate: String? = null,
    val currencySnapshots: List<CurrencySnapshot> = emptyList(),
)

@Serializable
data class TransactionDetail(
    val transaction: Transaction,
    val account: Account,
    val toAccount: Account? = null,
    val category: CategoryWithIcon,
)


@Serializable
data class CurrencySnapshot(val currency: String, val amount: Long, val rate: String, val date: String)
