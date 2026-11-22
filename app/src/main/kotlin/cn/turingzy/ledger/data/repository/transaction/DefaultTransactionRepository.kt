package cn.turingzy.ledger.data.repository.transaction

import cn.turingzy.ledger.data.repository.currency.ExchangeRateRepository
import cn.turingzy.ledger.data.repository.currency.MoneyValuation
import cn.turingzy.ledger.data.repository.settings.AppPreferencesRepository
import androidx.room.withTransaction
import cn.turingzy.ledger.data.database.LedgerDatabase
import cn.turingzy.ledger.data.database.dao.AccountDao
import cn.turingzy.ledger.data.database.dao.TransactionDao
import cn.turingzy.ledger.data.database.entity.TransactionEntity
import cn.turingzy.ledger.data.database.mapper.asEntity
import cn.turingzy.ledger.data.database.mapper.asExternalModel
import cn.turingzy.ledger.model.transaction.Transaction
import cn.turingzy.ledger.model.currency.CurrencyAmountOverride
import cn.turingzy.ledger.model.transaction.TransactionDetail
import cn.turingzy.ledger.model.transaction.TransactionBatchChange
import cn.turingzy.ledger.model.transaction.TransactionType
import cn.turingzy.ledger.di.Dispatcher
import cn.turingzy.ledger.di.LedgerDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import cn.turingzy.ledger.model.currency.primaryAmount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * [TransactionRepository] 的默认实现。
 *
 * 结合 [TransactionDao]、[AccountDao]、用户偏好设置以及 [MoneyValuation]，
 * 在 IO 调度器上完成交易记录的持久化、多币种动态估值转换、与关联账户余额的原子联动维护。
 *
 * @param database 账本本地数据库，用于多表原子事务
 * @param transactionDao 交易数据访问对象
 * @param accountDao 账户数据访问对象，用于联动更新账户余额
 * @param preferences 用户偏好设置仓库
 * @param valuation 货币估值计算与转换服务
 * @param rates 汇率仓库，监听汇率变动
 * @param ioDispatcher 用于执行数据库操作的 IO 协程调度器
 */
