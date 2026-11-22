package cn.turingzy.ledger.model.account

import kotlinx.serialization.Serializable

/**
 * 卡片外观模板数据模型。
 *
 * 定义账户在钱包视图中展示时的卡片视觉样式，包括背景封面与文本颜色。
 *
 * @property id 模板唯一标识，自增主键（内置模板使用负数 ID）
 * @property name 模板名称
 * @property coverUri 卡片封面资源的 URI（可以是 "res://" 格式内置资源或本地文件路径）
 * @property textColor 卡片上文字与前景图标的十六进制颜色值（如 "#FFFFFF"）
 * @property isSystem 是否为系统内置模板
 * @property isDeleted 软删除标记，true 表示已被删除
 */
@Serializable
data class CardTemplate(
    val id: Long = 0L,
    val name: String,
    val coverUri: String,
    val textColor: String = "#FFFFFF",
    val isSystem: Boolean = false,
    val isDeleted: Boolean = false,
)

