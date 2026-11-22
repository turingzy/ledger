package cn.turingzy.ledger.model.account

import kotlinx.serialization.Serializable

/**
 * 账户类型枚举。
 *
 * 定义账本支持的资产与负债账户分类。
 */
@Serializable
enum class AccountType {
    /**
     * 借记卡 / 储蓄卡账户。
     */
    DEBIT,

    /**
     * 信用卡账户（具有信用额度和负债属性）。
     */
    CREDIT,

    /**
     * 电子钱包账户（如微信零钱、支付宝余额等）。
     */
    WALLET,

    /**
     * 现金账户。
     */
    CASH,
}

