package cn.turingzy.ledger.model.account

import kotlinx.serialization.Serializable

@Serializable
enum class AccountType {
    DEBIT,
    CREDIT,
    WALLET,
    CASH,
}

