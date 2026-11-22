package cn.turingzy.ledger.data.database.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * 聚合账户数据模型。
 *
 * 结合 [AccountEntity] 及其关联的 [CardTemplateEntity] 卡片样式模板。
 *
 * @property account 账户基础数据实体
 * @property template 账户关联的卡片样式模板，若未设置模板则为 null
 */
data class PopulatedAccount(
    @Embedded val account: AccountEntity,
    @Relation(
        parentColumn = "template_id",
        entityColumn = "template_id",
    )
    val template: CardTemplateEntity?,
)

