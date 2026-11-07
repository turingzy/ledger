package cn.turingzy.ledger.model

import kotlinx.serialization.Serializable

@Serializable
enum class AccountType {
    DEBIT,
    CREDIT,
    WALLET,
    CASH,
}

