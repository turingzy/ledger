package cn.turingzy.ledger.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.data.database.entity.AccountEntity
import cn.turingzy.ledger.data.database.entity.PopulatedAccount
import kotlinx.coroutines.flow.Flow

/**
 * 账户数据访问对象 (DAO)。
 *
 * 提供账户实体的增删改查、余额增量变动、估值缓存更新以及资产总额统计等操作。
 */
@Dao
interface AccountDao {
    /**
     * 更新账户的基准货币估值信息。
     *
     * 仅在 [original] 与当前账户余额一致时才更新，防止并发修改覆盖。
     *
     * @param id 账户 ID
     * @param original 更新前校验的原始余额
     * @param balance 计算出的基准货币余额
     * @param currency 基准货币代码
     * @param rate 使用的汇率字符串
     * @param date 汇率日期
     */
    @Query("UPDATE tb_account SET base_balance = :balance, base_currency = :currency, base_rate = :rate, base_rate_date = :date WHERE account_id = :id AND current_balance = :original")
    suspend fun updateValuation(id: Long, original: Long, balance: Long, currency: String, rate: String?, date: String)

    /**
     * 查询指定 ID 的账户，包含已标记为软删除的记录。
     *
     * @param id 账户 ID
     * @return 账户实体，若不存在则为 null
     */
    @Query("SELECT * FROM tb_account WHERE account_id = :id")
    suspend fun getAccountIncludingDeleted(id: Long): AccountEntity?

    /**
     * 查询所有账户记录，供数据备份使用。
     */
    @Query("SELECT * FROM tb_account")
    suspend fun backupRows(): List<AccountEntity>

    /**
     * 清空所有账户记录（通常用于数据恢复前的数据清理）。
     */
    @Query("DELETE FROM tb_account")
    suspend fun clearRows()

    /**
     * 插入单个账户记录，若存在冲突则替换。
     *
     * @param account 账户实体
     * @return 新插入账户的自增主键 ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    /**
     * 批量插入账户记录，若冲突则忽略。
     *
     * @param accounts 账户实体列表
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    /**
     * 更新账户实体。
     *
     * @param account 账户实体
     */
    @Update
    suspend fun updateAccount(account: AccountEntity)

    /**
     * 更新未删除账户的基本属性信息。
     *
     * @param accountId 账户 ID
     * @param name 账户名称
     * @param type 账户类型
     * @param initialBalance 初始余额（分）
     * @param templateId 关联的卡片模板 ID
     * @param cardNumber 卡号后四位或标识
     * @param includeInTotal 是否计入净资产统计
     * @return 影响的行数
     */
    @Query(
        """
        UPDATE tb_account
        SET name = :name, type = :type, initial_balance = :initialBalance,
            template_id = :templateId, card_number = :cardNumber, encrypted_card_details = :encryptedCardDetails,
            include_in_total = :includeInTotal
        WHERE account_id = :accountId AND is_deleted = 0
        """,
    )
    suspend fun updateAccountInfo(
        accountId: Long,
        name: String,
        type: AccountType,
        initialBalance: Long,
        templateId: Long?,
        cardNumber: String?,
        includeInTotal: Boolean,
        encryptedCardDetails: String?,
    ): Int

    /**
     * 软删除指定账户（将 [AccountEntity.isDeleted] 置为 1）。
     *
     * @param id 账户 ID
     * @return 影响的行数
     */
    @Query("UPDATE tb_account SET is_deleted = 1 WHERE account_id = :id")
    suspend fun softDeleteAccount(id: Long): Int

    /**
     * 获取所有未删除账户及其关联模板信息的响应式数据流。
     *
     * @return 包含卡片模板详情的账户列表流
     */
    @Transaction
    @Query("SELECT * FROM tb_account WHERE is_deleted = 0 ORDER BY account_id ASC")
    fun getPopulatedAccounts(): Flow<List<PopulatedAccount>>

    /**
     * 一次性获取未删除的指定账户实体。
     *
     * @param id 账户 ID
     * @return 账户实体，若不存在或已删除则返回 null
     */
    @Query("SELECT * FROM tb_account WHERE account_id = :id AND is_deleted = 0")
    suspend fun getAccountByIdOnce(id: Long): AccountEntity?

    /**
     * 增量更新账户余额，并重置基准货币估值缓存为 NULL。
     *
     * @param accountId 账户 ID
     * @param deltaAmount 变动金额（分，正数增加，负数减少）
     * @return 影响的行数
     */
    @Query("UPDATE tb_account SET current_balance = current_balance + :deltaAmount, base_balance = NULL WHERE account_id = :accountId")
    suspend fun updateBalance(accountId: Long, deltaAmount: Long): Int

    /**
     * 统计所有计入总资产且未删除账户的当前余额总和。
     *
     * @return 资产总额（分）的数据流
     */
    @Query("SELECT COALESCE(SUM(current_balance), 0) FROM tb_account WHERE include_in_total = 1 AND is_deleted = 0")
    fun getTotalAsset(): Flow<Long>

    /**
     * 统计所有未删除信用账户的当前欠款/余额总和。
     *
     * @return 信用卡总负债（分）的数据流
     */
    @Query("SELECT COALESCE(SUM(current_balance), 0) FROM tb_account WHERE type = 'CREDIT' AND is_deleted = 0")
    fun getTotalCreditDebt(): Flow<Long>
}

