package cn.turingzy.ledger.data.repository

import cn.turingzy.ledger.model.Transaction
import cn.turingzy.ledger.model.TransactionDetail
import cn.turingzy.ledger.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getTransactionsStream(limit: Int = 100): Flow<List<TransactionDetail>>
    fun getTransaction(id: Long): Flow<TransactionDetail?>
    fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<TransactionDetail>>
    fun getTransactionsByAccount(accountId: Long): Flow<List<TransactionDetail>>
    fun getTransactionsByCategory(categoryId: Long): Flow<List<TransactionDetail>>
    fun getAmountSum(type: TransactionType, startTime: Long, endTime: Long): Flow<Long>
    fun getCategoryAmountSum(categoryId: Long, startTime: Long, endTime: Long): Flow<Long>
    suspend fun createTransaction(transaction: Transaction): Long
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(id: Long)
}

