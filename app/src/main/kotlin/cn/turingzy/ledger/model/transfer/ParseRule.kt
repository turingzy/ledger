package cn.turingzy.ledger.model.transfer

import kotlinx.serialization.Serializable

@Serializable
data class ParseRule(
    val id: Long = 0L,
    val name: String,
    val startRow: Int = 1,
    val mappingConfig: String,
    val defaultAccountId: Long? = null,
    val defaultCategoryId: Long? = null,
    val isSystem: Boolean = false,
    val isDeleted: Boolean = false,
)

