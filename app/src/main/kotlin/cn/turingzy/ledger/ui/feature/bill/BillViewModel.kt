package cn.turingzy.ledger.ui.feature.bill

import cn.turingzy.ledger.ui.feature.bill.detail.TransactionSaveState
import cn.turingzy.ledger.ui.feature.bill.search.BillSearchRequest
import cn.turingzy.ledger.ui.feature.bill.search.buildBillSearchResults
import cn.turingzy.ledger.ui.component.picker.CalendarSelection
import cn.turingzy.ledger.model.currency.primaryAmount
import cn.turingzy.ledger.model.currency.CurrencyAmountOverride
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import cn.turingzy.ledger.model.transaction.DateFilterType
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.turingzy.ledger.data.repository.account.AccountRepository
import cn.turingzy.ledger.data.repository.category.CategoryRepository
import cn.turingzy.ledger.data.repository.transaction.TransactionRepository
import cn.turingzy.ledger.model.account.AccountWithTemplate
import cn.turingzy.ledger.model.category.CategoryWithIcon
import cn.turingzy.ledger.model.transaction.Transaction
import cn.turingzy.ledger.model.transaction.TransactionBatchChange
import cn.turingzy.ledger.model.transaction.TransactionType
import cn.turingzy.ledger.di.Dispatcher
import cn.turingzy.ledger.di.LedgerDispatchers
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineDispatcher
import cn.turingzy.ledger.data.repository.currency.CurrencyRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

/**
 * 账单日期过滤条件定义。
 */
sealed interface BillDateFilter {
    /** 按周筛选，[anchorDate] 为该周内的任意基准日期。 */
    data class Week(val anchorDate: LocalDate) : BillDateFilter
    /** 按月份筛选，[yearMonth] 为目标年月。 */
    data class Month(val yearMonth: YearMonth) : BillDateFilter
    /** 按年份筛选，[year] 为目标年份。 */
    data class Year(val year: Int) : BillDateFilter
    /** 按起止日期闭区间筛选。 */
    data class Range(val startDate: LocalDate, val endDate: LocalDate) : BillDateFilter
}

private data class FilterCalcResult(
    val startEpoch: Long,
    val endEpoch: Long,
    val filterType: DateFilterType,
    val headerDisplay: String,
    val cardDisplay: String,
    val year: Int,
    val month: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
)

