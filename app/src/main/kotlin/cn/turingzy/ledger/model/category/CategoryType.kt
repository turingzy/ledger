package cn.turingzy.ledger.model.category

import kotlinx.serialization.Serializable

/**
 * 分类业务类型枚举。
 *
 * 标识分类适用于何种交易场景。
 */
@Serializable
enum class CategoryType {
    /**
     * 支出分类。
     */
    EXPENSE,

    /**
     * 收入分类。
     */
    INCOME,

    /**
     * 转账分类。
     */
    TRANSFER,
}

