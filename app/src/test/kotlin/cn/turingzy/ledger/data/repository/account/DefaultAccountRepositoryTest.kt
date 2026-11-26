package cn.turingzy.ledger.data.repository.account

import cn.turingzy.ledger.data.database.dao.AccountDao
import cn.turingzy.ledger.data.database.entity.AccountEntity
import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.account.BankCardDetails
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
    private val cardCipher: BankCardDetailsCipher = mockk()
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: DefaultAccountRepository

    @Before
    fun setup() {
        repository = DefaultAccountRepository(
            accountDao = accountDao,
            cardCipher = cardCipher,
            ioDispatcher = testDispatcher,
            preferences = mockk(relaxed = true), valuation = mockk(relaxed = true), rates = mockk(relaxed = true),
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
    @Test
    fun updateAccountInfoOnlyUsesTheMetadataQuery() = runTest(testDispatcher) {
        val edited = Account(
            id = 10L,
            name = "工资卡",
            type = AccountType.DEBIT,
            initialBalance = 12000L,
            currentBalance = 99999L,
            currency = "USD",
            templateId = 42L,
            cardNumber = "1234",
            includeInTotal = false,
        )
        repository.updateAccountInfo(edited)

        coVerify(exactly = 1) {
            accountDao.updateAccountInfo(10L, "工资卡", AccountType.DEBIT, 12000L, 42L, "1234", false, null)
        }
        coVerify(exactly = 0) { accountDao.updateAccount(any()) }
        coVerify(exactly = 0) { accountDao.updateBalance(any(), any()) }
    }

    @Test
    fun createAccountWritesCardDetailsAsEncryptedData() = runTest(testDispatcher) {
        val details = BankCardDetails("4111111111111111", "08/29", "123")
        every { cardCipher.encrypt(details) } returns "ciphertext"
        repository.createAccount(Account(name = "银行卡", type = AccountType.DEBIT, cardNumber = "1111", cardDetails = details))
        coVerify { accountDao.insertAccount(match { it.cardNumber == "1111" && it.encryptedCardDetails == "ciphertext" }) }
    }

}

