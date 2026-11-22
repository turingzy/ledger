package cn.turingzy.ledger.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 系统图标数据库实体，对应表 `tb_icon`。
 *
 * 存储 Material Symbols 或自定义图标资源映射信息。
 *
 * @property iconId 图标唯一标识，自增主键
 * @property keyName 唯一图标标识键（如 "restaurant", "shopping_cart"）
 * @property displayName 图标展示名称（如“餐饮”、“购物”）
 * @property iconUri 图标资源定位 URI 或符号名称
 * @property isSystem 是否为系统内置图标
 * @property sortOrder 图标选择器中的展示排序权重
 * @property isDeleted 软删除标记，true 表示已被软删除
 */
@Entity(
    tableName = "tb_icon",
    indices = [
        Index(value = ["key_name"], unique = true),
    ],
)
data class IconEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "icon_id")
    val iconId: Long = 0L,
    @ColumnInfo(name = "key_name")
    val keyName: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "icon_uri")
    val iconUri: String,
    @ColumnInfo(name = "is_system", defaultValue = "0")
    val isSystem: Boolean = false,
    @ColumnInfo(name = "sort_order", defaultValue = "0")
    val sortOrder: Int = 0,
    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Boolean = false,
)

