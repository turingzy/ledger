package cn.turingzy.ledger.ui.feature.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.turingzy.ledger.data.repository.AccountRepository
import cn.turingzy.ledger.data.repository.CategoryRepository
import cn.turingzy.ledger.data.repository.TransactionRepository
import cn.turingzy.ledger.model.AccountWithTemplate
import cn.turingzy.ledger.model.CategoryWithIcon
import cn.turingzy.ledger.model.Transaction
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
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

sealed interface LedgerDateFilter {
    data class Week(val anchorDate: LocalDate) : LedgerDateFilter
    data class Month(val yearMonth: YearMonth) : LedgerDateFilter
    data class Year(val year: Int) : LedgerDateFilter
    data class Range(val startDate: LocalDate, val endDate: LocalDate) : LedgerDateFilter
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LedgerViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    accountRepository: AccountRepository,
) : ViewModel() {

    private val dateFilter = MutableStateFlow<LedgerDateFilter>(LedgerDateFilter.Month(YearMonth.now()))

    val categories: StateFlow<List<CategoryWithIcon>> = categoryRepository.getCategoriesStream()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val accounts: StateFlow<List<AccountWithTemplate>> = accountRepository.getAccountsStream()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val uiState: StateFlow<LedgerUiState> = dateFilter.flatMapLatest { filter ->
        val zone = ZoneId.systemDefault()
        val calc = when (filter) {
            is LedgerDateFilter.Week -> {
                val startOfWeek = filter.anchorDate.with(DayOfWeek.MONDAY)
                val endOfWeek = filter.anchorDate.with(DayOfWeek.SUNDAY)
                val s = startOfWeek.atStartOfDay(zone).toInstant().toEpochMilli()
                val e = endOfWeek.atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
                val header = "${filter.anchorDate.year}.${filter.anchorDate.monthValue.toString().padStart(2, '0')}"
                val cardLabel = "结余"
                FilterCalcResult(s, e, DateFilterType.WEEK, header, cardLabel, filter.anchorDate.year, filter.anchorDate.monthValue, startOfWeek, endOfWeek)
            }
            is LedgerDateFilter.Month -> {
                val s = filter.yearMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val e = filter.yearMonth.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
                val header = "${filter.yearMonth.year}.${filter.yearMonth.monthValue.toString().padStart(2, '0')}"
                val cardLabel = "结余"
                FilterCalcResult(s, e, DateFilterType.MONTH, header, cardLabel, filter.yearMonth.year, filter.yearMonth.monthValue, filter.yearMonth.atDay(1), filter.yearMonth.atEndOfMonth())
            }
            is LedgerDateFilter.Year -> {
                val startOfYear = LocalDate.of(filter.year, 1, 1)
                val endOfYear = LocalDate.of(filter.year, 12, 31)
                val s = startOfYear.atStartOfDay(zone).toInstant().toEpochMilli()
                val e = endOfYear.atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
                val header = "${filter.year}"
                val cardLabel = "结余"
                FilterCalcResult(s, e, DateFilterType.YEAR, header, cardLabel, filter.year, 1, startOfYear, endOfYear)
            }
            is LedgerDateFilter.Range -> {
                val s = filter.startDate.atStartOfDay(zone).toInstant().toEpochMilli()
                val e = filter.endDate.atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
                val header = "${filter.startDate.year}.${filter.startDate.monthValue.toString().padStart(2, '0')}"
                val cardLabel = "结余"
                FilterCalcResult(s, e, DateFilterType.RANGE, header, cardLabel, filter.startDate.year, filter.startDate.monthValue, filter.startDate, filter.endDate)
            }
        }

        combine(
            transactionRepository.getAmountSum(TransactionType.INCOME, calc.startEpoch, calc.endEpoch),
            transactionRepository.getAmountSum(TransactionType.EXPENSE, calc.startEpoch, calc.endEpoch),
            transactionRepository.getTransactionsBetween(calc.startEpoch, calc.endEpoch),
        ) { income, expense, transactions ->
            val dayGroups = groupTransactionsByDay(transactions, zone)
            LedgerUiState.Success(
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
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LedgerUiState.Loading,
    )

    fun setMonth(year: Int, month: Int) {
        dateFilter.value = LedgerDateFilter.Month(YearMonth.of(year, month))
    }

    fun setWeek(anchorDate: LocalDate) {
        dateFilter.value = LedgerDateFilter.Week(anchorDate)
    }

    fun setYear(year: Int) {
        dateFilter.value = LedgerDateFilter.Year(year)
    }

    fun setRange(startDate: LocalDate, endDate: LocalDate) {
        dateFilter.value = LedgerDateFilter.Range(startDate, endDate)
    }

    fun createTransaction(
        type: TransactionType,
        amount: Long,
        accountId: Long,
        toAccountId: Long? = null,
        categoryId: Long,
        note: String? = null,
        transactionTime: Long = System.currentTimeMillis(),
    ) {
        viewModelScope.launch {
            val transaction = Transaction(
                type = type,
                amount = amount,
                accountId = accountId,
                toAccountId = toAccountId,
                categoryId = categoryId,
                transactionTime = transactionTime,
                note = note?.trim()?.takeIf { it.isNotEmpty() },
            )
            transactionRepository.createTransaction(transaction)
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(id)
        }
    }

    private fun groupTransactionsByDay(
        transactions: List<TransactionDetail>,
        zone: ZoneId,
    ): List<DayTransactionGroup> {
        val today = LocalDate.now(zone)
        val dateFormatter = DateTimeFormatter.ofPattern("MM.dd E", Locale.CHINESE)

        return transactions
            .groupBy { tx ->
                Instant.ofEpochMilli(tx.transaction.transactionTime).atZone(zone).toLocalDate()
            }
            .map { (date, txList) ->
                val dayExpense = txList
                    .filter { it.transaction.type == TransactionType.EXPENSE }
                    .sumOf { it.transaction.amount }
                val dayIncome = txList
                    .filter { it.transaction.type == TransactionType.INCOME }
                    .sumOf { it.transaction.amount }

                val prefix = when (date) {
                    today -> "今天 · "
                    today.minusDays(1) -> "昨天 · "
                    else -> ""
                }

                DayTransactionGroup(
                    dateEpochDay = date.toEpochDay(),
                    displayDate = "$prefix${date.format(dateFormatter)}",
                    totalExpense = dayExpense,
                    totalIncome = dayIncome,
                    transactions = txList.sortedByDescending { it.transaction.transactionTime },
                )
            }
            .sortedByDescending { it.dateEpochDay }
    }
}
