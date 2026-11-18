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

@Dao
interface TransactionDao {
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

    @Query("UPDATE tb_transaction SET base_amount = :amount, base_currency = :currency, base_rate = :rate, base_rate_date = :date, to_amount = :toAmount, transfer_rate = :transferRate, currency_snapshots = :snapshots WHERE transaction_id = :id AND amount = :originalAmount AND transaction_time = :originalTime")
    suspend fun updateValuation(id: Long, originalAmount: Long, originalTime: Long, amount: Long?, currency: String?, rate: String?, date: String?, toAmount: Long?, transferRate: String?, snapshots: String)

    @Query("SELECT * FROM tb_transaction")
    suspend fun backupRows(): List<TransactionEntity>

    @Query("DELETE FROM tb_transaction")
    suspend fun clearRows()


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("UPDATE tb_transaction SET is_deleted = 1 WHERE transaction_id = :id")
    suspend fun softDeleteTransaction(id: Long): Int

    @Transaction
    @Query("SELECT * FROM tb_transaction WHERE is_deleted = 0 ORDER BY transaction_time DESC, transaction_id DESC LIMIT :limit")
    fun getPopulatedTransactions(limit: Int = 100): Flow<List<PopulatedTransaction>>

    @Transaction
    @Query("SELECT * FROM tb_transaction WHERE transaction_id = :id AND is_deleted = 0")
    fun getPopulatedTransactionById(id: Long): Flow<PopulatedTransaction?>

    @Transaction
    @Query("SELECT * FROM tb_transaction WHERE transaction_time BETWEEN :startTime AND :endTime AND is_deleted = 0 ORDER BY transaction_time DESC, transaction_id DESC")
    fun getPopulatedTransactionsBetween(startTime: Long, endTime: Long): Flow<List<PopulatedTransaction>>

    @Transaction
    @Query("SELECT * FROM tb_transaction WHERE (account_id = :accountId OR to_account_id = :accountId) AND is_deleted = 0 ORDER BY transaction_time DESC, transaction_id DESC")
    fun getPopulatedTransactionsByAccount(accountId: Long): Flow<List<PopulatedTransaction>>

    @Transaction
    @Query("SELECT * FROM tb_transaction WHERE category_id = :categoryId AND is_deleted = 0 ORDER BY transaction_time DESC, transaction_id DESC")
    fun getPopulatedTransactionsByCategory(categoryId: Long): Flow<List<PopulatedTransaction>>

    @Query("SELECT * FROM tb_transaction WHERE transaction_id = :id AND is_deleted = 0")
    suspend fun getTransactionByIdOnce(id: Long): TransactionEntity?
}

