package cn.turingzy.ledger.model.category

import kotlinx.serialization.Serializable

@Serializable
data class Icon(
    val id: Long = 0L,
    val keyName: String,
    val displayName: String,
    val iconUri: String,
    val isSystem: Boolean = false,
    val sortOrder: Int = 0,
    val isDeleted: Boolean = false,
)

