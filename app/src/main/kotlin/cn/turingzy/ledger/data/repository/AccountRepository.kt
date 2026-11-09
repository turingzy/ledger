package cn.turingzy.ledger.data.repository

import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.model.AccountWithTemplate
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAccountsStream(): Flow<List<AccountWithTemplate>>
    fun getAccount(id: Long): Flow<AccountWithTemplate?>
    fun getTotalAsset(): Flow<Long>
    fun getTotalCreditDebt(): Flow<Long>
    suspend fun createAccount(account: Account): Long
    suspend fun updateAccount(account: Account)
    suspend fun updateAccountInfo(account: Account)
    suspend fun deleteAccount(id: Long)
    suspend fun adjustBalance(accountId: Long, newBalance: Long)
}

