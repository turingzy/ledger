package cn.turingzy.ledger.ui.feature.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.turingzy.ledger.data.repository.AccountRepository
import cn.turingzy.ledger.data.repository.CategoryRepository
import cn.turingzy.ledger.data.repository.TransactionRepository
import cn.turingzy.ledger.model.AccountWithTemplate
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.model.CategoryWithIcon
import cn.turingzy.ledger.model.Transaction
import cn.turingzy.ledger.model.TransactionDetail
import cn.turingzy.ledger.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LedgerViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    accountRepository: AccountRepository,
) : ViewModel() {

    private val currentMonth = MutableStateFlow(YearMonth.now())

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

    val uiState: StateFlow<LedgerUiState> = currentMonth.flatMapLatest { month ->
        val zone = ZoneId.systemDefault()
        val startEpoch = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val endEpoch = month.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()

        combine(
            transactionRepository.getAmountSum(TransactionType.INCOME, startEpoch, endEpoch),
            transactionRepository.getAmountSum(TransactionType.EXPENSE, startEpoch, endEpoch),
            transactionRepository.getTransactionsBetween(startEpoch, endEpoch),
        ) { income, expense, transactions ->
            val dayGroups = groupTransactionsByDay(transactions, zone)
            LedgerUiState.Success(
                monthDisplay = "${month.year}年${month.monthValue}月",
                monthIncome = income,
                monthExpense = expense,
                monthBalance = income - expense,
                dayGroups = dayGroups,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LedgerUiState.Loading,
    )

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
        val dateFormatter = DateTimeFormatter.ofPattern("M月d日 E", Locale.CHINESE)

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
