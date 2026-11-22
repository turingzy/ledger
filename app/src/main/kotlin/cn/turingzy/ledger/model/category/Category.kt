package cn.turingzy.ledger.model.category

import kotlinx.serialization.Serializable

/**
 * 交易分类业务模型。
 *
 * 用于对收支与转账交易进行归类整理。
 *
 * @property id 分类唯一标识，自增主键
 * @property name 分类名称（如“餐饮”、“薪资”）
 * @property type 分类所属类型，详见 [CategoryType]
 * @property iconId 关联的图标 ID，对应 [Icon.id]
 * @property sortOrder 显示排序顺序，数值越小越靠前
 * @property isSystem 是否为系统内置分类（不可直接物理删除）
 * @property isDeleted 软删除标记，true 表示已被删除
 */
@Serializable
data class Category(
    val id: Long = 0L,
    val name: String,
    val type: CategoryType,
    val iconId: Long,
    val sortOrder: Int = 0,
    val isSystem: Boolean = false,
    val isDeleted: Boolean = false,
)

/**
 * 包含图标详情的分类数据模型。
 *
 * 聚合了分类基础信息及其对应的图标对象，便于 UI 层直接渲染图标与文案。
 *
 * @property category 分类对象
 * @property icon 关联的图标对象
 */
@Serializable
data class CategoryWithIcon(
    val category: Category,
    val icon: Icon,
)

