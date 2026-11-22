package cn.turingzy.ledger.model.transaction

/**
 * 交易批量变更操作密封接口。
 *
 * 定义在多选批量操作模式下对选中交易所支持的修改类型。
 */
sealed interface TransactionBatchChange {
    /**
     * 批量更改所属账户。
     *
     * @property accountId 目标账户 ID
     */
    data class Account(val accountId: Long) : TransactionBatchChange

    /**
     * 批量更改所属分类。
     *
     * @property categoryId 目标分类 ID
     */
    data class Category(val categoryId: Long) : TransactionBatchChange

    /**
     * 批量删除选中交易。
     */
    data object Delete : TransactionBatchChange
}