class DefaultTransactionRepository @Inject constructor(
    private val database: LedgerDatabase,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val preferences: AppPreferencesRepository,
    private val valuation: MoneyValuation,
    private val rates: ExchangeRateRepository,
    @param:Dispatcher(LedgerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : TransactionRepository {

    /**
     * 将数据库查询到的关联交易数据流与用户主币种及汇率变动结合，进行实时估值换算。
     */
    private fun valued(rows: Flow<List<cn.turingzy.ledger.data.database.entity.PopulatedTransaction>>, currency: String? = null): Flow<List<TransactionDetail>> =
        combine(rows, preferences.preferences, rates.changes) { entries, settings, _ -> entries to (currency ?: settings.primaryCurrency) }
            .map { (entries, primary) -> valuation.transactions(entries.map { it.asExternalModel() }, primary) }.flowOn(ioDispatcher)

    /**
     * 获取最近交易明细列表流（含估值计算）。
     *
     * @param limit 返回的最大数量
     */
    override fun getTransactionsStream(limit: Int): Flow<List<TransactionDetail>> = valued(transactionDao.getPopulatedTransactions(limit))

    /**
     * 搜索交易明细列表流（含估值计算）。
     *
     * @param query 搜索关键字
     */
    override fun searchTransactions(query: String): Flow<List<TransactionDetail>> = valued(transactionDao.searchTransactions(query))

    /**
     * 获取最近使用的分类 ID 列表。
     *
     * @param limit 最大条数
     */
    override fun recentCategoryIds(limit: Int): Flow<List<Long>> = transactionDao.recentCategoryIds(limit)

    /**
     * 获取单条交易详情流（含估值计算）。
     *
     * @param id 交易 ID
     */
    override fun getTransaction(id: Long): Flow<TransactionDetail?> =
        valued(transactionDao.getPopulatedTransactionById(id).map { listOfNotNull(it) }).map { it.firstOrNull() }

    /**
     * 获取指定时间范围内的交易明细列表流（含指定币种或主币种估值）。
     *
     * @param startTime 起始时间戳（毫秒）
     * @param endTime 截止时间戳（毫秒）
     * @param currency 目标币种代码（可选）
     */
    override fun getTransactionsBetween(startTime: Long, endTime: Long, currency: String?): Flow<List<TransactionDetail>> =
        valued(transactionDao.getPopulatedTransactionsBetween(startTime, endTime), currency)

    /**
     * 获取指定账户下的交易明细列表流（含估值计算）。
     *
     * @param accountId 账户 ID
     */
    override fun getTransactionsByAccount(accountId: Long): Flow<List<TransactionDetail>> = valued(transactionDao.getPopulatedTransactionsByAccount(accountId))

    /**
     * 获取指定分类下的交易明细列表流（含估值计算）。
     *
     * @param categoryId 分类 ID
     */
    override fun getTransactionsByCategory(categoryId: Long): Flow<List<TransactionDetail>> = valued(transactionDao.getPopulatedTransactionsByCategory(categoryId))

    /**
     * 统计指定时间范围内某收支类型折合主币种后的金额总和流。
     *
     * @param type 交易类型
     * @param startTime 起始时间戳（毫秒）
     * @param endTime 截止时间戳（毫秒）
     * @param currency 汇总目标币种（可选）
     */
    override fun getAmountSum(type: TransactionType, startTime: Long, endTime: Long, currency: String?): Flow<Long> =
        combine(getTransactionsBetween(startTime, endTime, currency), preferences.preferences) { entries, settings ->
            entries.filter { it.transaction.type == type }.fold(0L) { total, entry ->
                Math.addExact(total, checkNotNull(entry.primaryAmount(currency ?: settings.primaryCurrency)) { "缺少汇率" })
            }
        }

    /**
     * 统计指定分类在指定时间范围内折合主币种后的金额总和流。
     *
     * @param categoryId 分类 ID
     * @param startTime 起始时间戳（毫秒）
     * @param endTime 截止时间戳（毫秒）
     */
    override fun getCategoryAmountSum(categoryId: Long, startTime: Long, endTime: Long): Flow<Long> =
        combine(getTransactionsBetween(startTime, endTime), preferences.preferences) { entries, settings ->
            entries.filter { it.transaction.categoryId == categoryId }.fold(0L) { total, entry ->
                Math.addExact(total, checkNotNull(entry.primaryAmount(settings.primaryCurrency)) { "缺少汇率" })
            }
        }

    private suspend fun value(transaction: Transaction, amountOverride: CurrencyAmountOverride?): Transaction {
        val source = requireNotNull(accountDao.getAccountByIdOnce(transaction.accountId)) { "账户已删除" }.asExternalModel()
        val target = transaction.toAccountId?.let { requireNotNull(accountDao.getAccountByIdOnce(it)) { "转入账户已删除" }.asExternalModel() }
        require(transaction.amount > 0L && if (transaction.type == TransactionType.TRANSFER) target != null && target.id != source.id else target == null)
        return valuation.transaction(transaction, source, target, preferences.preferences.first().primaryCurrency, amountOverride)
    }

    private suspend fun changeBalance(id: Long, delta: Long) {
        val account = requireNotNull(accountDao.getAccountIncludingDeleted(id)) { "账户不存在" }
        accountDao.updateAccount(account.copy(currentBalance = Math.addExact(account.currentBalance, delta), baseBalance = null))
    }

    /**
     * 创建交易记录，并在数据库事务中原子更新相关账户余额。
     *
     * @param transaction 待创建的交易
     * @param amountOverride 可选的币种金额覆盖
     * @return 新增交易的自增 ID
     */
    override suspend fun createTransaction(transaction: Transaction, amountOverride: CurrencyAmountOverride?): Long =
        withContext(ioDispatcher) {
            val valued = value(transaction, amountOverride)
            database.withTransaction {
                check(accountDao.getAccountByIdOnce(transaction.accountId) != null) { "账户已删除" }
                transaction.toAccountId?.let { check(accountDao.getAccountByIdOnce(it) != null) { "转入账户已删除" } }
                when (transaction.type) {
                    TransactionType.EXPENSE -> {
                        changeBalance(transaction.accountId, -transaction.amount)
                    }
                    TransactionType.INCOME -> {
                        changeBalance(transaction.accountId, transaction.amount)
                    }
                    TransactionType.TRANSFER -> {
                        val toAccountId = checkNotNull(transaction.toAccountId) {
                            "Transfer destination account must not be null"
                        }
                        changeBalance(transaction.accountId, -transaction.amount)
                        changeBalance(toAccountId, checkNotNull(valued.toAmount))
                    }
                }
                transactionDao.insertTransaction(valued.asEntity())
            }
        }

    /**
     * 更新交易记录。若涉及金额、账户、转账目标或交易类型的财务变动，
     * 则在数据库事务中先回滚旧账户余额再应用新余额，并重新进行货币估值。
     *
     * @param transaction 待更新的交易
     * @param amountOverride 可选的币种金额覆盖
     */
    override suspend fun updateTransaction(transaction: Transaction, amountOverride: CurrencyAmountOverride?) {
        withContext(ioDispatcher) {
            val previous = checkNotNull(transactionDao.getTransactionByIdOnce(transaction.id)) { "交易已删除" }
            val financialChange = previous.amount != transaction.amount || previous.accountId != transaction.accountId ||
                previous.toAccountId != transaction.toAccountId || previous.type != transaction.type ||
                previous.transactionTime != transaction.transactionTime || (transaction.toAmount != null && previous.toAmount != transaction.toAmount)
            val updated = if (financialChange) {
                transaction.copy(baseAmount = null, baseCurrency = null, baseRate = null, baseRateDate = null, currencySnapshots = emptyList())
            } else {
                previous.asExternalModel().copy(note = transaction.note, categoryId = transaction.categoryId)
            }
            val valued = if (financialChange || amountOverride != null) value(updated, amountOverride) else updated
            database.withTransaction {
                val oldEntity = checkNotNull(transactionDao.getTransactionByIdOnce(transaction.id)) { "交易已删除" }
                check(oldEntity == previous && !oldEntity.isDeleted) { "交易已发生变化，请重新打开" }
                if (!financialChange) {
                    transactionDao.updateTransaction(valued.asEntity())
                    return@withTransaction
                }

                when (oldEntity.type) {
                    TransactionType.EXPENSE -> {
                        changeBalance(oldEntity.accountId, oldEntity.amount)
                    }
                    TransactionType.INCOME -> {
                        changeBalance(oldEntity.accountId, -oldEntity.amount)
                    }
                    TransactionType.TRANSFER -> {
                        oldEntity.toAccountId?.let { toAcc ->
                            changeBalance(oldEntity.accountId, oldEntity.amount)
                            changeBalance(toAcc, -(oldEntity.toAmount ?: oldEntity.amount))
                        }
                    }
                }

                when (transaction.type) {
                    TransactionType.EXPENSE -> {
                        changeBalance(transaction.accountId, -transaction.amount)
                    }
                    TransactionType.INCOME -> {
                        changeBalance(transaction.accountId, transaction.amount)
                    }
                    TransactionType.TRANSFER -> {
                        val toAccountId = checkNotNull(transaction.toAccountId) {
                            "Transfer destination account must not be null"
                        }
                        changeBalance(transaction.accountId, -transaction.amount)
                        changeBalance(toAccountId, checkNotNull(valued.toAmount))
                    }
                }

                transactionDao.updateTransaction(valued.copy(importKey = oldEntity.importKey).asEntity())
            }
        }
    }

    /**
     * 软删除指定交易，并在数据库事务中反向回滚对账户余额的影响。
     *
     * @param id 交易 ID
     */
    override suspend fun deleteTransaction(id: Long) {
        withContext(ioDispatcher) {
            database.withTransaction {
                val entity = transactionDao.getTransactionByIdOnce(id) ?: return@withTransaction
                if (entity.isDeleted || (entity.type == TransactionType.TRANSFER && entity.toAccountId == null)) {
                    return@withTransaction
                }
                delete(entity)
            }
        }
    }

    /**
     * 批量应用变更至一组交易（支持批量改账户、批量改分类、批量删除），并在事务中保证账户余额一致性。
     *
     * @param ids 目标交易 ID 集合
     * @param change 批量变更指令
     */
    override suspend fun applyBatch(ids: Set<Long>, change: TransactionBatchChange) {
        if (ids.isEmpty()) return
        withContext(ioDispatcher) {
            database.withTransaction {
                val transactions = ids.map { id ->
                    checkNotNull(transactionDao.getTransactionByIdOnce(id)).also { check(!it.isDeleted) }
                }
                when (change) {
                    is TransactionBatchChange.Account -> {
                        val target = checkNotNull(accountDao.getAccountByIdOnce(change.accountId))
                        transactions.forEach { transaction ->
                            val source = checkNotNull(accountDao.getAccountIncludingDeleted(transaction.accountId))
                            require(source.currency == target.currency && target.accountId != transaction.toAccountId)
                            if (source.accountId != target.accountId) {
                                val delta = sourceDelta(transaction)
                                changeBalance(source.accountId, -delta)
                                changeBalance(target.accountId, delta)
                                transactionDao.updateTransaction(transaction.copy(accountId = target.accountId))
                            }
                        }
                    }
                    is TransactionBatchChange.Category -> {
                        val category = checkNotNull(database.categoryDao().getCategoryByIdOnce(change.categoryId))
                        transactions.forEach { transaction ->
                            require(category.type.name == transaction.type.name)
                            transactionDao.updateTransaction(transaction.copy(categoryId = category.categoryId))
                        }
                    }
                    TransactionBatchChange.Delete -> transactions.forEach { delete(it) }
                }
            }
        }
    }

    private fun sourceDelta(entity: TransactionEntity): Long =
        if (entity.type == TransactionType.INCOME) entity.amount else -entity.amount

    private suspend fun delete(entity: TransactionEntity) {
        changeBalance(entity.accountId, -sourceDelta(entity))
        if (entity.type == TransactionType.TRANSFER) {
            changeBalance(checkNotNull(entity.toAccountId), -(entity.toAmount ?: entity.amount))
        }
        transactionDao.softDeleteTransaction(entity.transactionId)
    }
}
