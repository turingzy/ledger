package cn.turingzy.ledger.ui.feature.analytics

import cn.turingzy.ledger.ui.feature.analytics.calculation.analyticsDisplayRange
import cn.turingzy.ledger.ui.feature.analytics.calculation.analyticsRatio
import cn.turingzy.ledger.ui.feature.analytics.calculation.analyticsTransaction
import cn.turingzy.ledger.ui.feature.analytics.calculation.previousAnalyticsRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.turingzy.ledger.data.repository.transaction.TransactionRepository
import cn.turingzy.ledger.data.repository.currency.CurrencyRepository
import cn.turingzy.ledger.model.currency.primaryAmount
import cn.turingzy.ledger.model.category.CategoryType
import cn.turingzy.ledger.model.transaction.TransactionDetail
import cn.turingzy.ledger.model.transaction.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import cn.turingzy.ledger.di.Dispatcher
import cn.turingzy.ledger.di.LedgerDispatchers
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
import cn.turingzy.ledger.ui.component.picker.CalendarSelection
import javax.inject.Inject

/**
 * 统计分析 ViewModel。
 *
 * 负责管理统计时间范围（周/月/季/年/全部）、收支类型（支出/收入）以及币种选择，
 * 并基于数据库中的交易明细计算指定周期及其对比周期的汇总金额、分类排行榜与收支明细。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    @Dispatcher(LedgerDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher,
    private val currencies: CurrencyRepository,
) : ViewModel() {

    private val period =
        MutableStateFlow(AnalyticsPeriod(AnalyticsTimeScope.MONTH, LocalDate.now()))
    private val categoryType = MutableStateFlow(CategoryType.EXPENSE)


    private val analyticsState = combine(
        period,
        categoryType,
        currencies.selection,
    ) { selectedPeriod, catType, currency ->
        Triple(selectedPeriod, catType, currency)
    }.flatMapLatest { (selectedPeriod, catType, currency) ->
        val scope = selectedPeriod.scope
        val zone = ZoneId.systemDefault()
        if (scope == AnalyticsTimeScope.ALL) {
            val customRange = selectedPeriod.customSelection
            val queryStart = customRange?.let { it.start.atStartOfDay(zone).toInstant().toEpochMilli() } ?: Long.MIN_VALUE
            val queryEnd = customRange?.let {
                it.end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
            } ?: Long.MAX_VALUE
            return@flatMapLatest transactionRepository.getTransactionsBetween(queryStart, queryEnd, currency.currency).map { entries ->
                val transactions = entries.filterNot { it.transaction.isDeleted }
                val dates = transactions.map { Instant.ofEpochMilli(it.transaction.transactionTime).atZone(zone).toLocalDate() }
                val start = customRange?.start ?: dates.minOrNull() ?: LocalDate.now()
                val end = customRange?.end ?: dates.maxOrNull() ?: start
                val expense = transactions.filter { it.transaction.type == TransactionType.EXPENSE }.sumOf { it.transaction.baseAmount ?: it.transaction.amount }
                val income = transactions.filter { it.transaction.type == TransactionType.INCOME }.sumOf { it.transaction.baseAmount ?: it.transaction.amount }
                AnalyticsUiState.Success(
                    amountsAvailable = transactions.filter { it.transaction.type != TransactionType.TRANSFER }.all { it.primaryAmount(currency.currency) != null },
                    currency = currency.currency,
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
            transactionRepository.getTransactionsBetween(startEpoch, endEpoch, currency.currency),
            transactionRepository.getTransactionsBetween(previousStart, previousEnd, currency.currency),
        ) { transactions, previous ->
            fun total(entries: List<TransactionDetail>, type: TransactionType) = entries.filter { it.transaction.type == type }
                .fold(0L) { total, entry -> Math.addExact(total, entry.primaryAmount(currency.currency) ?: 0L) }
            val totalExpense = total(transactions, TransactionType.EXPENSE)
            val totalIncome = total(transactions, TransactionType.INCOME)
            val previousExpense = total(previous, TransactionType.EXPENSE)
            val previousIncome = total(previous, TransactionType.INCOME)
            val rankings = calculateCategoryRankings(transactions, catType)
            AnalyticsUiState.Success(
                amountsAvailable = (transactions + previous).filter { it.transaction.type != TransactionType.TRANSFER }.all { it.primaryAmount(currency.currency) != null },
                currency = currency.currency,
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

    /**
     * 统计分析页面的只读 UI 状态流。
     */
    val uiState: StateFlow<AnalyticsUiState> = analyticsState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AnalyticsUiState.Loading,
    )

    /**
     * 切换统计时间粒度（周、月、季、年、全部）。
     *
     * @param scope 目标时间粒度。
     */
    fun setTimeScope(scope: AnalyticsTimeScope) {
        if (period.value.scope == scope && period.value.customSelection == null) return
        period.value = AnalyticsPeriod(scope, LocalDate.now())
    }

    /**
     * 切换分类收支统计类型（支出或收入）。
     *
     * @param type 目标分类类型。
     */
    fun setCategoryType(type: CategoryType) {
        categoryType.value = type
    }

    /**
     * 按当前时间跨度向前或向后移动周期。
     *
     * @param delta 偏移量，正数表示向未来方向移动，负数表示向过去方向移动。
     */
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

    /**
     * 选择指定日期作为统计周期的基准锚点。
     *
     * @param date 选中的基准日期。
     */
    fun selectPeriod(date: LocalDate) {
        period.value = period.value.copy(anchor = date, customSelection = null)
    }

    /**
     * 选择自定义日期范围。
     *
     * @param start 起始日期。
     * @param end 结束日期。
     */
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

    /**
     * 内部记录的统计周期配置。
     *
     * @property scope 时间跨度粒度。
     * @property anchor 周期锚点日期。
     * @property customSelection 显式指定的自定义日历选择范围（若存在）。
     */
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

    /**
     * 计算并生成指定收支类型的分类排行榜。
     *
     * @param transactions 当前周期内的交易明细列表。
     * @param targetCatType 目标分类类型（支出/收入）。
     * @return 按总金额降序排列的分类排行榜列表。
     */
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
        val totalAmount = filtered.sumOf { it.transaction.baseAmount ?: it.transaction.amount }

        if (totalAmount <= 0L) return emptyList()

        return filtered
            .groupBy { it.category.category.id }
            .map { (catId, txList) ->
                val catName = txList.first().category.category.name
                val catTotal = txList.sumOf { it.transaction.baseAmount ?: it.transaction.amount }
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
