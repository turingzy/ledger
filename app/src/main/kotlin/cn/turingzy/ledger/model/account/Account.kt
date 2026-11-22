package cn.turingzy.ledger.model.account

import kotlinx.serialization.Serializable

/**
 * 账户业务模型。
 *
 * 表示用户管理的一个资产或负债账户（如银行卡、信用卡、电子钱包或现金）。
 *
 * @property id 账户唯一标识，自增主键
 * @property name 账户名称
 * @property type 账户类型，详见 [AccountType]
 * @property initialBalance 初始余额（单位：分）
 * @property currentBalance 当前余额（单位：分）
 * @property currency 账户币种代码，符合 ISO 4217 标准（默认为 "CNY"）
 * @property templateId 关联的卡片外观模板 ID，为 null 时使用默认外观
 * @property cardNumber 卡号或自定义编号（通常为后四位），可为空
 * @property includeInTotal 是否计入总资产统计
 * @property isDeleted 软删除标记，true 表示该账户已被删除
 * @property baseBalance 折算为主币种后的余额（单位：分），在查询视图或多币种估值时填充
 * @property baseCurrency 主币种代码，与 [baseBalance] 对应
 * @property baseRate 折算为主币种所使用的汇率快照
 * @property baseRateDate 汇率快照生效日期（格式：yyyy-MM-dd）
 */
@Serializable
data class Account(
    val id: Long = 0L,
    val name: String,
    val type: AccountType,
    val initialBalance: Long = 0L,
    val currentBalance: Long = 0L,
    val currency: String = "CNY",
    val templateId: Long? = null,
    val cardNumber: String? = null,
    val includeInTotal: Boolean = true,
    val isDeleted: Boolean = false,
    val baseBalance: Long? = null,
    val baseCurrency: String? = null,
    val baseRate: String? = null,
    val baseRateDate: String? = null,
)

/**
 * 包含关联卡片模板信息的账户数据模型。
 *
 * 常用于钱包卡片展示等需要同时获取账户数据与外观模板的场景。
 *
 * @property account 账户信息
 * @property template 关联的卡片外观模板，为 null 时表示未设置或使用默认样式
 */
@Serializable
data class AccountWithTemplate(
    val account: Account,
    val template: CardTemplate? = null,
)

