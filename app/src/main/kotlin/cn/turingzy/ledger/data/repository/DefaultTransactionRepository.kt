package cn.turingzy.ledger.data.repository

import androidx.room.withTransaction
import cn.turingzy.ledger.database.LedgerDatabase
import cn.turingzy.ledger.database.dao.AccountDao
import cn.turingzy.ledger.database.dao.TransactionDao
import cn.turingzy.ledger.database.mapper.asEntity
import cn.turingzy.ledger.database.mapper.asExternalModel
import cn.turingzy.ledger.model.Transaction
import cn.turingzy.ledger.model.TransactionDetail
import cn.turingzy.ledger.model.TransactionType
import cn.turingzy.ledger.util.Dispatcher
import cn.turingzy.ledger.util.LedgerDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DefaultTransactionRepository @Inject constructor(
    private val database: LedgerDatabase,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    @param:Dispatcher(LedgerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : TransactionRepository {

    override fun getTransactionsStream(limit: Int): Flow<List<TransactionDetail>> =
        transactionDao.getPopulatedTransactions(limit)
            .map { list -> list.map { it.asExternalModel() } }
            .flowOn(ioDispatcher)

    override fun getTransaction(id: Long): Flow<TransactionDetail?> =
        transactionDao.getPopulatedTransactionById(id)
            .map { it?.asExternalModel() }
            .flowOn(ioDispatcher)

    override fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<TransactionDetail>> =
        transactionDao.getPopulatedTransactionsBetween(startTime, endTime)
            .map { list -> list.map { it.asExternalModel() } }
            .flowOn(ioDispatcher)

    override fun getTransactionsByAccount(accountId: Long): Flow<List<TransactionDetail>> =
        transactionDao.getPopulatedTransactionsByAccount(accountId)
            .map { list -> list.map { it.asExternalModel() } }
            .flowOn(ioDispatcher)

    override fun getTransactionsByCategory(categoryId: Long): Flow<List<TransactionDetail>> =
        transactionDao.getPopulatedTransactionsByCategory(categoryId)
            .map { list -> list.map { it.asExternalModel() } }
            .flowOn(ioDispatcher)

    override fun getAmountSum(type: TransactionType, startTime: Long, endTime: Long): Flow<Long> =
        transactionDao.getAmountSumByTypeAndDateRange(type, startTime, endTime)
            .flowOn(ioDispatcher)

    override fun getCategoryAmountSum(categoryId: Long, startTime: Long, endTime: Long): Flow<Long> =
        transactionDao.getAmountSumByCategoryAndDateRange(categoryId, startTime, endTime)
            .flowOn(ioDispatcher)

    override suspend fun createTransaction(transaction: Transaction): Long =
        withContext(ioDispatcher) {
            database.withTransaction {
                when (transaction.type) {
                    TransactionType.EXPENSE -> {
                        accountDao.updateBalance(transaction.accountId, -transaction.amount)
                    }
                    TransactionType.INCOME -> {
                        accountDao.updateBalance(transaction.accountId, transaction.amount)
                    }
                    TransactionType.TRANSFER -> {
                        val toAccountId = checkNotNull(transaction.toAccountId) {
                            "Transfer destination account must not be null"
                        }
                        accountDao.updateBalance(transaction.accountId, -transaction.amount)
                        accountDao.updateBalance(toAccountId, transaction.amount)
                    }
                }
                transactionDao.insertTransaction(transaction.asEntity())
            }
        }

    override suspend fun updateTransaction(transaction: Transaction) {
        withContext(ioDispatcher) {
            database.withTransaction {
                val oldEntity = transactionDao.getTransactionByIdOnce(transaction.id) ?: return@withTransaction

                when (oldEntity.type) {
                    TransactionType.EXPENSE -> {
                        accountDao.updateBalance(oldEntity.accountId, oldEntity.amount)
                    }
                    TransactionType.INCOME -> {
                        accountDao.updateBalance(oldEntity.accountId, -oldEntity.amount)
                    }
                    TransactionType.TRANSFER -> {
                        oldEntity.toAccountId?.let { toAcc ->
                            accountDao.updateBalance(oldEntity.accountId, oldEntity.amount)
                            accountDao.updateBalance(toAcc, -oldEntity.amount)
                        }
                    }
                }

                when (transaction.type) {
                    TransactionType.EXPENSE -> {
                        accountDao.updateBalance(transaction.accountId, -transaction.amount)
                    }
                    TransactionType.INCOME -> {
                        accountDao.updateBalance(transaction.accountId, transaction.amount)
                    }
                    TransactionType.TRANSFER -> {
                        val toAccountId = checkNotNull(transaction.toAccountId) {
                            "Transfer destination account must not be null"
                        }
                        accountDao.updateBalance(transaction.accountId, -transaction.amount)
                        accountDao.updateBalance(toAccountId, transaction.amount)
                    }
                }

                transactionDao.updateTransaction(transaction.asEntity())
            }
        }
    }

    override suspend fun deleteTransaction(id: Long) {
        withContext(ioDispatcher) {
            database.withTransaction {
                val entity = transactionDao.getTransactionByIdOnce(id) ?: return@withTransaction
                if (entity.isDeleted) return@withTransaction

                when (entity.type) {
                    TransactionType.EXPENSE -> {
                        accountDao.updateBalance(entity.accountId, entity.amount)
                    }
                    TransactionType.INCOME -> {
                        accountDao.updateBalance(entity.accountId, -entity.amount)
                    }
                    TransactionType.TRANSFER -> {
                        val toAccountId = entity.toAccountId ?: return@withTransaction
                        accountDao.updateBalance(entity.accountId, entity.amount)
                        accountDao.updateBalance(toAccountId, -entity.amount)
                    }
                }

                transactionDao.softDeleteTransaction(id)
            }
        }
    }
}

