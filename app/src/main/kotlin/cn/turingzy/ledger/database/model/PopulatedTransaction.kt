package cn.turingzy.ledger.database.model

import androidx.room.Embedded
import androidx.room.Relation

data class PopulatedTransaction(
    @Embedded val transaction: TransactionEntity,
    @Relation(
        parentColumn = "account_id",
        entityColumn = "account_id",
    )
    val account: AccountEntity,
    @Relation(
        parentColumn = "to_account_id",
        entityColumn = "account_id",
    )
    val toAccount: AccountEntity?,
    @Relation(
        parentColumn = "category_id",
        entityColumn = "category_id",
        entity = CategoryEntity::class,
    )
    val categoryWithIcon: PopulatedCategory,
)

