package cn.turingzy.ledger.model.backup

import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.CardTemplate
import cn.turingzy.ledger.model.category.Category
import cn.turingzy.ledger.model.category.Icon
import cn.turingzy.ledger.model.settings.AppPreferences
import cn.turingzy.ledger.model.settings.SummaryCardTarget
import cn.turingzy.ledger.model.transaction.Transaction
import cn.turingzy.ledger.model.transfer.ParseRule
import kotlinx.serialization.Serializable

/**
 * 账本备份文件数据根模型。
 *
 * 封装账本完整或部分的导出数据，包含数据格式版本、账单数据与系统设置快照。
 *
 * @property format 备份文件格式标识符（默认为 "cn.turingzy.ledger.backup"）
 * @property version 备份结构版本号，用于版本迁移与兼容性校验
 * @property createdAt 备份创建时间戳（毫秒）
 * @property bills 账单数据快照，若仅备份设置则为 null
 * @property settings 应用设置与元数据快照，若仅备份账单则为 null
 */
@Serializable
data class LedgerBackup(
    val format: String = "cn.turingzy.ledger.backup",
    val version: Int = 1,
    val createdAt: Long = 0L,
    val bills: LedgerBillSnapshot? = null,
    val settings: LedgerSettingsSnapshot? = null,
)

/**
 * 账单核心业务数据快照。
 *
 * 包含账户、交易明细、分类、图标及卡片模板等全套业务数据。
 *
 * @property accounts 账户列表
 * @property transactions 交易记录列表
 * @property categories 分类列表
 * @property icons 图标列表
 * @property templates 卡片模板列表
 */
@Serializable
data class LedgerBillSnapshot(
    val accounts: List<Account>,
    val transactions: List<Transaction>,
    val categories: List<Category>,
    val icons: List<Icon>,
    val templates: List<CardTemplate>,
)

/**
 * 应用设置与辅助数据快照。
 *
 * 包含账单导入解析规则、卡片模板、外观配置、偏好设置以及汇率缓存。
 *
 * @property rules 导入解析规则列表
 * @property templates 卡片模板列表
 * @property appearances 卡片外观个性化配置列表
 * @property preferences 用户偏好设置，详见 [AppPreferences]
 * @property rates 历史汇率缓存列表
 */
@Serializable
data class LedgerSettingsSnapshot(
    val rules: List<ParseRule>,
    val templates: List<CardTemplate>,
    val appearances: List<BackupAppearance> = emptyList(),
    val preferences: AppPreferences? = null,
    val rates: List<BackupExchangeRate> = emptyList(),
)

/**
 * 卡片外观配置备份模型。
 *
 * @property target 卡片应用目标位置，详见 [SummaryCardTarget]
 * @property transparency 透明度百分比（0..100）
 * @property image 自定义背景图片文件名或 URI，可为空
 */
@Serializable
data class BackupAppearance(val target: SummaryCardTarget, val transparency: Int, val image: String? = null)

/**
 * 汇率记录备份模型。
 *
 * @property date 汇率生效日期（格式：yyyy-MM-dd）
 * @property currency 货币代码（ISO 4217）
 * @property perUsd 每 1 美元折算该货币的汇率数值字符串
 * @property manual 是否为用户手动录入调整的汇率
 * @property updatedAtMillis 本地记录或拉取的时间戳（毫秒）
 */
@Serializable
data class BackupExchangeRate(
    val date: String,
    val currency: String,
    val perUsd: String,
    val manual: Boolean = false,
    val updatedAtMillis: Long? = null,
)
