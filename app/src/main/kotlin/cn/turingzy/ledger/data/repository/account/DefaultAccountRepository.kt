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

class DefaultAccountRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val preferences: AppPreferencesRepository,
    private val valuation: MoneyValuation,
    private val rates: ExchangeRateRepository,
    @param:Dispatcher(LedgerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : AccountRepository {

    override fun getAccountsStream(): Flow<List<AccountWithTemplate>> =
        combine(accountDao.getPopulatedAccounts(), preferences.preferences, rates.changes) { entries, settings, _ -> entries to settings.primaryCurrency }
            .map { (entries, primary) -> valuation.accounts(entries.map { it.asExternalModel() }, primary) }
            .flowOn(ioDispatcher)

    override fun getAccount(id: Long): Flow<AccountWithTemplate?> =
        getAccountsStream().map { entries -> entries.firstOrNull { it.account.id == id } }
            .flowOn(ioDispatcher)

    override fun getTotalAsset(): Flow<Long> =
        getAccountsStream().map { entries -> entries.filter { it.account.includeInTotal }.fold(0L) { total, entry ->
            Math.addExact(total, checkNotNull(entry.account.baseBalance) { "缺少汇率" })
        } }

    override fun getTotalCreditDebt(): Flow<Long> =
        getAccountsStream().map { entries -> entries.filter { it.account.type == cn.turingzy.ledger.model.account.AccountType.CREDIT }.fold(0L) { total, entry ->
            Math.addExact(total, checkNotNull(entry.account.baseBalance) { "缺少汇率" })
        } }

    override suspend fun createAccount(account: Account): Long =
        withContext(ioDispatcher) {
            val accountToSave = if (account.currentBalance == 0L && account.initialBalance != 0L) {
                account.copy(currentBalance = account.initialBalance)
            } else {
                account
            }
            accountDao.insertAccount(accountToSave.asEntity())
        }

    override suspend fun updateAccount(account: Account) {
        withContext(ioDispatcher) {
            accountDao.updateAccount(account.asEntity())
        }
    }

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

    override suspend fun deleteAccount(id: Long) {
        withContext(ioDispatcher) {
            accountDao.softDeleteAccount(id)
        }
    }

    override suspend fun adjustBalance(accountId: Long, newBalance: Long) {
        withContext(ioDispatcher) {
            val account = accountDao.getAccountByIdOnce(accountId) ?: return@withContext
            val delta = newBalance - account.currentBalance
            accountDao.updateBalance(accountId, delta)
        }
    }
}

