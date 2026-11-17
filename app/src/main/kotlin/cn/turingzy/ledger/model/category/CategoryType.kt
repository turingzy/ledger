package cn.turingzy.ledger.model.category

import kotlinx.serialization.Serializable

@Serializable
enum class CategoryType {
    EXPENSE,
    INCOME,
    TRANSFER,
}

