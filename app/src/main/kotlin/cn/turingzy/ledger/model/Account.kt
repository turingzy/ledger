package cn.turingzy.ledger.model

import kotlinx.serialization.Serializable

@Serializable
data class Account(
    val id: Long = 0L,
    val name: String,
    val type: AccountType,
    val initialBalance: Long = 0L,
    val currentBalance: Long = 0L,
    val currency: String = "CNY",
    val templateId: Long? = null,
    val cardNumber: String? = null,
    val includeInTotal: Boolean = true,
    val isDeleted: Boolean = false,
    val baseBalance: Long? = null,
    val baseCurrency: String? = null,
    val baseRate: String? = null,
    val baseRateDate: String? = null,
)

@Serializable
data class AccountWithTemplate(
    val account: Account,
    val template: CardTemplate? = null,
)

