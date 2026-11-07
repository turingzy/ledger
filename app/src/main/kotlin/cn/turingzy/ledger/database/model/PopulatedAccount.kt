package cn.turingzy.ledger.database.model

import androidx.room.Embedded
import androidx.room.Relation

data class PopulatedAccount(
    @Embedded val account: AccountEntity,
    @Relation(
        parentColumn = "template_id",
        entityColumn = "template_id",
    )
    val template: CardTemplateEntity?,
)

