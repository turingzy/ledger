package cn.turingzy.ledger.model.category

import kotlinx.serialization.Serializable

/**
 * 图标数据模型。
 *
 * 用于分类、账户等业务实体的图标展示与选择。
 *
 * @property id 图标唯一标识，自增主键
 * @property keyName 图标标识键名（如 Material Symbols 名称或资源 key）
 * @property displayName 图标展示名称（如“餐饮”、“交通”）
 * @property iconUri 图标资源 URI（支持内置矢量符号引用或自定义图片路径）
 * @property isSystem 是否为系统预置图标
 * @property sortOrder 显示排序顺序，数值越小越靠前
 * @property isDeleted 软删除标记，true 表示已被删除
 */
@Serializable
data class Icon(
    val id: Long = 0L,
    val keyName: String,
    val displayName: String,
    val iconUri: String,
    val isSystem: Boolean = false,
    val sortOrder: Int = 0,
    val isDeleted: Boolean = false,
)

