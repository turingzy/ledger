package cn.turingzy.ledger.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import cn.turingzy.ledger.data.database.entity.PopulatedTransaction
import cn.turingzy.ledger.data.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * 交易记录数据访问对象 (DAO)。
 *
 * 提供记账交易记录的增删改查、多表关联聚合查询（账户、分类、转账目标账户）、关键字搜索及汇率估值缓存刷新等功能。
 */
@Dao
interface TransactionDao {
    /**
     * 根据关键字模糊搜索交易记录。
     *
     * 匹配备注、分类名称、源账户名称或转账目标账户名称。
     *
     * @param query 搜索关键词
     * @return 符合条件的聚合交易记录列表流，按交易时间倒序排列
     */
    @Transaction
    @Query("""SELECT t.* FROM tb_transaction t
        JOIN tb_account a ON a.account_id = t.account_id
        JOIN tb_category c ON c.category_id = t.category_id
        LEFT JOIN tb_account target ON target.account_id = t.to_account_id
        WHERE t.is_deleted = 0 AND (instr(lower(COALESCE(t.note, '')), lower(:query)) > 0
        OR instr(lower(c.name), lower(:query)) > 0 OR instr(lower(a.name), lower(:query)) > 0
        OR instr(lower(COALESCE(target.name, '')), lower(:query)) > 0)
        ORDER BY t.transaction_time DESC, t.transaction_id DESC""")
    fun searchTransactions(query: String): Flow<List<PopulatedTransaction>>

    /**
     * 查询最近使用的分类 ID 列表。
     *
     * @param limit 返回条数上限
     * @return 分类 ID 列表流
     */
    @Query("SELECT category_id FROM tb_transaction WHERE is_deleted = 0 ORDER BY transaction_time DESC, transaction_id DESC LIMIT :limit")
    fun recentCategoryIds(limit: Int): Flow<List<Long>>

    /**
     * 更新交易记录的估值信息与货币快照。
     *
     * 采用乐观并发控制，仅当 [originalAmount] 与 [originalTime] 与现有记录匹配时才更新。
     *
     * @param id 交易 ID
     * @param originalAmount 原交易金额
     * @param originalTime 原交易时间戳
     * @param amount 基准货币折算金额
     * @param currency 基准货币代码
     * @param rate 基准汇率
     * @param date 汇率生效日期
     * @param toAmount 转账目标账户折算金额
     * @param transferRate 转账汇率
     * @param snapshots 序列化的货币快照 JSON 字符串
     */
    @Query("UPDATE tb_transaction SET base_amount = :amount, base_currency = :currency, base_rate = :rate, base_rate_date = :date, to_amount = :toAmount, transfer_rate = :transferRate, currency_snapshots = :snapshots WHERE transaction_id = :id AND amount = :originalAmount AND transaction_time = :originalTime")
    suspend fun updateValuation(id: Long, originalAmount: Long, originalTime: Long, amount: Long?, currency: String?, rate: String?, date: String?, toAmount: Long?, transferRate: String?, snapshots: String)

    /**
     * 查询所有交易记录，供数据备份使用。
     */
    @Query("SELECT * FROM tb_transaction")
    suspend fun backupRows(): List<TransactionEntity>

    /**
     * 清空所有交易记录（通常用于恢复备份前的数据清理）。
     */
    @Query("DELETE FROM tb_transaction")
    suspend fun clearRows()

    /**
     * 插入单个交易记录，主键冲突时替换。
     *
     * @param transaction 交易实体
     * @return 新增交易记录的主键 ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    /**
     * 更新交易记录实体。
     *
     * @param transaction 待更新的交易实体
     */
    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    /**
     * 软删除指定交易记录。
     *
     * @param id 交易 ID
     * @return 影响的行数
     */
    @Query("UPDATE tb_transaction SET is_deleted = 1 WHERE transaction_id = :id")
    suspend fun softDeleteTransaction(id: Long): Int

    /**
     * 获取指定条数的未删除关联交易记录列表流。
     *
     * @param limit 返回记录最大数量，默认为 100
     * @return 聚合交易记录列表流，按交易时间倒序排列
     */
    @Transaction
    @Query("SELECT * FROM tb_transaction WHERE is_deleted = 0 ORDER BY transaction_time DESC, transaction_id DESC LIMIT :limit")
    fun getPopulatedTransactions(limit: Int = 100): Flow<List<PopulatedTransaction>>

    /**
     * 观察指定 ID 的未删除关联交易记录详情。
     *
     * @param id 交易 ID
     * @return 聚合交易记录流，若不存在或已删除则为 null
     */
    @Transaction
    @Query("SELECT * FROM tb_transaction WHERE transaction_id = :id AND is_deleted = 0")
    fun getPopulatedTransactionById(id: Long): Flow<PopulatedTransaction?>

    /**
     * 查询指定时间范围内的关联交易记录。
     *
     * @param startTime 起始时间戳（毫秒）
     * @param endTime 截止时间戳（毫秒）
     * @return 聚合交易记录列表流
     */
    @Transaction
    @Query("SELECT * FROM tb_transaction WHERE transaction_time BETWEEN :startTime AND :endTime AND is_deleted = 0 ORDER BY transaction_time DESC, transaction_id DESC")
    fun getPopulatedTransactionsBetween(startTime: Long, endTime: Long): Flow<List<PopulatedTransaction>>

    /**
     * 查询与指定账户关联的所有未删除交易记录（作为源账户或目标转入账户）。
     *
     * @param accountId 账户 ID
     * @return 聚合交易记录列表流
     */
    @Transaction
    @Query("SELECT * FROM tb_transaction WHERE (account_id = :accountId OR to_account_id = :accountId) AND is_deleted = 0 ORDER BY transaction_time DESC, transaction_id DESC")
    fun getPopulatedTransactionsByAccount(accountId: Long): Flow<List<PopulatedTransaction>>

    /**
     * 查询属于指定分类的所有未删除交易记录。
     *
     * @param categoryId 分类 ID
     * @return 聚合交易记录列表流
     */
    @Transaction
    @Query("SELECT * FROM tb_transaction WHERE category_id = :categoryId AND is_deleted = 0 ORDER BY transaction_time DESC, transaction_id DESC")
    fun getPopulatedTransactionsByCategory(categoryId: Long): Flow<List<PopulatedTransaction>>

    /**
     * 一次性查询未删除的指定交易实体。
     *
     * @param id 交易 ID
     * @return 交易实体，不存在或已删除则返回 null
     */
    @Query("SELECT * FROM tb_transaction WHERE transaction_id = :id AND is_deleted = 0")
    suspend fun getTransactionByIdOnce(id: Long): TransactionEntity?
}

