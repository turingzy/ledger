package cn.turingzy.ledger.ui.feature.ledger

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import cn.turingzy.ledger.data.repository.AccountRepository
import cn.turingzy.ledger.data.repository.CategoryRepository
import cn.turingzy.ledger.model.AccountWithTemplate
import cn.turingzy.ledger.data.repository.TransactionRepository
import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.model.AccountType
import cn.turingzy.ledger.model.Category
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.model.CategoryWithIcon
import cn.turingzy.ledger.model.Icon
import cn.turingzy.ledger.model.Transaction
import cn.turingzy.ledger.model.TransactionDetail
import cn.turingzy.ledger.model.TransactionType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionDetailsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<TransactionRepository>()
    private val categoryRepository = mockk<CategoryRepository>()
    private val accountRepository = mockk<AccountRepository>()
    private val detail = TransactionDetail(
        transaction = Transaction(
            id = 42L, type = TransactionType.TRANSFER, amount = 12345L,
            accountId = 1L, toAccountId = 2L, categoryId = 1L, transactionTime = 0L, note = "账户转账",
        ),
        account = Account(id = 1L, name = "转出账户", type = AccountType.DEBIT),
        toAccount = Account(id = 2L, name = "转入账户", type = AccountType.WALLET),
        category = CategoryWithIcon(
            Category(id = 1L, name = "转账", type = CategoryType.TRANSFER, iconId = 1L),
            Icon(id = 1L, keyName = "transfer", displayName = "转账", iconUri = "res://"),
        ),
    )

    private val accounts get() = listOfNotNull(detail.account, detail.toAccount).map { AccountWithTemplate(it) }
    private val categories get() = listOf(detail.category)

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { categoryRepository.getCategoriesStream() } returns flowOf(categories)
        every { accountRepository.getAccountsStream() } returns flowOf(accounts)
    }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun selectedIdLoadsExactTransactionAndReflectsDeletion() = runTest(dispatcher) {
        val transactions = MutableStateFlow<TransactionDetail?>(detail)
        every { repository.getTransaction(42L) } returns transactions
        val viewModel = TransactionDetailsViewModel(SavedStateHandle(mapOf("editingTransactionId" to 42L)), repository, categoryRepository, accountRepository)
        viewModel.uiState.test {
            assertEquals(TransactionDetailsUiState.Loading, awaitItem())
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(TransactionDetailsUiState.Success(detail, categories, accounts), awaitItem())
            transactions.value = detail.copy(transaction = detail.transaction.copy(isDeleted = true))
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(TransactionDetailsUiState.Unavailable(42L), awaitItem())
            transactions.value = null
            dispatcher.scheduler.advanceUntilIdle()
            expectNoEvents()
        }
    }

    @Test
    fun repositoryFailureShowsErrorInsteadOfEmptyDetails() = runTest(dispatcher) {
        every { repository.getTransaction(42L) } returns flow { throw java.io.IOException("Read failure") }
        val viewModel = TransactionDetailsViewModel(SavedStateHandle(mapOf("editingTransactionId" to 42L)), repository, categoryRepository, accountRepository)
        viewModel.uiState.test {
            assertEquals(TransactionDetailsUiState.Loading, awaitItem())
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(TransactionDetailsUiState.Error(42L), awaitItem())
        }
    }
    @Test
    fun savingUpdatesOriginalIdAndIgnoresRepeatedConfirm() = runTest(dispatcher) {
        every { repository.getTransaction(42L) } returns flowOf(detail)
        coEvery { repository.updateTransaction(any()) } returns Unit
        val viewModel = TransactionDetailsViewModel(
            SavedStateHandle(mapOf("editingTransactionId" to 42L)), repository, categoryRepository, accountRepository,
        )
        viewModel.uiState.test {
            awaitItem()
            dispatcher.scheduler.advanceUntilIdle()
            awaitItem()
            val edited = detail.transaction.copy(id = 0L, amount = 67890L, note = "修改后的备注")
            viewModel.save(edited)
            assertEquals(true, viewModel.saveState.value.saving)
            viewModel.save(edited)
            dispatcher.scheduler.advanceUntilIdle()
            coVerify(exactly = 1) { repository.updateTransaction(edited.copy(id = 42L)) }
            coVerify(exactly = 0) { repository.createTransaction(any()) }
            assertEquals(TransactionSaveState(saved = true), viewModel.saveState.value)
        }
    }

    @Test
    fun saveFailureKeepsDetailsAndAllowsRetry() = runTest(dispatcher) {
        every { repository.getTransaction(42L) } returns flowOf(detail)
        coEvery { repository.updateTransaction(any()) } throws java.io.IOException("Write failure")
        val viewModel = TransactionDetailsViewModel(
            SavedStateHandle(mapOf("editingTransactionId" to 42L)), repository, categoryRepository, accountRepository,
        )
        viewModel.uiState.test {
            awaitItem()
            dispatcher.scheduler.advanceUntilIdle()
            val original = awaitItem()
            viewModel.save(detail.transaction)
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(TransactionSaveState(error = "保存失败，请重试"), viewModel.saveState.value)
            assertEquals(original, viewModel.uiState.value)
            coEvery { repository.updateTransaction(any()) } returns Unit
            viewModel.save(detail.transaction)
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(TransactionSaveState(saved = true), viewModel.saveState.value)
            coVerify(exactly = 2) { repository.updateTransaction(detail.transaction) }
        }
    }

    @Test
    fun failedLoadDoesNotPreventOpeningAnotherTransaction() = runTest(dispatcher) {
        every { repository.getTransaction(42L) } returns flow { throw java.io.IOException("Read failure") }
        val next = detail.copy(transaction = detail.transaction.copy(id = 43L, note = "另一笔交易"))
        every { repository.getTransaction(43L) } returns flowOf(next)
        val viewModel = TransactionDetailsViewModel(
            SavedStateHandle(), repository, categoryRepository, accountRepository,
        )
        viewModel.uiState.test {
            assertEquals(TransactionDetailsUiState.Loading, awaitItem())
            viewModel.selectTransaction(42L)
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(TransactionDetailsUiState.Error(42L), expectMostRecentItem())
            viewModel.selectTransaction(43L)
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(TransactionDetailsUiState.Success(next, categories, accounts), expectMostRecentItem())
        }
    }

    @Test
    fun closingAndReopeningClearsSaveStateAndUsesNewSelection() = runTest(dispatcher) {
        every { repository.getTransaction(42L) } returns flowOf(detail)
        val next = detail.copy(transaction = detail.transaction.copy(id = 43L, amount = 98765L))
        every { repository.getTransaction(43L) } returns flowOf(next)
        coEvery { repository.updateTransaction(any()) } returns Unit
        val viewModel = TransactionDetailsViewModel(
            SavedStateHandle(), repository, categoryRepository, accountRepository,
        )
        viewModel.uiState.test {
            awaitItem()
            viewModel.selectTransaction(42L)
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(TransactionDetailsUiState.Success(detail, categories, accounts), expectMostRecentItem())
            viewModel.save(detail.transaction)
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(true, viewModel.saveState.value.saved)
            viewModel.selectTransaction(null)
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(null, viewModel.selectedTransactionId.value)
            assertEquals(TransactionSaveState(), viewModel.saveState.value)
            assertEquals(TransactionDetailsUiState.Loading, expectMostRecentItem())
            viewModel.selectTransaction(43L)
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(TransactionDetailsUiState.Success(next, categories, accounts), expectMostRecentItem())
            viewModel.save(next.transaction)
            dispatcher.scheduler.advanceUntilIdle()
            coVerify(exactly = 1) { repository.updateTransaction(next.transaction) }
        }
    }

    @Test
    fun changingSelectionStopsUpdatesFromPreviousTransaction() = runTest(dispatcher) {
        val first = MutableStateFlow<TransactionDetail?>(detail)
        val next = detail.copy(transaction = detail.transaction.copy(id = 43L))
        every { repository.getTransaction(42L) } returns first
        every { repository.getTransaction(43L) } returns flowOf(next)
        val viewModel = TransactionDetailsViewModel(
            SavedStateHandle(), repository, categoryRepository, accountRepository,
        )
        viewModel.uiState.test {
            awaitItem()
            viewModel.selectTransaction(42L)
            dispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem()
            viewModel.selectTransaction(43L)
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(TransactionDetailsUiState.Success(next, categories, accounts), expectMostRecentItem())
            first.value = null
            dispatcher.scheduler.advanceUntilIdle()
            expectNoEvents()
            assertEquals(43L, viewModel.selectedTransactionId.value)
        }
    }

}
