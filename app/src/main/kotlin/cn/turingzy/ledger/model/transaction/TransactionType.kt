package cn.turingzy.ledger.model.transaction

import kotlinx.serialization.Serializable

/**
 * 交易业务类型枚举。
 *
 * 区分单笔财务记录的资金流向模式。
 */
@Serializable
enum class TransactionType {
    /**
     * 支出交易（资金从账户流出）。
     */
    EXPENSE,

    /**
     * 收入交易（资金流入账户）。
     */
    INCOME,

    /**
     * 转账交易（资金在内部账户之间划转）。
     */
    TRANSFER,
}

