package cn.turingzy.ledger.ui.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.turingzy.ledger.data.repository.TransactionRepository
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.model.TransactionDetail
import cn.turingzy.ledger.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import cn.turingzy.ledger.util.Dispatcher
import cn.turingzy.ledger.util.LedgerDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import cn.turingzy.ledger.ui.component.calendar.CalendarSelection
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    @Dispatcher(LedgerDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val period =
        MutableStateFlow(AnalyticsPeriod(AnalyticsTimeScope.MONTH, LocalDate.now()))
    private val categoryType = MutableStateFlow(CategoryType.EXPENSE)

    private val analyticsState = combine(
        period,
        categoryType,
    ) { selectedPeriod, catType ->
        selectedPeriod to catType
    }.flatMapLatest { (selectedPeriod, catType) ->
        val scope = selectedPeriod.scope
        val zone = ZoneId.systemDefault()
        if (scope == AnalyticsTimeScope.ALL) {
            val customRange = selectedPeriod.customSelection
            val queryStart = customRange?.let { it.start.atStartOfDay(zone).toInstant().toEpochMilli() } ?: Long.MIN_VALUE
            val queryEnd = customRange?.let {
                it.end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
            } ?: Long.MAX_VALUE
            return@flatMapLatest transactionRepository.getTransactionsBetween(queryStart, queryEnd).map { entries ->
                val transactions = entries.filterNot { it.transaction.isDeleted }
                val dates = transactions.map { Instant.ofEpochMilli(it.transaction.transactionTime).atZone(zone).toLocalDate() }
                val start = customRange?.start ?: dates.minOrNull() ?: LocalDate.now()
                val end = customRange?.end ?: dates.maxOrNull() ?: start
                val expense = transactions.filter { it.transaction.type == TransactionType.EXPENSE }.sumOf { it.transaction.amount }
                val income = transactions.filter { it.transaction.type == TransactionType.INCOME }.sumOf { it.transaction.amount }
                AnalyticsUiState.Success(
                    timeScope = scope,
                    categoryType = catType,
                    periodDisplay = "$start - $end",
                    totalExpense = expense,
                    totalIncome = income,
                    balance = income - expense,
                    rankings = calculateCategoryRankings(transactions, catType),
                    periodStart = start,
                    periodEnd = end,
                )
            }
        }
        val selection = selectedPeriod.selection
        val startEpoch = selection.start.atStartOfDay(zone).toInstant().toEpochMilli()
        val endEpoch = selection.end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

        val previousRange = previousAnalyticsRange(selection.start, selection.end)
        val previousStart = previousRange.start.atStartOfDay(zone).toInstant().toEpochMilli()
        val previousEnd = previousRange.end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        combine(
            transactionRepository.getAmountSum(TransactionType.EXPENSE, startEpoch, endEpoch),
            transactionRepository.getAmountSum(TransactionType.INCOME, startEpoch, endEpoch),
            transactionRepository.getTransactionsBetween(startEpoch, endEpoch),
            transactionRepository.getAmountSum(TransactionType.EXPENSE, previousStart, previousEnd),
            transactionRepository.getAmountSum(TransactionType.INCOME, previousStart, previousEnd),
        ) { totalExpense, totalIncome, transactions, previousExpense, previousIncome ->
            val rankings = calculateCategoryRankings(transactions, catType)
            AnalyticsUiState.Success(
                timeScope = scope,
                categoryType = catType,
                periodDisplay = selection.analyticsDisplayRange(),
                totalExpense = totalExpense,
                totalIncome = totalIncome,
                balance = totalIncome - totalExpense,
                rankings = rankings,
                periodStart = selection.start,
                periodEnd = selection.end,
                comparison = AnalyticsComparison(previousRange.start, previousRange.end, previousExpense, previousIncome),
            )
        }
    }.flowOn(defaultDispatcher)

    val uiState: StateFlow<AnalyticsUiState> = analyticsState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AnalyticsUiState.Loading,
    )

    fun setTimeScope(scope: AnalyticsTimeScope) {
        if (period.value.scope == scope && period.value.customSelection == null) return
        period.value = AnalyticsPeriod(scope, LocalDate.now())
    }

    fun setCategoryType(type: CategoryType) {
        categoryType.value = type
    }

    fun navigatePeriod(delta: Int) {
        val selected = period.value
        if (selected.scope == AnalyticsTimeScope.ALL) return
        val anchor = when (selected.scope) {
            AnalyticsTimeScope.WEEK -> selected.anchor.plusWeeks(delta.toLong())
            AnalyticsTimeScope.MONTH -> selected.anchor.plusMonths(delta.toLong())
            AnalyticsTimeScope.THREE_MONTHS -> selected.anchor.plusMonths(delta.toLong() * 3)
            AnalyticsTimeScope.YEAR -> selected.anchor.plusYears(delta.toLong())
            AnalyticsTimeScope.ALL -> return
        }
        val custom = selected.customSelection?.let {
            val days = ChronoUnit.DAYS.between(it.start, it.end) + 1
            CalendarSelection(it.start.plusDays(days * delta), it.end.plusDays(days * delta))
        }
        period.value = selected.copy(anchor = anchor, customSelection = custom)
    }

    fun selectPeriod(date: LocalDate) {
        period.value = period.value.copy(anchor = date, customSelection = null)
    }

    fun selectRange(start: LocalDate, end: LocalDate) {
        val selection = CalendarSelection(start, end)
        if (period.value.scope == AnalyticsTimeScope.ALL) {
            period.value = period.value.copy(customSelection = selection)
            return
        }
        val days = ChronoUnit.DAYS.between(start, end) + 1
        val scope = when {
            selection == CalendarSelection.year(start.year) -> AnalyticsTimeScope.YEAR
            selection == CalendarSelection.month(YearMonth.from(start)) -> AnalyticsTimeScope.MONTH
            start.dayOfMonth == 1 && end == start.plusMonths(3).minusDays(1) -> AnalyticsTimeScope.THREE_MONTHS
            days <= 7 -> AnalyticsTimeScope.WEEK
            days <= 31 -> AnalyticsTimeScope.MONTH
            else -> AnalyticsTimeScope.YEAR
        }
        val anchor = if (scope == AnalyticsTimeScope.THREE_MONTHS) end else start
        val selected = AnalyticsPeriod(scope, anchor)
        period.value = selected.copy(customSelection = selection.takeUnless { it == selected.selection })
    }

    private data class AnalyticsPeriod(
        val scope: AnalyticsTimeScope,
        val anchor: LocalDate,
        val customSelection: CalendarSelection? = null,
    ) {
        val selection: CalendarSelection
            get() = customSelection ?: when (scope) {
                AnalyticsTimeScope.WEEK -> CalendarSelection.week(anchor)
                AnalyticsTimeScope.MONTH -> CalendarSelection.month(YearMonth.from(anchor))
                AnalyticsTimeScope.THREE_MONTHS -> CalendarSelection(
                    YearMonth.from(anchor).minusMonths(2).atDay(1),
                    YearMonth.from(anchor).atEndOfMonth(),
                )
                AnalyticsTimeScope.YEAR -> CalendarSelection.year(anchor.year)
                AnalyticsTimeScope.ALL -> CalendarSelection(anchor, anchor)
            }
    }

    private fun calculateCategoryRankings(
        transactions: List<TransactionDetail>,
        targetCatType: CategoryType,
    ): List<CategoryExpenseRanking> {
        val targetTxType = when (targetCatType) {
            CategoryType.EXPENSE -> TransactionType.EXPENSE
            CategoryType.INCOME -> TransactionType.INCOME
            CategoryType.TRANSFER -> TransactionType.TRANSFER
        }

        val filtered =
            transactions.filter { !it.transaction.isDeleted && it.transaction.type == targetTxType }
        val totalAmount = filtered.sumOf { it.transaction.amount }

        if (totalAmount <= 0L) return emptyList()

        return filtered
            .groupBy { it.category.category.id }
            .map { (catId, txList) ->
                val catName = txList.first().category.category.name
                val catTotal = txList.sumOf { it.transaction.amount }
                val percentage = analyticsRatio(catTotal, totalAmount)
                CategoryExpenseRanking(
                    categoryId = catId,
                    categoryName = catName,
                    totalAmount = catTotal,
                    percentage = percentage,
                    transactions = txList.sortedByDescending { it.transaction.transactionTime }
                        .map { analyticsTransaction(it, ZoneId.systemDefault()) },
                )
            }
            .sortedByDescending { it.totalAmount }
    }
}
