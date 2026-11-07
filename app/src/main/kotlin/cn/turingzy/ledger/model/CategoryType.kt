package cn.turingzy.ledger.model

import kotlinx.serialization.Serializable

@Serializable
enum class CategoryType {
    EXPENSE,
    INCOME,
}

