package cn.turingzy.ledger.data.database.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * 聚合分类数据模型。
 *
 * 结合 [CategoryEntity] 及其一对一关联绑定的 [IconEntity] 图标实体。
 *
 * @property category 分类基础数据实体
 * @property icon 分类关联的图标实体
 */
data class PopulatedCategory(
    @Embedded val category: CategoryEntity,
    @Relation(
        parentColumn = "icon_id",
        entityColumn = "icon_id",
    )
    val icon: IconEntity,
)

