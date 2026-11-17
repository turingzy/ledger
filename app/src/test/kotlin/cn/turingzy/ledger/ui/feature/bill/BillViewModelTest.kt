package cn.turingzy.ledger.ui.feature.bill

import cn.turingzy.ledger.data.repository.currency.CurrencyRepository
import cn.turingzy.ledger.data.repository.currency.CurrencySelection
import io.mockk.every
import cn.turingzy.ledger.model.transaction.DateFilterType
import app.cash.turbine.test
import cn.turingzy.ledger.data.repository.account.AccountRepository
import cn.turingzy.ledger.data.repository.category.CategoryRepository
import cn.turingzy.ledger.data.repository.transaction.TransactionRepository
import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.category.Category
import cn.turingzy.ledger.model.category.CategoryType
import cn.turingzy.ledger.model.category.CategoryWithIcon
import cn.turingzy.ledger.model.category.Icon
import cn.turingzy.ledger.model.transaction.Transaction
import cn.turingzy.ledger.model.transaction.TransactionDetail
import cn.turingzy.ledger.model.transaction.TransactionType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.flow
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
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.ZoneId
import kotlin.coroutines.ContinuationInterceptor

@OptIn(ExperimentalCoroutinesApi::class)
class BillViewModelTest {

    private val transactionRepository: TransactionRepository = mockk(relaxed = true)
    private val categoryRepository: CategoryRepository = mockk(relaxed = true)
    private val accountRepository: AccountRepository = mockk(relaxed = true)
    private val currencies: CurrencyRepository = mockk()
    private val currencySelection = kotlinx.coroutines.flow.MutableStateFlow(CurrencySelection())
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: BillViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { currencies.selection } returns currencySelection
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun ledgerDataIsCollectedOnTheInjectedWorkerDispatcher() = runTest {
        val worker = StandardTestDispatcher(testScheduler, name = "ledger-worker")
        var collectionDispatcher: ContinuationInterceptor? = null
        coEvery { categoryRepository.getCategoriesStream() } returns flowOf(emptyList())
        coEvery { accountRepository.getAccountsStream() } returns flowOf(emptyList())
        coEvery { transactionRepository.getAmountSum(any(), any(), any(), "CNY") } returns flowOf(0L)
        coEvery { transactionRepository.getTransactionsBetween(any(), any(), "CNY") } returns flow {
            collectionDispatcher = currentCoroutineContext()[ContinuationInterceptor]
            emit(emptyList())
        }
        viewModel = BillViewModel(transactionRepository, categoryRepository, accountRepository, worker, currencies)

        viewModel.uiState.test {
            assertEquals(BillUiState.Loading, awaitItem())
            testScheduler.advanceUntilIdle()
            assertTrue(awaitItem() is BillUiState.Success)
            assertEquals(worker, collectionDispatcher)
        }
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
                transactionTime = LocalDate.of(2024, 2, 29).atTime(12, 0)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
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
        coEvery { transactionRepository.getAmountSum(TransactionType.INCOME, any(), any(), "CNY") } returns flowOf(0L)
        coEvery { transactionRepository.getAmountSum(TransactionType.EXPENSE, any(), any(), "CNY") } returns flowOf(4500L)
        coEvery { transactionRepository.getTransactionsBetween(any(), any(), "CNY") } returns flowOf(listOf(testTx))

        viewModel = BillViewModel(
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository,
            accountRepository = accountRepository,
            defaultDispatcher = testDispatcher,
            currencies = currencies,
        )

        viewModel.uiState.test {
            assertEquals(BillUiState.Loading, awaitItem())

            testDispatcher.scheduler.advanceUntilIdle()

            val successItem = awaitItem()
            assertTrue(successItem is BillUiState.Success)
            val success = successItem as BillUiState.Success
            val month = YearMonth.now()
            val zone = ZoneId.systemDefault()
            val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val end = month.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
            assertEquals(DateFilterType.MONTH, success.filterType)
            assertEquals(month.atDay(1), success.filterStartDate)
            assertEquals(month.atEndOfMonth(), success.filterEndDate)
            coVerify { transactionRepository.getTransactionsBetween(start, end, "CNY") }
            assertEquals(4500L, success.monthExpense)
            assertEquals(-4500L, success.monthBalance)
            assertEquals(1, success.dayGroups.size)
            assertEquals(1, success.dayGroups.first().transactions.size)
            assertEquals("02/29 周四", success.dayGroups.first().displayDate)
        }
    }

