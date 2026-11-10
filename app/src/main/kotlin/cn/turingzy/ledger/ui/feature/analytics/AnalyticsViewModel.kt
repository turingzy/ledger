package cn.turingzy.ledger.ui.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.turingzy.ledger.data.repository.AnalyticsCardSettingsRepository
import cn.turingzy.ledger.model.AnalyticsCard
import cn.turingzy.ledger.model.AnalyticsCardSettings
import cn.turingzy.ledger.data.repository.TransactionRepository
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.model.TransactionDetail
import cn.turingzy.ledger.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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
    private val cardSettingsRepository: AnalyticsCardSettingsRepository,
) : ViewModel() {

    private val period =
        MutableStateFlow(AnalyticsPeriod(AnalyticsTimeScope.MONTH, LocalDate.now()))
    private val categoryType = MutableStateFlow(CategoryType.EXPENSE)

    private val cardSettingsError = MutableStateFlow<String?>(null)

    private val analyticsState = combine(
        period,
        categoryType,
    ) { selectedPeriod, catType ->
        selectedPeriod to catType
    }.flatMapLatest { (selectedPeriod, catType) ->
        val scope = selectedPeriod.scope
        val zone = ZoneId.systemDefault()
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
                trendPoints = buildAnalyticsTrend(transactions, scope, selection.start, zone, selection.end),
            )
        }
    }

    val uiState: StateFlow<AnalyticsUiState> = combine<AnalyticsUiState.Success, AnalyticsCardSettings, String?, AnalyticsUiState>(
        analyticsState,
        cardSettingsRepository.settings,
        cardSettingsError,
    ) { state, settings, error ->
        state.copy(cardSettings = settings, cardSettingsError = error)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AnalyticsUiState.Loading,
    )

    fun setCardEnabled(card: AnalyticsCard, enabled: Boolean) {
        updateCardSettings { cardSettingsRepository.setEnabled(card, enabled) }
    }

    fun setCardOrder(order: List<AnalyticsCard>) {
        updateCardSettings { cardSettingsRepository.setOrder(order) }
    }

    fun dismissCardSettingsError() {
        cardSettingsError.value = null
    }

    private fun updateCardSettings(update: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                update()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.e(error, "Unable to save analytics card settings")
                cardSettingsError.value = "卡片设置保存失败，请重试"
            }
        }
    }

    fun setTimeScope(scope: AnalyticsTimeScope) {
        if (period.value.scope == scope && period.value.customSelection == null) return
        period.value = AnalyticsPeriod(scope, LocalDate.now())
    }

    fun setCategoryType(type: CategoryType) {
        categoryType.value = type
    }

    fun navigatePeriod(delta: Int) {
        val selected = period.value
        val anchor = when (selected.scope) {
            AnalyticsTimeScope.WEEK -> selected.anchor.plusWeeks(delta.toLong())
            AnalyticsTimeScope.MONTH -> selected.anchor.plusMonths(delta.toLong())
            AnalyticsTimeScope.THREE_MONTHS -> selected.anchor.plusMonths(delta.toLong() * 3)
            AnalyticsTimeScope.YEAR -> selected.anchor.plusYears(delta.toLong())
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
                        .map { detail ->
                            AnalyticsTransaction(
                                id = detail.transaction.id,
                                date = Instant.ofEpochMilli(detail.transaction.transactionTime)
                                    .atZone(ZoneId.systemDefault()).toLocalDate(),
                                note = detail.transaction.note.orEmpty()
                                    .ifBlank { detail.category.category.name },
                                accountName = detail.account.name,
                                amount = detail.transaction.amount,
                            )
                        },
                )
            }
            .sortedByDescending { it.totalAmount }
    }
}
