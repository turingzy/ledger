package cn.turingzy.ledger.data.repository

import cn.turingzy.ledger.database.dao.AccountDao
import cn.turingzy.ledger.database.mapper.asEntity
import cn.turingzy.ledger.database.mapper.asExternalModel
import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.model.AccountWithTemplate
import cn.turingzy.ledger.util.Dispatcher
import cn.turingzy.ledger.util.LedgerDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DefaultAccountRepository @Inject constructor(
    private val accountDao: AccountDao,
    @param:Dispatcher(LedgerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : AccountRepository {

    override fun getAccountsStream(): Flow<List<AccountWithTemplate>> =
        accountDao.getPopulatedAccounts()
            .map { list -> list.map { it.asExternalModel() } }
            .flowOn(ioDispatcher)

    override fun getAccount(id: Long): Flow<AccountWithTemplate?> =
        accountDao.getPopulatedAccountById(id)
            .map { it?.asExternalModel() }
            .flowOn(ioDispatcher)

    override fun getTotalAsset(): Flow<Long> =
        accountDao.getTotalAsset()
            .flowOn(ioDispatcher)

    override fun getTotalCreditDebt(): Flow<Long> =
        accountDao.getTotalCreditDebt()
            .flowOn(ioDispatcher)

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

