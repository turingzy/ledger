package cn.turingzy.ledger.ui.feature.bill

import cn.turingzy.ledger.model.transaction.DateFilterType
import cn.turingzy.ledger.model.transaction.TransactionDetail
import java.time.LocalDate

data class DayCurrencyAmounts(val currency: String, val expense: Long, val income: Long)

data class DayTransactionGroup(
    val dateEpochDay: Long,
    val displayDate: String,
    val totalExpense: Long,
    val totalIncome: Long,
    val transactions: List<TransactionDetail>,
    val currencyAmounts: List<DayCurrencyAmounts> = emptyList(),
)

sealed interface BillUiState {
    data object Loading : BillUiState

    data class Success(
        val year: Int,
        val month: Int,
        val monthDisplay: String, // e.g. "2026年 11月"
        val cardMonthDisplay: String, // e.g. "11月 结余"
        val monthIncome: Long,
        val monthExpense: Long,
        val monthBalance: Long,
        val dayGroups: List<DayTransactionGroup>,
        val filterType: DateFilterType = DateFilterType.MONTH,
        val filterStartDate: LocalDate? = null,
        val filterEndDate: LocalDate? = null,
        val currency: String = "CNY",
        val searchQuery: String = "",
        val amountsAvailable: Boolean = true,
    ) : BillUiState
}
