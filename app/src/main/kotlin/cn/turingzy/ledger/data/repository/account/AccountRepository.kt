package cn.turingzy.ledger.data.repository.account

import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountWithTemplate
import kotlinx.coroutines.flow.Flow

/**
 * 账户数据仓库接口。
 *
 * 提供账户数据的查询、创建、更新、软删除以及余额调整等核心功能。
 */
interface AccountRepository {
    /**
     * 获取所有账户及其关联卡片模板列表的响应式流。
     *
     * 账户金额会自动折算为主币种基准金额。
     */
    fun getAccountsStream(): Flow<List<AccountWithTemplate>>

    /**
     * 根据账户 ID 获取指定账户及其关联卡片模板信息的流。
     *
     * @param id 账户 ID
     * @return 包含账户详情的流；若不存在则发射 null
     */
    fun getAccount(id: Long): Flow<AccountWithTemplate?>

    /**
     * 获取所有计入总资产的账户折合主币种后的总资产金额流。
     *
     * 金额单位为分。
     */
    fun getTotalAsset(): Flow<Long>

    /**
     * 获取所有信用卡类型账户折合主币种后的总负债金额流。
     *
     * 金额单位为分。
     */
    fun getTotalCreditDebt(): Flow<Long>

    /**
     * 创建新账户。
     *
     * @param account 待创建的账户信息
     * @return 新创建账户的自增 ID
     */
    suspend fun createAccount(account: Account): Long

    /**
     * 全量更新账户信息（包括余额等字段）。
     *
     * @param account 待更新的账户信息
     */
    suspend fun updateAccount(account: Account)

    /**
     * 仅更新账户的元数据（如名称、类型、卡号、卡片模板、是否计入总资产等），不修改当前余额。
     *
     * @param account 待更新的账户信息
     */
    suspend fun updateAccountInfo(account: Account)

    /**
     * 软删除指定 ID 的账户。
     *
     * @param id 待删除的账户 ID
     */
    suspend fun deleteAccount(id: Long)

    /**
     * 调整账户余额至指定新金额，通过计算差额原子更新数据库。
     *
     * @param accountId 账户 ID
     * @param newBalance 调整后的目标余额（单位：分）
     */
    suspend fun adjustBalance(accountId: Long, newBalance: Long)
}

