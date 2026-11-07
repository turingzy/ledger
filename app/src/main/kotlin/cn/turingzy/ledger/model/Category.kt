package cn.turingzy.ledger.model

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: Long = 0L,
    val name: String,
    val type: CategoryType,
    val iconId: Long,
    val sortOrder: Int = 0,
    val isSystem: Boolean = false,
    val isDeleted: Boolean = false,
)

@Serializable
data class CategoryWithIcon(
    val category: Category,
    val icon: Icon,
)

