package cn.turingzy.ledger.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import cn.turingzy.ledger.database.model.PopulatedTransaction
import cn.turingzy.ledger.database.model.TransactionEntity
import cn.turingzy.ledger.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

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

    @Query("SELECT COALESCE(SUM(amount), 0) FROM tb_transaction WHERE type = :type AND transaction_time BETWEEN :startTime AND :endTime AND is_deleted = 0")
    fun getAmountSumByTypeAndDateRange(type: TransactionType, startTime: Long, endTime: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM tb_transaction WHERE category_id = :categoryId AND transaction_time BETWEEN :startTime AND :endTime AND is_deleted = 0")
    fun getAmountSumByCategoryAndDateRange(categoryId: Long, startTime: Long, endTime: Long): Flow<Long>
}

