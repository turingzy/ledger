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

    private val timeScope = MutableStateFlow(AnalyticsTimeScope.MONTH)
    private val categoryType = MutableStateFlow(CategoryType.EXPENSE)
    private val periodOffset = MutableStateFlow(0) // 偏移量: 0 代表当前周期, -1 代表上一周期, 1 代表下一周期

    val uiState: StateFlow<AnalyticsUiState> = combine(
        timeScope,
        categoryType,
        periodOffset,
    ) { scope, catType, offset ->
        Triple(scope, catType, offset)
    }.flatMapLatest { (scope, catType, offset) ->
        val zone = ZoneId.systemDefault()
        val now = LocalDate.now(zone)

        val (startEpoch, endEpoch, periodDisplay) = calculateTimeRange(scope, offset, now, zone)

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
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AnalyticsUiState.Loading,
    )

    fun setTimeScope(scope: AnalyticsTimeScope) {
        timeScope.value = scope
        periodOffset.value = 0
    }

    fun setCategoryType(type: CategoryType) {
        categoryType.value = type
    }

    fun navigatePeriod(delta: Int) {
        periodOffset.value += delta
    }

    private fun calculateTimeRange(
        scope: AnalyticsTimeScope,
        offset: Int,
        now: LocalDate,
        zone: ZoneId,
    ): Triple<Long, Long, String> {
        return when (scope) {
            AnalyticsTimeScope.WEEK -> {
                val currentMonday = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val targetMonday = currentMonday.plusWeeks(offset.toLong())
                val targetSunday = targetMonday.plusDays(6)

                val start = targetMonday.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = targetSunday.atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
                val display = "${targetMonday.monthValue}.${targetMonday.dayOfMonth} - ${targetSunday.monthValue}.${targetSunday.dayOfMonth}"
                Triple(start, end, display)
            }

            AnalyticsTimeScope.MONTH -> {
                val currentYm = YearMonth.from(now)
                val targetYm = currentYm.plusMonths(offset.toLong())

                val start = targetYm.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val end = targetYm.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
                val display = "${targetYm.year}年${targetYm.monthValue}月"
                Triple(start, end, display)
            }

            AnalyticsTimeScope.YEAR -> {
                val targetYear = now.year + offset
                val start = LocalDate.of(targetYear, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli()
                val end = LocalDate.of(targetYear, 12, 31).atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
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

        val filtered = transactions.filter { it.transaction.type == targetTxType }
        val totalAmount = filtered.sumOf { it.transaction.amount }

        if (totalAmount <= 0L) return emptyList()

        return filtered
            .groupBy { it.category.category.id }
            .map { (catId, txList) ->
                val catName = txList.first().category.category.name
                val catTotal = txList.sumOf { it.transaction.amount }
                val percentage = catTotal.toFloat() / totalAmount.toFloat()
                CategoryExpenseRanking(
                    categoryId = catId,
                    categoryName = catName,
                    totalAmount = catTotal,
                    percentage = percentage,
                )
            }
            .sortedByDescending { it.totalAmount }
    }
}
