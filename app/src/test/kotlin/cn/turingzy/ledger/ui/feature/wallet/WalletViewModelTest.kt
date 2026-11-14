package cn.turingzy.ledger.ui.feature.wallet

import app.cash.turbine.test
import cn.turingzy.ledger.data.repository.CardTemplateRepository
import cn.turingzy.ledger.model.CardTemplate
import cn.turingzy.ledger.data.repository.AccountRepository
import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.model.AccountType
import cn.turingzy.ledger.model.AccountWithTemplate
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WalletViewModelTest {

    private val templateRepository: CardTemplateRepository = mockk()
    private val accountRepository: AccountRepository = mockk(relaxed = true)
    private val transactionRepository: cn.turingzy.ledger.data.repository.TransactionRepository = mockk()
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: WalletViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { transactionRepository.getTransactionsBetween(any(), any()) } returns flowOf(emptyList())
        coEvery { templateRepository.getTemplatesStream() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_initialSuccess_combinesRepositoryFlows() = runTest {
        val testAccounts = listOf(
            AccountWithTemplate(
                account = Account(
                    id = 1L,
                    name = "Cash",
                    type = AccountType.CASH,
                    currentBalance = 50000L,
                ),
            ),
        )

        val templates = listOf(CardTemplate(id = 42L, name = "自定义卡面", coverUri = "file:///card.png"))
        coEvery { templateRepository.getTemplatesStream() } returns flowOf(templates)
        coEvery { accountRepository.getTotalAsset() } returns flowOf(50000L)
        coEvery { accountRepository.getTotalCreditDebt() } returns flowOf(0L)
        coEvery { accountRepository.getAccountsStream() } returns flowOf(testAccounts)

        viewModel = WalletViewModel(accountRepository, templateRepository, testDispatcher, transactionRepository)

        viewModel.uiState.test {
            // First emission is Loading
            assertEquals(WalletUiState.Loading, awaitItem())

            testDispatcher.scheduler.advanceUntilIdle()

            val successItem = awaitItem()
            assertTrue(successItem is WalletUiState.Success)
            val success = successItem as WalletUiState.Success
            assertEquals(50000L, success.totalAsset)
            assertEquals(0L, success.totalDebt)
            assertEquals(1, success.accounts.size)
            assertEquals("Cash", success.accounts.first().account.name)
            assertEquals(templates, success.templates)
        }
    }

    @Test
    fun createAccount_delegatesToRepository() = runTest {
        coEvery { accountRepository.getTotalAsset() } returns flowOf(0L)
        coEvery { accountRepository.getTotalCreditDebt() } returns flowOf(0L)
        coEvery { accountRepository.getAccountsStream() } returns flowOf(emptyList())

        viewModel = WalletViewModel(accountRepository, templateRepository, testDispatcher, transactionRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.createAccount(
            name = "Debit Card",
            type = AccountType.DEBIT,
            initialBalance = 100000L,
            cardNumber = "1234",
            includeInTotal = false,
            currency = "USD",
            templateId = 42L,
        )

        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            accountRepository.createAccount(
                match {
                    it.name == "Debit Card" &&
                        it.type == AccountType.DEBIT &&
                        it.initialBalance == 100000L &&
                        it.currentBalance == 100000L &&
                        it.cardNumber == "1234" &&
                        !it.includeInTotal &&
                        it.currency == "USD" &&
                        it.templateId == 42L
                },
            )
        }
    }

    @Test
    fun adjustBalance_delegatesToRepository() = runTest {
        coEvery { accountRepository.getTotalAsset() } returns flowOf(0L)
        coEvery { accountRepository.getTotalCreditDebt() } returns flowOf(0L)
        coEvery { accountRepository.getAccountsStream() } returns flowOf(emptyList())

        viewModel = WalletViewModel(accountRepository, templateRepository, testDispatcher, transactionRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.adjustBalance(accountId = 5L, newBalance = 88000L)

        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            accountRepository.adjustBalance(5L, 88000L)
        }
    }

    @Test
    fun deleteAccount_delegatesToRepository() = runTest {
        coEvery { accountRepository.getTotalAsset() } returns flowOf(0L)
        coEvery { accountRepository.getTotalCreditDebt() } returns flowOf(0L)
        coEvery { accountRepository.getAccountsStream() } returns flowOf(emptyList())

        viewModel = WalletViewModel(accountRepository, templateRepository, testDispatcher, transactionRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteAccount(accountId = 10L)

        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            accountRepository.deleteAccount(10L)
        }
    }
    @Test
    fun updateAccountInfoUsesMetadataSaveInsteadOfBalanceAdjustment() = runTest {
        coEvery { accountRepository.getTotalAsset() } returns flowOf(0L)
        coEvery { accountRepository.getTotalCreditDebt() } returns flowOf(0L)
        coEvery { accountRepository.getAccountsStream() } returns flowOf(emptyList())
        viewModel = WalletViewModel(accountRepository, templateRepository, testDispatcher, transactionRepository)
        val edited = Account(id = 10L, name = "新名称", type = AccountType.DEBIT, currentBalance = 500L)

        viewModel.updateAccountInfo(edited)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { accountRepository.updateAccountInfo(edited) }
        coVerify(exactly = 0) { accountRepository.adjustBalance(any(), any()) }
    }

}
