package cn.turingzy.ledger.ui.feature.analytics

import app.cash.turbine.test
import cn.turingzy.ledger.data.repository.AnalyticsCardSettingsRepository
import cn.turingzy.ledger.model.AnalyticsCardSettings
import cn.turingzy.ledger.model.AnalyticsCard
import io.mockk.every
import kotlinx.coroutines.flow.MutableStateFlow
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
import io.mockk.coVerify
import io.mockk.coEvery
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
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {

    private val transactionRepository: TransactionRepository = mockk(relaxed = true)
    private val settings = MutableStateFlow(AnalyticsCardSettings())
    private val cardSettingsRepository: AnalyticsCardSettingsRepository = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: AnalyticsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { cardSettingsRepository.settings } returns settings
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_initialSuccess_calculatesTotalsAndRankings() = runTest {
        val testTx1 = TransactionDetail(
            transaction = Transaction(
                id = 1L,
                type = TransactionType.EXPENSE,
                amount = 6000L,
                accountId = 1L,
                categoryId = 10L,
                transactionTime = System.currentTimeMillis(),
            ),
            account = Account(id = 1L, name = "Card", type = AccountType.DEBIT),
            category = CategoryWithIcon(
                category = Category(
                    id = 10L,
                    name = "Food",
                    type = CategoryType.EXPENSE,
                    iconId = 1L
                ),
                icon = Icon(id = 1L, keyName = "ic_food", displayName = "Food", iconUri = "res://"),
            ),
        )
        val testTx2 = TransactionDetail(
            transaction = Transaction(
                id = 2L,
                type = TransactionType.EXPENSE,
                amount = 4000L,
                accountId = 1L,
                categoryId = 20L,
                transactionTime = System.currentTimeMillis(),
            ),
            account = Account(id = 1L, name = "Card", type = AccountType.DEBIT),
            category = CategoryWithIcon(
                category = Category(
                    id = 20L,
                    name = "Transport",
                    type = CategoryType.EXPENSE,
                    iconId = 2L
                ),
                icon = Icon(
                    id = 2L,
                    keyName = "ic_bus",
                    displayName = "Transport",
                    iconUri = "res://"
                ),
            ),
        )

        coEvery {
            transactionRepository.getAmountSum(
                TransactionType.EXPENSE,
                any(),
                any()
            )
        } returns flowOf(10000L)
        coEvery {
            transactionRepository.getAmountSum(
                TransactionType.INCOME,
                any(),
                any()
            )
        } returns flowOf(25000L)
        coEvery {
            transactionRepository.getTransactionsBetween(
                any(),
                any()
            )
        } returns flowOf(listOf(testTx1, testTx2))

        viewModel = AnalyticsViewModel(transactionRepository, cardSettingsRepository)

        viewModel.uiState.test {
            assertEquals(AnalyticsUiState.Loading, awaitItem())

            testDispatcher.scheduler.advanceUntilIdle()

            val successItem = awaitItem()
            assertTrue(successItem is AnalyticsUiState.Success)
            val success = successItem as AnalyticsUiState.Success

            assertEquals(10000L, success.totalExpense)
            assertEquals(25000L, success.totalIncome)
            assertEquals(15000L, success.balance)
            assertEquals(2, success.rankings.size)

            val topRanking = success.rankings.first()
            assertEquals("Food", topRanking.categoryName)
            assertEquals(6000L, topRanking.totalAmount)
            assertEquals(1, topRanking.transactions.size)
            assertEquals(6000L, topRanking.transactions.single().amount)
            assertEquals("Food", topRanking.transactions.single().note)
            assertEquals("Card", topRanking.transactions.single().accountName)
            assertEquals(10000L, success.trendPoints.sumOf { it.expense })
            assertEquals(0.6f, topRanking.percentage, 0.01f)
        }
    }

    @Test
    fun setTimeScope_updatesScopeAndResetsOffset() = runTest {
        coEvery { transactionRepository.getAmountSum(any(), any(), any()) } returns flowOf(0L)
        coEvery { transactionRepository.getTransactionsBetween(any(), any()) } returns flowOf(
            emptyList()
        )

        viewModel = AnalyticsViewModel(transactionRepository, cardSettingsRepository)

        viewModel.uiState.test {
            assertEquals(AnalyticsUiState.Loading, awaitItem())
            testDispatcher.scheduler.advanceUntilIdle()

            val initialSuccess = awaitItem()
            assertTrue(initialSuccess is AnalyticsUiState.Success)

            viewModel.setTimeScope(AnalyticsTimeScope.YEAR)
            testDispatcher.scheduler.advanceUntilIdle()

            val updatedSuccess = awaitItem()
            assertTrue(updatedSuccess is AnalyticsUiState.Success)
            assertEquals(
                AnalyticsTimeScope.YEAR,
                (updatedSuccess as AnalyticsUiState.Success).timeScope
            )
        }
    }

    @Test
    fun setCategoryType_switchesRankingsTarget() = runTest {
        coEvery { transactionRepository.getAmountSum(any(), any(), any()) } returns flowOf(0L)
        coEvery { transactionRepository.getTransactionsBetween(any(), any()) } returns flowOf(
            emptyList()
        )

        viewModel = AnalyticsViewModel(transactionRepository, cardSettingsRepository)

        viewModel.uiState.test {
            assertEquals(AnalyticsUiState.Loading, awaitItem())
            testDispatcher.scheduler.advanceUntilIdle()

            val initialSuccess = awaitItem()
            assertTrue(initialSuccess is AnalyticsUiState.Success)

            viewModel.setCategoryType(CategoryType.INCOME)
            testDispatcher.scheduler.advanceUntilIdle()

            val updatedSuccess = awaitItem()
            assertTrue(updatedSuccess is AnalyticsUiState.Success)
            assertEquals(
                CategoryType.INCOME,
                (updatedSuccess as AnalyticsUiState.Success).categoryType
            )
        }
    }

    @Test
    fun selectPeriod_queriesFullCalendarPeriodAndNavigatesFromSelection() = runTest {
        coEvery { transactionRepository.getAmountSum(any(), any(), any()) } returns flowOf(0L)
        coEvery { transactionRepository.getTransactionsBetween(any(), any()) } returns flowOf(
            emptyList()
        )
        viewModel = AnalyticsViewModel(transactionRepository, cardSettingsRepository)
        val zone = ZoneId.systemDefault()
        val cases = listOf(
            Triple(AnalyticsTimeScope.WEEK, LocalDate.of(2020, 12, 31), LocalDate.of(2020, 12, 28)),
            Triple(AnalyticsTimeScope.MONTH, LocalDate.of(2024, 2, 29), LocalDate.of(2024, 2, 1)),
            Triple(AnalyticsTimeScope.YEAR, LocalDate.of(2024, 7, 15), LocalDate.of(2024, 1, 1)),
        )

        viewModel.uiState.test {
            awaitItem()
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem()

            cases.forEach { (scope, selectedDate, expectedStart) ->
                viewModel.setTimeScope(scope)
                viewModel.selectPeriod(selectedDate)
                testDispatcher.scheduler.advanceUntilIdle()
                val selected = expectMostRecentItem() as AnalyticsUiState.Success
                assertEquals(scope, selected.timeScope)
                assertEquals(expectedStart, selected.periodStart)
                if (scope == AnalyticsTimeScope.WEEK) {
                    assertEquals("2020 12.28-2021 01.03", selected.periodDisplay)
                }

                val nextStart = when (scope) {
                    AnalyticsTimeScope.WEEK -> expectedStart.plusWeeks(1)
                    AnalyticsTimeScope.MONTH -> expectedStart.plusMonths(1)
                    AnalyticsTimeScope.THREE_MONTHS -> expectedStart.plusMonths(3)
                    AnalyticsTimeScope.YEAR -> expectedStart.plusYears(1)
                }
                val startMillis = expectedStart.atStartOfDay(zone).toInstant().toEpochMilli()
                val endMillis = nextStart.atStartOfDay(zone).toInstant().toEpochMilli() - 1
                coVerify {
                    transactionRepository.getAmountSum(
                        TransactionType.EXPENSE,
                        startMillis,
                        endMillis
                    )
                }
                coVerify {
                    transactionRepository.getAmountSum(
                        TransactionType.INCOME,
                        startMillis,
                        endMillis
                    )
                }
                coVerify { transactionRepository.getTransactionsBetween(startMillis, endMillis) }

                viewModel.navigatePeriod(1)
                testDispatcher.scheduler.advanceUntilIdle()
                assertEquals(nextStart, (awaitItem() as AnalyticsUiState.Success).periodStart)

                viewModel.navigatePeriod(-1)
                testDispatcher.scheduler.advanceUntilIdle()
                assertEquals(expectedStart, (awaitItem() as AnalyticsUiState.Success).periodStart)
            }
        }
    }

    @Test
    fun selectRange_queriesExactBoundariesAndScopeChangeRestoresCalendarPeriod() = runTest {
        coEvery { transactionRepository.getAmountSum(any(), any(), any()) } returns flowOf(0L)
        coEvery { transactionRepository.getTransactionsBetween(any(), any()) } returns flowOf(emptyList())
        viewModel = AnalyticsViewModel(transactionRepository, cardSettingsRepository)
        val start = LocalDate.of(2024, 2, 27)
        val end = LocalDate.of(2024, 3, 4)
        val zone = ZoneId.systemDefault()

        viewModel.uiState.test {
            awaitItem()
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem()

            viewModel.selectRange(start, end)
            testDispatcher.scheduler.advanceUntilIdle()
            val selected = awaitItem() as AnalyticsUiState.Success
            assertEquals(start, selected.periodStart)
            assertEquals(end, selected.periodEnd)
            assertEquals("2024 02.27-03.04", selected.periodDisplay)
            assertEquals(start, selected.trendPoints.first().date)
            assertEquals(end, selected.trendPoints.last().date)
            assertEquals(7, selected.trendPoints.size)
            val startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
            val endMillis = end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
            coVerify { transactionRepository.getTransactionsBetween(startMillis, endMillis) }
            coVerify { transactionRepository.getAmountSum(TransactionType.EXPENSE, startMillis, endMillis) }
            coVerify { transactionRepository.getAmountSum(TransactionType.INCOME, startMillis, endMillis) }

            viewModel.setTimeScope(AnalyticsTimeScope.WEEK)
            testDispatcher.scheduler.advanceUntilIdle()
            val reset = awaitItem() as AnalyticsUiState.Success
            assertEquals(LocalDate.now().with(java.time.DayOfWeek.MONDAY), reset.periodStart)
            assertEquals(reset.periodStart.plusDays(6), reset.periodEnd)
        }
    }

    @Test
    fun confirmedCalendarMonth_navigatesByMonthAcrossLeapDay() = runTest {
        coEvery { transactionRepository.getAmountSum(any(), any(), any()) } returns flowOf(0L)
        coEvery { transactionRepository.getTransactionsBetween(any(), any()) } returns flowOf(emptyList())
        viewModel = AnalyticsViewModel(transactionRepository, cardSettingsRepository)

        viewModel.uiState.test {
            awaitItem()
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem()
            viewModel.selectRange(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31))
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem()
            viewModel.navigatePeriod(1)
            testDispatcher.scheduler.advanceUntilIdle()
            val february = awaitItem() as AnalyticsUiState.Success
            assertEquals(LocalDate.of(2024, 2, 1), february.periodStart)
            assertEquals(LocalDate.of(2024, 2, 29), february.periodEnd)
            viewModel.navigatePeriod(-1)
            testDispatcher.scheduler.advanceUntilIdle()
            val january = awaitItem() as AnalyticsUiState.Success
            assertEquals(LocalDate.of(2024, 1, 1), january.periodStart)
            assertEquals(LocalDate.of(2024, 1, 31), january.periodEnd)
        }
    }

    @Test
    fun threeMonthScope_navigatesWholeMonthsAcrossYearBoundary() = runTest {
        coEvery { transactionRepository.getAmountSum(any(), any(), any()) } returns flowOf(0L)
        coEvery { transactionRepository.getTransactionsBetween(any(), any()) } returns flowOf(emptyList())
        viewModel = AnalyticsViewModel(transactionRepository, cardSettingsRepository)

        viewModel.uiState.test {
            awaitItem()
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem()
            viewModel.selectRange(LocalDate.of(2023, 12, 1), LocalDate.of(2024, 2, 29))
            testDispatcher.scheduler.advanceUntilIdle()
            val selected = awaitItem() as AnalyticsUiState.Success
            assertEquals(AnalyticsTimeScope.THREE_MONTHS, selected.timeScope)
            assertEquals(3, selected.trendPoints.size)
            viewModel.navigatePeriod(1)
            testDispatcher.scheduler.advanceUntilIdle()
            val next = awaitItem() as AnalyticsUiState.Success
            assertEquals(LocalDate.of(2024, 3, 1), next.periodStart)
            assertEquals(LocalDate.of(2024, 5, 31), next.periodEnd)
            viewModel.navigatePeriod(-1)
            testDispatcher.scheduler.advanceUntilIdle()
            val previous = awaitItem() as AnalyticsUiState.Success
            assertEquals(selected.periodStart, previous.periodStart)
            assertEquals(selected.periodEnd, previous.periodEnd)
        }
    }

    @Test
    fun comparisonQueriesPreviousMonthAndUpdatesWithSelectedRange() = runTest {
        val zone = ZoneId.systemDefault()
        val januaryStart = LocalDate.of(2024, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val januaryEnd = LocalDate.of(2024, 2, 1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val februaryStart = LocalDate.of(2024, 2, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val februaryEnd = LocalDate.of(2024, 3, 1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        coEvery { transactionRepository.getAmountSum(any(), any(), any()) } returns flowOf(0L)
        coEvery { transactionRepository.getAmountSum(TransactionType.INCOME, januaryStart, januaryEnd) } returns flowOf(20000L)
        coEvery { transactionRepository.getAmountSum(TransactionType.EXPENSE, januaryStart, januaryEnd) } returns flowOf(12000L)
        coEvery { transactionRepository.getAmountSum(TransactionType.INCOME, februaryStart, februaryEnd) } returns flowOf(30000L)
        coEvery { transactionRepository.getAmountSum(TransactionType.EXPENSE, februaryStart, februaryEnd) } returns flowOf(10000L)
        coEvery { transactionRepository.getTransactionsBetween(any(), any()) } returns flowOf(emptyList())
        viewModel = AnalyticsViewModel(transactionRepository, cardSettingsRepository)
        viewModel.uiState.test {
            awaitItem()
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem()
            viewModel.selectRange(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29))
            testDispatcher.scheduler.advanceUntilIdle()
            val february = awaitItem() as AnalyticsUiState.Success
            val comparison = checkNotNull(february.comparison)
            assertEquals(LocalDate.of(2024, 1, 1), comparison.periodStart)
            assertEquals(LocalDate.of(2024, 1, 31), comparison.periodEnd)
            assertEquals(20000L, comparison.totalIncome)
            assertEquals(12000L, comparison.totalExpense)
            assertEquals(8000L, comparison.balance)
            assertEquals(30000L, february.totalIncome)
            assertEquals(10000L, february.totalExpense)
            viewModel.navigatePeriod(1)
            testDispatcher.scheduler.advanceUntilIdle()
            val march = awaitItem() as AnalyticsUiState.Success
            val previous = checkNotNull(march.comparison)
            assertEquals(LocalDate.of(2024, 2, 1), previous.periodStart)
            assertEquals(LocalDate.of(2024, 2, 29), previous.periodEnd)
            assertEquals(30000L, previous.totalIncome)
            assertEquals(10000L, previous.totalExpense)
        }
    }

    @Test
    fun cardSettingsPersistVisibilityAndOrderWithoutReloadingStatistics() = runTest {
        coEvery { transactionRepository.getAmountSum(any(), any(), any()) } returns flowOf(0L)
        coEvery { transactionRepository.getTransactionsBetween(any(), any()) } returns flowOf(emptyList())
        coEvery { cardSettingsRepository.setEnabled(any(), any()) } coAnswers {
            settings.value = settings.value.withEnabled(firstArg(), secondArg())
        }
        coEvery { cardSettingsRepository.setOrder(any()) } coAnswers {
            settings.value = settings.value.withOrder(firstArg())
        }
        viewModel = AnalyticsViewModel(transactionRepository, cardSettingsRepository)
        viewModel.uiState.test {
            awaitItem()
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem()
            viewModel.setCardEnabled(AnalyticsCard.TREND, false)
            testDispatcher.scheduler.advanceUntilIdle()
            val disabled = awaitItem() as AnalyticsUiState.Success
            assertTrue(AnalyticsCard.TREND !in disabled.cardSettings.enabledCards)
            val order = listOf(AnalyticsCard.CATEGORIES, AnalyticsCard.COMPARISON, AnalyticsCard.TREND)
            viewModel.setCardOrder(order)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(order, (awaitItem() as AnalyticsUiState.Success).cardSettings.order)
            viewModel.setCardEnabled(AnalyticsCard.TREND, true)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(order, (awaitItem() as AnalyticsUiState.Success).cardSettings.enabledCards)
            coVerify(exactly = 1) { transactionRepository.getTransactionsBetween(any(), any()) }
            coVerify { cardSettingsRepository.setOrder(order) }
        }
    }

    @Test
    fun cardSettingsSaveFailureKeepsStoredSettingsAndReportsError() = runTest {
        coEvery { transactionRepository.getAmountSum(any(), any(), any()) } returns flowOf(0L)
        coEvery { transactionRepository.getTransactionsBetween(any(), any()) } returns flowOf(emptyList())
        coEvery { cardSettingsRepository.setEnabled(any(), any()) } throws java.io.IOException("Test write failure")
        viewModel = AnalyticsViewModel(transactionRepository, cardSettingsRepository)
        viewModel.uiState.test {
            awaitItem()
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem()
            viewModel.setCardEnabled(AnalyticsCard.TREND, false)
            testDispatcher.scheduler.advanceUntilIdle()
            val failed = awaitItem() as AnalyticsUiState.Success
            assertTrue(AnalyticsCard.TREND in failed.cardSettings.enabledCards)
            assertEquals("卡片设置保存失败，请重试", failed.cardSettingsError)
            viewModel.dismissCardSettingsError()
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(null, (awaitItem() as AnalyticsUiState.Success).cardSettingsError)
        }
    }

}
