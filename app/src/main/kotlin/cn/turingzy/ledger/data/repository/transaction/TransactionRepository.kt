package cn.turingzy.ledger.data.repository.transaction

import cn.turingzy.ledger.model.transaction.Transaction
import cn.turingzy.ledger.model.currency.CurrencyAmountOverride
import cn.turingzy.ledger.model.transaction.TransactionDetail
import cn.turingzy.ledger.model.transaction.TransactionBatchChange
import cn.turingzy.ledger.model.transaction.TransactionType
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getTransactionsStream(limit: Int = 100): Flow<List<TransactionDetail>>
    fun searchTransactions(query: String): Flow<List<TransactionDetail>>
    fun getTransaction(id: Long): Flow<TransactionDetail?>
    fun getTransactionsBetween(startTime: Long, endTime: Long, currency: String? = null): Flow<List<TransactionDetail>>
    fun getTransactionsByAccount(accountId: Long): Flow<List<TransactionDetail>>
    fun getTransactionsByCategory(categoryId: Long): Flow<List<TransactionDetail>>
    fun getAmountSum(type: TransactionType, startTime: Long, endTime: Long, currency: String? = null): Flow<Long>
    fun getCategoryAmountSum(categoryId: Long, startTime: Long, endTime: Long): Flow<Long>
    suspend fun createTransaction(transaction: Transaction, amountOverride: CurrencyAmountOverride? = null): Long
    suspend fun updateTransaction(transaction: Transaction, amountOverride: CurrencyAmountOverride? = null)
    suspend fun deleteTransaction(id: Long)
    suspend fun applyBatch(ids: Set<Long>, change: TransactionBatchChange)
}

