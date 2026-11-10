package cn.turingzy.ledger.ui.feature.ledger

import app.cash.turbine.test
import cn.turingzy.ledger.data.repository.AccountRepository
import cn.turingzy.ledger.data.repository.CategoryRepository
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
import java.time.YearMonth
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class LedgerViewModelTest {

    private val transactionRepository: TransactionRepository = mockk(relaxed = true)
    private val categoryRepository: CategoryRepository = mockk(relaxed = true)
    private val accountRepository: AccountRepository = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: LedgerViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_initialSuccess_groupsTransactionsByDay() = runTest {
        val testTx = TransactionDetail(
            transaction = Transaction(
                id = 1L,
                type = TransactionType.EXPENSE,
                amount = 4500L,
                accountId = 1L,
                categoryId = 2L,
                transactionTime = 1700000000000L,
                note = "Lunch",
            ),
            account = Account(id = 1L, name = "WeChat", type = AccountType.WALLET),
            category = CategoryWithIcon(
                category = Category(id = 2L, name = "Dining", type = CategoryType.EXPENSE, iconId = 1L),
                icon = Icon(id = 1L, keyName = "ic_food", displayName = "Dining", iconUri = "res://"),
            ),
        )

        coEvery { categoryRepository.getCategoriesStream() } returns flowOf(emptyList())
        coEvery { accountRepository.getAccountsStream() } returns flowOf(emptyList())
        coEvery { transactionRepository.getAmountSum(TransactionType.INCOME, any(), any()) } returns flowOf(0L)
        coEvery { transactionRepository.getAmountSum(TransactionType.EXPENSE, any(), any()) } returns flowOf(4500L)
        coEvery { transactionRepository.getTransactionsBetween(any(), any()) } returns flowOf(listOf(testTx))

        viewModel = LedgerViewModel(
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository,
            accountRepository = accountRepository,
        )

        viewModel.uiState.test {
            assertEquals(LedgerUiState.Loading, awaitItem())

            testDispatcher.scheduler.advanceUntilIdle()

            val successItem = awaitItem()
            assertTrue(successItem is LedgerUiState.Success)
            val success = successItem as LedgerUiState.Success
            val month = YearMonth.now()
            val zone = ZoneId.systemDefault()
            val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val end = month.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
            assertEquals(DateFilterType.MONTH, success.filterType)
            assertEquals(month.atDay(1), success.filterStartDate)
            assertEquals(month.atEndOfMonth(), success.filterEndDate)
            coVerify { transactionRepository.getTransactionsBetween(start, end) }
            assertEquals(4500L, success.monthExpense)
            assertEquals(-4500L, success.monthBalance)
            assertEquals(1, success.dayGroups.size)
            assertEquals(1, success.dayGroups.first().transactions.size)
        }
    }

    @Test
    fun createTransaction_delegatesToRepository() = runTest {
        coEvery { categoryRepository.getCategoriesStream() } returns flowOf(emptyList())
        coEvery { accountRepository.getAccountsStream() } returns flowOf(emptyList())
        coEvery { transactionRepository.getAmountSum(any(), any(), any()) } returns flowOf(0L)
        coEvery { transactionRepository.getTransactionsBetween(any(), any()) } returns flowOf(emptyList())

        viewModel = LedgerViewModel(
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository,
            accountRepository = accountRepository,
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.createTransaction(
            type = TransactionType.EXPENSE,
            amount = 3500L,
            accountId = 1L,
            categoryId = 2L,
            note = "Coffee",
            transactionTime = 1_700_000_000_000L,
        )

        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            transactionRepository.createTransaction(
                match {
                    it.type == TransactionType.EXPENSE &&
                        it.amount == 3500L &&
                        it.accountId == 1L &&
                        it.categoryId == 2L &&
                        it.note == "Coffee" &&
                        it.transactionTime == 1_700_000_000_000L
                },
            )
        }
    }

    @Test
    fun deleteTransaction_delegatesToRepository() = runTest {
        coEvery { categoryRepository.getCategoriesStream() } returns flowOf(emptyList())
        coEvery { accountRepository.getAccountsStream() } returns flowOf(emptyList())
        coEvery { transactionRepository.getAmountSum(any(), any(), any()) } returns flowOf(0L)
        coEvery { transactionRepository.getTransactionsBetween(any(), any()) } returns flowOf(emptyList())

        viewModel = LedgerViewModel(
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository,
            accountRepository = accountRepository,
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteTransaction(42L)

        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            transactionRepository.deleteTransaction(42L)
        }
    }
}