    @Test
    fun createTransaction_delegatesToRepository() = runTest {
        coEvery { categoryRepository.getCategoriesStream() } returns flowOf(emptyList())
        coEvery { accountRepository.getAccountsStream() } returns flowOf(emptyList())
        coEvery { transactionRepository.getAmountSum(any(), any(), any(), "CNY") } returns flowOf(0L)
        coEvery { transactionRepository.getTransactionsBetween(any(), any(), "CNY") } returns flowOf(emptyList())

        viewModel = BillViewModel(
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository,
            accountRepository = accountRepository,
            defaultDispatcher = testDispatcher,
            currencies = currencies,
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
        coEvery { transactionRepository.getAmountSum(any(), any(), any(), "CNY") } returns flowOf(0L)
        coEvery { transactionRepository.getTransactionsBetween(any(), any(), "CNY") } returns flowOf(emptyList())

        viewModel = BillViewModel(
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository,
            accountRepository = accountRepository,
            defaultDispatcher = testDispatcher,
            currencies = currencies,
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteTransaction(42L)

        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            transactionRepository.deleteTransaction(42L)
        }
    }
    @Test
    fun periodSelectionChangesViewAtomicallyAndQueriesCompletePeriods() = runTest {
        coEvery { categoryRepository.getCategoriesStream() } returns flowOf(emptyList())
        coEvery { accountRepository.getAccountsStream() } returns flowOf(emptyList())
        coEvery { transactionRepository.getAmountSum(any(), any(), any(), "CNY") } returns flowOf(0L)
        coEvery { transactionRepository.getTransactionsBetween(any(), any(), "CNY") } returns flowOf(emptyList())
        viewModel = BillViewModel(transactionRepository, categoryRepository, accountRepository, testDispatcher, currencies)
        val cases = listOf(
            Triple(DateFilterType.WEEK, LocalDate.of(2021, 1, 1), LocalDate.of(2020, 12, 28) to LocalDate.of(2021, 1, 3)),
            Triple(DateFilterType.MONTH, LocalDate.of(2024, 2, 29), LocalDate.of(2024, 2, 1) to LocalDate.of(2024, 2, 29)),
            Triple(DateFilterType.YEAR, LocalDate.of(2024, 2, 29), LocalDate.of(2024, 1, 1) to LocalDate.of(2024, 12, 31)),
        )
        viewModel.uiState.test {
            awaitItem()
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem()
            cases.forEach { (type, date, range) ->
                viewModel.selectPeriod(type, date)
                testDispatcher.scheduler.advanceUntilIdle()
                val selected = expectMostRecentItem() as BillUiState.Success
                assertEquals(type, selected.filterType)
                assertEquals(range.first, selected.filterStartDate)
                assertEquals(range.second, selected.filterEndDate)
                val zone = ZoneId.systemDefault()
                val startMillis = range.first.atStartOfDay(zone).toInstant().toEpochMilli()
                val endMillis = range.second.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                coVerify { transactionRepository.getTransactionsBetween(startMillis, endMillis, "CNY") }
                coVerify { transactionRepository.getTransactionsBetween(startMillis, endMillis, "CNY") }
                coVerify { transactionRepository.getTransactionsBetween(startMillis, endMillis, "CNY") }

                val today = LocalDate.now()
                viewModel.selectPeriod(type, today)
                testDispatcher.scheduler.advanceUntilIdle()
                val current = expectMostRecentItem() as BillUiState.Success
                val expectedStart = when (type) {
                    DateFilterType.WEEK -> today.with(DayOfWeek.MONDAY)
                    DateFilterType.MONTH -> today.withDayOfMonth(1)
                    DateFilterType.YEAR -> today.withDayOfYear(1)
                    DateFilterType.RANGE -> today
                }
                assertEquals(type, current.filterType)
                assertEquals(expectedStart, current.filterStartDate)
            }
        }
    }

    @Test
    fun globalSearchIgnoresTheSelectedPeriodAndCurrencyAndClosingRestoresThem() = runTest {
        coEvery { categoryRepository.getCategoriesStream() } returns flowOf(emptyList())
        coEvery { accountRepository.getAccountsStream() } returns flowOf(emptyList())
        coEvery { transactionRepository.getAmountSum(any(), any(), any(), "CNY") } returns flowOf(0L)
        coEvery { transactionRepository.getTransactionsBetween(any(), any(), "CNY") } returns flowOf(emptyList())
        val historical = TransactionDetail(
            Transaction(id = 10, type = TransactionType.EXPENSE, amount = 1000L, accountId = 2,
                categoryId = 1, transactionTime = 0L, note = "旧账"),
            Account(id = 2, name = "美元账户", type = AccountType.CASH, currency = "USD"),
            category = CategoryWithIcon(Category(id = 1, name = "餐饮", type = CategoryType.EXPENSE, iconId = 1),
                Icon(id = 1, keyName = "food", displayName = "餐饮", iconUri = "res://")),
        )
        coEvery { transactionRepository.searchTransactions("旧账") } returns flowOf(listOf(historical))
        viewModel = BillViewModel(transactionRepository, categoryRepository, accountRepository, testDispatcher, currencies)
        viewModel.uiState.test {
            awaitItem()
            testDispatcher.scheduler.advanceUntilIdle()
            val before = awaitItem() as BillUiState.Success
            viewModel.search("  旧账  ")
            testDispatcher.scheduler.advanceUntilIdle()
            val result = awaitItem() as BillUiState.Success
            assertEquals(10L, result.dayGroups.single().transactions.single().transaction.id)
            assertEquals("USD", result.dayGroups.single().transactions.single().account.currency)
            assertEquals(0L, result.dayGroups.single().totalExpense)
            viewModel.search("")
            testDispatcher.scheduler.advanceUntilIdle()
            val restored = awaitItem() as BillUiState.Success
            assertEquals(before, restored)
        }
    }

    @Test
    fun failedSaveCanBeRetriedAndRapidClicksCannotCreateDuplicates() = runTest {
        coEvery { categoryRepository.getCategoriesStream() } returns flowOf(emptyList())
        coEvery { accountRepository.getAccountsStream() } returns flowOf(emptyList())
        coEvery { transactionRepository.createTransaction(any()) } throws IllegalStateException("disk error")
        viewModel = BillViewModel(transactionRepository, categoryRepository, accountRepository, testDispatcher, currencies)
        fun save() = viewModel.createTransaction(TransactionType.EXPENSE, 100L, 1L, categoryId = 1L)
        save()
        save()
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 1) { transactionRepository.createTransaction(any()) }
        assertTrue(viewModel.saveState.value.error != null)
        assertTrue(!viewModel.saveState.value.saved)
        coEvery { transactionRepository.createTransaction(any()) } returns 1L
        save()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.saveState.value.saved)
        save()
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 2) { transactionRepository.createTransaction(any()) }
    }

    @Test
    fun ledgerTotalsAndRowsUseTheSelectedCurrency() = runTest {
        coEvery { categoryRepository.getCategoriesStream() } returns flowOf(emptyList())
        coEvery { accountRepository.getAccountsStream() } returns flowOf(emptyList())
        coEvery { transactionRepository.getAmountSum(any(), any(), any(), any()) } returns flowOf(0L)
        coEvery { transactionRepository.getTransactionsBetween(any(), any(), any()) } returns flowOf(emptyList())
        viewModel = BillViewModel(transactionRepository, categoryRepository, accountRepository, testDispatcher, currencies)
        viewModel.uiState.test {
            awaitItem()
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem()
            currencySelection.value = CurrencySelection("USD")
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals("USD", (awaitItem() as BillUiState.Success).currency)
            coVerify { transactionRepository.getTransactionsBetween(any(), any(), "USD") }
            coVerify { transactionRepository.getTransactionsBetween(any(), any(), "USD") }
            coVerify { transactionRepository.getTransactionsBetween(any(), any(), "USD") }
        }
    }

}
