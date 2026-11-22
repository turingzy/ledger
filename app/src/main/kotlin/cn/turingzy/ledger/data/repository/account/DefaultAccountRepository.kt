package cn.turingzy.ledger.data.repository.account

import cn.turingzy.ledger.data.repository.currency.ExchangeRateRepository
import cn.turingzy.ledger.data.repository.currency.MoneyValuation
import cn.turingzy.ledger.data.repository.settings.AppPreferencesRepository
import cn.turingzy.ledger.data.database.dao.AccountDao
import cn.turingzy.ledger.data.database.mapper.asEntity
import cn.turingzy.ledger.data.database.mapper.asExternalModel
import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountWithTemplate
import cn.turingzy.ledger.di.Dispatcher
import cn.turingzy.ledger.di.LedgerDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * [AccountRepository] 的默认实现。
 *
 * 结合 [AccountDao]、用户偏好设置以及汇率信息，在指定的 IO 调度器上处理账户的持久化、多币种估值和余额调整。
 *
 * @property accountDao 账户数据访问对象
 * @property preferences 用户偏好设置仓库，提供当前主币种配置
 * @property valuation 货币估值计算工具，用于换算账户基准余额
 * @property rates 汇率仓库，监听汇率变动
 * @property ioDispatcher 用于执行数据库及后台计算的 IO 协程调度器
 */
class DefaultAccountRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val preferences: AppPreferencesRepository,
    private val valuation: MoneyValuation,
    private val rates: ExchangeRateRepository,
    @param:Dispatcher(LedgerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : AccountRepository {

    /**
     * 获取所有账户及其关联卡片模板的流，并结合当前主币种和汇率计算基准金额。
     */
    override fun getAccountsStream(): Flow<List<AccountWithTemplate>> =
        combine(accountDao.getPopulatedAccounts(), preferences.preferences, rates.changes) { entries, settings, _ -> entries to settings.primaryCurrency }
            .map { (entries, primary) -> valuation.accounts(entries.map { it.asExternalModel() }, primary) }
            .flowOn(ioDispatcher)

    /**
     * 根据账户 ID 获取指定账户及卡片模板信息。
     *
     * @param id 账户 ID
     */
    override fun getAccount(id: Long): Flow<AccountWithTemplate?> =
        getAccountsStream().map { entries -> entries.firstOrNull { it.account.id == id } }
            .flowOn(ioDispatcher)

    /**
     * 计算所有标记为计入总资产的账户折合主币种后的总资产金额流。
     */
    override fun getTotalAsset(): Flow<Long> =
        getAccountsStream().map { entries -> entries.filter { it.account.includeInTotal }.fold(0L) { total, entry ->
            Math.addExact(total, checkNotNull(entry.account.baseBalance) { "缺少汇率" })
        } }

    /**
     * 计算所有信用卡类型账户折合主币种后的总负债金额流。
     */
    override fun getTotalCreditDebt(): Flow<Long> =
        getAccountsStream().map { entries -> entries.filter { it.account.type == cn.turingzy.ledger.model.account.AccountType.CREDIT }.fold(0L) { total, entry ->
            Math.addExact(total, checkNotNull(entry.account.baseBalance) { "缺少汇率" })
        } }

    /**
     * 创建账户。如果初始余额不为 0 且当前余额为 0，则默认将当前余额设置为初始余额。
     *
     * @param account 待创建的账户
     * @return 新账户自增 ID
     */
    override suspend fun createAccount(account: Account): Long =
        withContext(ioDispatcher) {
            val accountToSave = if (account.currentBalance == 0L && account.initialBalance != 0L) {
                account.copy(currentBalance = account.initialBalance)
            } else {
                account
            }
            accountDao.insertAccount(accountToSave.asEntity())
        }

    /**
     * 全量更新账户信息。
     *
     * @param account 待更新的账户
     */
    override suspend fun updateAccount(account: Account) {
        withContext(ioDispatcher) {
            accountDao.updateAccount(account.asEntity())
        }
    }

    /**
     * 仅更新账户的元数据，不修改当前余额。
     *
     * @param account 包含新元数据的账户
     */
    override suspend fun updateAccountInfo(account: Account) {
        withContext(ioDispatcher) {
            accountDao.updateAccountInfo(
                accountId = account.id,
                name = account.name,
                type = account.type,
                initialBalance = account.initialBalance,
                templateId = account.templateId,
                cardNumber = account.cardNumber,
                includeInTotal = account.includeInTotal,
            )
        }
    }

    /**
     * 软删除指定 ID 的账户。
     *
     * @param id 待删除的账户 ID
     */
    override suspend fun deleteAccount(id: Long) {
        withContext(ioDispatcher) {
            accountDao.softDeleteAccount(id)
        }
    }

    /**
     * 调整账户余额至目标值，通过计算增量原子更新。
     *
     * @param accountId 账户 ID
     * @param newBalance 目标余额（单位：分）
     */
    override suspend fun adjustBalance(accountId: Long, newBalance: Long) {
        withContext(ioDispatcher) {
            val account = accountDao.getAccountByIdOnce(accountId) ?: return@withContext
            val delta = newBalance - account.currentBalance
            accountDao.updateBalance(accountId, delta)
        }
    }
}

