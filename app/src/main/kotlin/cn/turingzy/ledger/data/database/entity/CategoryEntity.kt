package cn.turingzy.ledger.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import cn.turingzy.ledger.model.category.CategoryType

/**
 * 交易分类数据库实体，对应表 `tb_category`。
 *
 * 存储账单收支分类定义及其绑定的展示图标。
 *
 * @property categoryId 分类唯一标识，自增主键
 * @property name 分类名称（如“餐饮”、“购物”、“工资”）
 * @property type 分类所属类型（支出/收入）[CategoryType]
 * @property iconId 关联的系统图标 ID
 * @property sortOrder 展示排序权重，值越小越靠前
 * @property isSystem 是否为系统内置分类
 * @property isDeleted 软删除标记，true 表示已被软删除
 */
@Entity(
    tableName = "tb_category",
    foreignKeys = [
        ForeignKey(
            entity = IconEntity::class,
            parentColumns = ["icon_id"],
            childColumns = ["icon_id"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("icon_id"),
    ],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "category_id")
    val categoryId: Long = 0L,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "type")
    val type: CategoryType,
    @ColumnInfo(name = "icon_id")
    val iconId: Long,
    @ColumnInfo(name = "sort_order", defaultValue = "0")
    val sortOrder: Int = 0,
    @ColumnInfo(name = "is_system", defaultValue = "0")
    val isSystem: Boolean = false,
    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Boolean = false,
)

