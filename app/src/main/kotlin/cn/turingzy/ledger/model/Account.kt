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
    val sortOrder: Int = 0,
    val isDeleted: Boolean = false,
)

@Serializable
data class AccountWithTemplate(
    val account: Account,
    val template: CardTemplate? = null,
)

