package cn.turingzy.ledger.data.repository.transaction

import cn.turingzy.ledger.model.transaction.Transaction
import cn.turingzy.ledger.model.currency.CurrencyAmountOverride
import cn.turingzy.ledger.model.transaction.TransactionDetail
import cn.turingzy.ledger.model.transaction.TransactionBatchChange
import cn.turingzy.ledger.model.transaction.TransactionType
import kotlinx.coroutines.flow.Flow

/**
 * 交易数据仓库接口。
 *
 * 提供交易流水明细的查询、检索、多维度统计（收支汇总/分类汇总）、单笔增删改以及批量修改/删除能力。
 */
interface TransactionRepository {
    /**
     * 获取最近交易明细列表流。
     *
     * @param limit 返回的最大记录数，默认为 100
     * @return 包含关联账户、分类与图标的交易明细列表流
     */
    fun getTransactionsStream(limit: Int = 100): Flow<List<TransactionDetail>>

    /**
     * 根据搜索关键字检索交易列表流（匹配备注、分类名称、账户名称等）。
     *
     * @param query 搜索关键词
     * @return 匹配的交易明细列表流
     */
    fun searchTransactions(query: String): Flow<List<TransactionDetail>>

    /**
     * 获取最近记账使用过的分类 ID 列表流。
     *
     * @param limit 返回的最大分类数量
     * @return 最近使用的分类 ID 列表流
     */
    fun recentCategoryIds(limit: Int): Flow<List<Long>>

    /**
     * 根据交易 ID 获取指定交易详情流。
     *
     * @param id 交易 ID
     * @return 交易明细流；若不存在则发射 null
     */
    fun getTransaction(id: Long): Flow<TransactionDetail?>

    /**
     * 获取指定时间范围内的交易明细列表流。
     *
     * @param startTime 起始时间戳（毫秒）
     * @param endTime 截止时间戳（毫秒）
     * @param currency 指定评估的币种代码；若为 null 则使用用户设置的主币种
     * @return 交易明细列表流
     */
    fun getTransactionsBetween(startTime: Long, endTime: Long, currency: String? = null): Flow<List<TransactionDetail>>

    /**
     * 获取指定账户下的所有交易明细列表流。
     *
     * @param accountId 账户 ID
     * @return 交易明细列表流
     */
    fun getTransactionsByAccount(accountId: Long): Flow<List<TransactionDetail>>

    /**
     * 获取指定分类下的所有交易明细列表流。
     *
     * @param categoryId 分类 ID
     * @return 交易明细列表流
     */
    fun getTransactionsByCategory(categoryId: Long): Flow<List<TransactionDetail>>

    /**
     * 统计指定时间范围内某收支类型的折算金额总和流。
     *
     * @param type 交易类型（支出或收入）
     * @param startTime 起始时间戳（毫秒）
     * @param endTime 截止时间戳（毫秒）
     * @param currency 汇总币种；若为 null 则使用当前主币种
     * @return 折合后的金额总和流（单位：分）
     */
    fun getAmountSum(type: TransactionType, startTime: Long, endTime: Long, currency: String? = null): Flow<Long>

    /**
     * 统计指定时间范围内某个分类折算为主币种后的金额总和流。
     *
     * @param categoryId 分类 ID
     * @param startTime 起始时间戳（毫秒）
     * @param endTime 截止时间戳（毫秒）
     * @return 折合后的金额总和流（单位：分）
     */
    fun getCategoryAmountSum(categoryId: Long, startTime: Long, endTime: Long): Flow<Long>

    /**
     * 创建单笔交易，并在事务中原子扣减或增加关联账户余额。
     *
     * @param transaction 待创建的交易数据
     * @param amountOverride 用户可选指定的特定币种金额覆盖
     * @return 新增交易的自增 ID
     */
    suspend fun createTransaction(transaction: Transaction, amountOverride: CurrencyAmountOverride? = null): Long

    /**
     * 更新单笔交易，并在发生金额、账户或类型变化时在事务中回滚旧余额并应用新余额。
     *
     * @param transaction 待更新的交易数据
     * @param amountOverride 用户可选指定的特定币种金额覆盖
     */
    suspend fun updateTransaction(transaction: Transaction, amountOverride: CurrencyAmountOverride? = null)

    /**
     * 软删除指定 ID 的交易，并在事务中回滚对应的账户余额。
     *
     * @param id 待删除的交易 ID
     */
    suspend fun deleteTransaction(id: Long)

    /**
     * 批量修改或删除一组交易记录。
     *
     * @param ids 目标交易 ID 集合
     * @param change 批量操作类型（批量变更账户、批量变更分类或批量删除）
     */
    suspend fun applyBatch(ids: Set<Long>, change: TransactionBatchChange)
}

