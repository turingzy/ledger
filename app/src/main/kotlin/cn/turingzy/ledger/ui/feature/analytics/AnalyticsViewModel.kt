package cn.turingzy.ledger.ui.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.turingzy.ledger.data.repository.TransactionRepository
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.model.TransactionDetail
import cn.turingzy.ledger.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val period =
        MutableStateFlow(AnalyticsPeriod(AnalyticsTimeScope.MONTH, LocalDate.now()))
    private val categoryType = MutableStateFlow(CategoryType.EXPENSE)

    val uiState: StateFlow<AnalyticsUiState> = combine(
        period,
        categoryType,
    ) { selectedPeriod, catType ->
        selectedPeriod to catType
    }.flatMapLatest { (selectedPeriod, catType) ->
        val scope = selectedPeriod.scope
        val zone = ZoneId.systemDefault()
        val (startEpoch, endEpoch, periodDisplay) = calculateTimeRange(
            scope,
            selectedPeriod.anchor,
            zone
        )

        combine(
            transactionRepository.getAmountSum(TransactionType.EXPENSE, startEpoch, endEpoch),
            transactionRepository.getAmountSum(TransactionType.INCOME, startEpoch, endEpoch),
            transactionRepository.getTransactionsBetween(startEpoch, endEpoch),
        ) { totalExpense, totalIncome, transactions ->
            val rankings = calculateCategoryRankings(transactions, catType)
            AnalyticsUiState.Success(
                timeScope = scope,
                categoryType = catType,
                periodDisplay = periodDisplay,
                totalExpense = totalExpense,
                totalIncome = totalIncome,
                balance = totalIncome - totalExpense,
                rankings = rankings,
                periodStart = Instant.ofEpochMilli(startEpoch).atZone(zone).toLocalDate(),
                trendPoints = buildAnalyticsTrend(transactions, scope, selectedPeriod.anchor, zone),
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AnalyticsUiState.Loading,
    )

    fun setTimeScope(scope: AnalyticsTimeScope) {
        if (period.value.scope == scope) return
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
            AnalyticsTimeScope.YEAR -> selected.anchor.plusYears(delta.toLong())
        }
        period.value = selected.copy(anchor = anchor)
    }

    fun selectPeriod(date: LocalDate) {
        period.value = period.value.copy(anchor = date)
    }

    private data class AnalyticsPeriod(val scope: AnalyticsTimeScope, val anchor: LocalDate)

    private fun calculateTimeRange(
        scope: AnalyticsTimeScope,
        anchor: LocalDate,
        zone: ZoneId,
    ): Triple<Long, Long, String> {
        return when (scope) {
            AnalyticsTimeScope.WEEK -> {
                val targetMonday = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val targetSunday = targetMonday.plusDays(6)

                val start = targetMonday.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = targetSunday.atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant()
                    .toEpochMilli()
                val endDisplay = if (targetMonday.year == targetSunday.year) {
                    "${targetSunday.monthValue}.${targetSunday.dayOfMonth}"
                } else {
                    "${targetSunday.year}.${targetSunday.monthValue}.${targetSunday.dayOfMonth}"
                }
                val display =
                    "${targetMonday.year}.${targetMonday.monthValue}.${targetMonday.dayOfMonth} - $endDisplay"
                Triple(start, end, display)
            }

            AnalyticsTimeScope.MONTH -> {
                val targetYm = YearMonth.from(anchor)

                val start = targetYm.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val end =
                    targetYm.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant()
                        .toEpochMilli()
                val display = "${targetYm.year}年${targetYm.monthValue}月"
                Triple(start, end, display)
            }

            AnalyticsTimeScope.YEAR -> {
                val targetYear = anchor.year
                val start =
                    LocalDate.of(targetYear, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli()
                val end =
                    LocalDate.of(targetYear, 12, 31).atTime(23, 59, 59, 999_000_000).atZone(zone)
                        .toInstant().toEpochMilli()
                val display = "${targetYear}年"
                Triple(start, end, display)
            }
        }
    }

    private fun calculateCategoryRankings(
        transactions: List<TransactionDetail>,
        targetCatType: CategoryType,
    ): List<CategoryExpenseRanking> {
        val targetTxType = when (targetCatType) {
            CategoryType.EXPENSE -> TransactionType.EXPENSE
            CategoryType.INCOME -> TransactionType.INCOME
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
