package cn.turingzy.ledger.model.transfer

import kotlinx.serialization.Serializable

/**
 * 账单导入解析规则数据模型。
 *
 * 对应数据库持久化存储与备份结构，包含列映射策略及默认账户/分类等配置。
 *
 * @property id 规则唯一标识，自增主键
 * @property name 规则显示名称
 * @property startRow 数据起始行号（从 1 开始计数，默认为 1）
 * @property mappingConfig 列映射与解析配置的 JSON 字符串
 * @property defaultAccountId 导入交易的默认归属账户 ID，可为空
 * @property defaultCategoryId 导入交易的默认兜底分类 ID，可为空
 * @property isSystem 是否为系统预置规则
 * @property isDeleted 软删除标记，true 表示已被删除
 */
@Serializable
data class ParseRule(
    val id: Long = 0L,
    val name: String,
    val startRow: Int = 1,
    val mappingConfig: String,
    val defaultAccountId: Long? = null,
    val defaultCategoryId: Long? = null,
    val isSystem: Boolean = false,
    val isDeleted: Boolean = false,
)

