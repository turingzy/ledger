package cn.turingzy.ledger.model.account

import kotlinx.serialization.Serializable

@Serializable
data class CardTemplate(
    val id: Long = 0L,
    val name: String,
    val coverUri: String,
    val textColor: String = "#FFFFFF",
    val isSystem: Boolean = false,
    val isDeleted: Boolean = false,
)