/**
 * 账单功能的主业务 ViewModel。
 *
 * 负责维护当前选中的统计周期、加载并聚合周期内的交易明细与收支总结、
 * 处理快捷记账、批量操作、快速搜索以及分类使用建议。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BillViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    accountRepository: AccountRepository,
    @param:Dispatcher(LedgerDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher,
    private val currencies: CurrencyRepository,
) : ViewModel() {

    private val dateFilter = MutableStateFlow<BillDateFilter>(BillDateFilter.Month(YearMonth.now()))

    /** 当前所有分类列表（带图标）的状态流。 */
    val categories: StateFlow<List<CategoryWithIcon>> = categoryRepository.getCategoriesStream()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** 当前所有账户列表（带模板外观）的状态流。 */
    val accounts: StateFlow<List<AccountWithTemplate>> = accountRepository.getAccountsStream()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _saveState = MutableStateFlow(TransactionSaveState())
    /** 快捷记账的保存状态流。 */
    internal val saveState = _saveState.asStateFlow()
    /** 重置快捷记账保存状态。 */
    internal fun resetSaveState() { if (!_saveState.value.saving) _saveState.value = TransactionSaveState() }

    /** 账单主页面的 UI 状态流，根据日期过滤与主货币动态计算收支、结余与按天分组。 */
    val uiState: StateFlow<BillUiState> = combine(dateFilter, currencies.selection, ::Pair).flatMapLatest { (filter, currency) ->
        val zone = ZoneId.systemDefault()
        val calc = when (filter) {
            is BillDateFilter.Week -> {
                val week = CalendarSelection.week(filter.anchorDate)
                val (startOfWeek, endOfWeek) = week
                val s = startOfWeek.atStartOfDay(zone).toInstant().toEpochMilli()
                val e = endOfWeek.atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
                val header = week.displayRange()
                val cardLabel = "结余"
                FilterCalcResult(s, e, DateFilterType.WEEK, header, cardLabel, filter.anchorDate.year, filter.anchorDate.monthValue, startOfWeek, endOfWeek)
            }
            is BillDateFilter.Month -> {
                val s = filter.yearMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val e = filter.yearMonth.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
                val header = "${filter.yearMonth.year}.${filter.yearMonth.monthValue.toString().padStart(2, '0')}"
                val cardLabel = "结余"
                FilterCalcResult(s, e, DateFilterType.MONTH, header, cardLabel, filter.yearMonth.year, filter.yearMonth.monthValue, filter.yearMonth.atDay(1), filter.yearMonth.atEndOfMonth())
            }
            is BillDateFilter.Year -> {
                val startOfYear = LocalDate.of(filter.year, 1, 1)
                val endOfYear = LocalDate.of(filter.year, 12, 31)
                val s = startOfYear.atStartOfDay(zone).toInstant().toEpochMilli()
                val e = endOfYear.atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
                val header = "${filter.year}"
                val cardLabel = "结余"
                FilterCalcResult(s, e, DateFilterType.YEAR, header, cardLabel, filter.year, 1, startOfYear, endOfYear)
            }
            is BillDateFilter.Range -> {
                val s = filter.startDate.atStartOfDay(zone).toInstant().toEpochMilli()
                val e = filter.endDate.atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
                val header = "${filter.startDate.year}.${filter.startDate.monthValue.toString().padStart(2, '0')}"
                val cardLabel = "结余"
                FilterCalcResult(s, e, DateFilterType.RANGE, header, cardLabel, filter.startDate.year, filter.startDate.monthValue, filter.startDate, filter.endDate)
            }
        }

        transactionRepository.getTransactionsBetween(calc.startEpoch, calc.endEpoch, currency.currency).map { periodTransactions ->
            val available = periodTransactions.filter { it.transaction.type != TransactionType.TRANSFER }.all { it.primaryAmount(currency.currency) != null }
            fun total(type: TransactionType) = periodTransactions.filter { it.transaction.type == type }
                .fold(0L) { total, entry -> Math.addExact(total, entry.primaryAmount(currency.currency) ?: 0L) }
            val income = total(TransactionType.INCOME)
            val expense = total(TransactionType.EXPENSE)
            val dayGroups = groupTransactionsByDay(periodTransactions, zone, currency.currency)
            BillUiState.Success(
                currency = currency.currency,
                amountsAvailable = available,
                year = calc.year,
                month = calc.month,
                monthDisplay = calc.headerDisplay,
                cardMonthDisplay = calc.cardDisplay,
                monthIncome = income,
                monthExpense = expense,
                monthBalance = income - expense,
                dayGroups = dayGroups,
                filterType = calc.filterType,
                filterStartDate = calc.startDate,
                filterEndDate = calc.endDate,
            )
        }
    }.flowOn(defaultDispatcher).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BillUiState.Loading,
    )

    /** 根据近期使用频次推荐最多 6 个常用分类供搜索筛选。 */
    internal val searchSuggestions = combine(categories, flow { emitAll(transactionRepository.recentCategoryIds(100)) }) { available, recent ->
        val counts = recent.groupingBy { it }.eachCount()
        available.asSequence()
            .filterNot { it.category.isDeleted }
            .sortedWith(compareByDescending<CategoryWithIcon> { counts[it.category.id] ?: 0 }.thenBy { it.category.sortOrder })
            .take(6).toList()
    }.flowOn(defaultDispatcher)

    /**
     * 执行账单搜索，不受当前主页日期周期限制。
     *
     * @param request 搜索请求参数（包含关键词、分类、类型、日期及金额区间）。
     * @return 过滤并排序后的搜索结果 Flow。
     */
    internal fun search(request: BillSearchRequest) = combine(
        currencies.selection,
        request.categoryId?.let(transactionRepository::getTransactionsByCategory)
            ?: transactionRepository.searchTransactions(request.query.trim()),
    ) { currency, rows ->
        buildBillSearchResults(rows, request, currency.currency, ZoneId.systemDefault())
    }.flowOn(defaultDispatcher)

    /** 切换统计周期为指定月份。 */
    fun setMonth(year: Int, month: Int) {
        dateFilter.value = BillDateFilter.Month(YearMonth.of(year, month))
    }

    /** 切换统计周期为指定基准日期所在的周。 */
    fun setWeek(anchorDate: LocalDate) {
        dateFilter.value = BillDateFilter.Week(anchorDate)
    }

    /** 切换统计周期为指定年份。 */
    fun setYear(year: Int) {
        dateFilter.value = BillDateFilter.Year(year)
    }

    /**
     * 响应通用周期选择器的选择结果。
     *
     * @param type 周期类型（周、月、年等）。
     * @param date 选中的基准日期。
     */
    fun selectPeriod(type: DateFilterType, date: LocalDate) {
        when (type) {
            DateFilterType.WEEK -> setWeek(date)
            DateFilterType.MONTH -> setMonth(date.year, date.monthValue)
            DateFilterType.YEAR -> setYear(date.year)
            DateFilterType.RANGE -> Unit
        }
    }

    /**
     * 创建一条新交易记录。
     *
     * @param type 交易类型（支出、收入、转账）。
     * @param amount 交易金额（分）。
     * @param accountId 支出账户或转出账户 ID。
     * @param toAccountId 转入账户 ID（仅转账交易生效）。
     * @param categoryId 交易分类 ID。
     * @param note 交易备注。
     * @param transactionTime 交易发生时间戳（毫秒）。
     * @param toAmount 跨币种转账时的转入金额（分）。
     * @param amountOverride 自定义金额汇率折算覆盖。
     */
    fun createTransaction(
        type: TransactionType,
        amount: Long,
        accountId: Long,
        toAccountId: Long? = null,
        categoryId: Long,
        note: String? = null,
        transactionTime: Long = System.currentTimeMillis(),
        toAmount: Long? = null,
        amountOverride: CurrencyAmountOverride? = null,
    ) {
        if (_saveState.value.saving || _saveState.value.saved) return
        _saveState.value = TransactionSaveState(saving = true)
        viewModelScope.launch {
            val transaction = Transaction(
                type = type,
                amount = amount,
                accountId = accountId,
                toAccountId = toAccountId,
                categoryId = categoryId,
                transactionTime = transactionTime,
                note = note?.trim()?.takeIf { it.isNotEmpty() },
                toAmount = toAmount,
            )
            try {
                transactionRepository.createTransaction(transaction, amountOverride)
                _saveState.value = TransactionSaveState(saved = true)
            } catch (error: CancellationException) {
                _saveState.value = TransactionSaveState()
                throw error
            } catch (error: Exception) {
                Timber.e(error, "Unable to create transaction")
                _saveState.value = TransactionSaveState(error = "保存失败，请重试")
            }
        }
    }

    /** 以新记录保存原交易的时间、金额及历史估值，由仓库原子更新账户余额。 */
    internal suspend fun copyTransaction(source: Transaction) {
        transactionRepository.createTransaction(source.copy(id = 0L, importKey = null))
    }

    /**
     * 对选中的交易批量应用修改。
     *
     * @param ids 目标交易 ID 集合。
     * @param change 批量变更指令（换账户、换分类或删除）。
     */
    internal suspend fun applyBatch(ids: Set<Long>, change: TransactionBatchChange) =
        transactionRepository.applyBatch(ids, change)

    /**
     * 删除指定 ID 的单条交易。
     *
     * @param id 待删除的交易 ID。
     */
    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(id)
        }
    }
}
