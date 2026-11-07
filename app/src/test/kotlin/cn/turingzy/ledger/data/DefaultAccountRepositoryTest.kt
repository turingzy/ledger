package cn.turingzy.ledger.data

import cn.turingzy.ledger.data.repository.DefaultAccountRepository
import cn.turingzy.ledger.database.dao.AccountDao
import cn.turingzy.ledger.database.model.AccountEntity
import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.model.AccountType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultAccountRepositoryTest {

    private val accountDao: AccountDao = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: DefaultAccountRepository

    @Before
    fun setup() {
        repository = DefaultAccountRepository(
            accountDao = accountDao,
            ioDispatcher = testDispatcher,
        )
    }

    @Test
    fun createAccount_withInitialBalanceAndZeroCurrent_setsCurrentToInitial() = runTest(testDispatcher) {
        val account = Account(
            name = "Salary Card",
            type = AccountType.DEBIT,
            initialBalance = 100000L,
            currentBalance = 0L,
        )
        coEvery { accountDao.insertAccount(any()) } returns 1L

        val id = repository.createAccount(account)

        assertEquals(1L, id)
        coVerify {
            accountDao.insertAccount(
                match { it.currentBalance == 100000L && it.initialBalance == 100000L },
            )
        }
    }

    @Test
    fun adjustBalance_computesCorrectDeltaAndUpdates() = runTest(testDispatcher) {
        val existingEntity = AccountEntity(
            accountId = 10L,
            name = "Wallet",
            type = AccountType.WALLET,
            currentBalance = 5000L,
        )
        coEvery { accountDao.getAccountByIdOnce(10L) } returns existingEntity
        coEvery { accountDao.updateBalance(10L, any()) } returns 1

        repository.adjustBalance(10L, 8000L)

        // Delta should be 8000 - 5000 = 3000
        coVerify {
            accountDao.updateBalance(10L, 3000L)
        }
    }

    @Test
    fun deleteAccount_softDeletesInDao() = runTest(testDispatcher) {
        coEvery { accountDao.softDeleteAccount(5L) } returns 1

        repository.deleteAccount(5L)

        coVerify {
            accountDao.softDeleteAccount(5L)
        }
    }
}

