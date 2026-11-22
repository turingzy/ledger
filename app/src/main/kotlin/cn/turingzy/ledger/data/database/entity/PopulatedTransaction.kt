package cn.turingzy.ledger.data.database.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * 聚合交易记录数据模型。
 *
 * 结合 [TransactionEntity] 交易实体及其关联的支付账户、转账入账账户和带图标的分类。
 *
 * @property transaction 交易基础数据实体
 * @property account 发生交易的源账户实体
 * @property toAccount 转账交易的目标入账账户实体，非转账或无目标账户时为 null
 * @property categoryWithIcon 交易关联的分类及其图标聚合实体 [PopulatedCategory]
 */
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

