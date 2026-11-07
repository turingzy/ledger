package cn.turingzy.ledger.database.model

import androidx.room.Embedded
import androidx.room.Relation

data class PopulatedCategory(
    @Embedded val category: CategoryEntity,
    @Relation(
        parentColumn = "icon_id",
        entityColumn = "icon_id",
    )
    val icon: IconEntity,
)

