package cn.turingzy.ledger.ui.feature.ledger

import cn.turingzy.ledger.model.DateFilterType
import cn.turingzy.ledger.model.TransactionDetail
import java.time.LocalDate

data class DayTransactionGroup(
    val dateEpochDay: Long,
    val displayDate: String,
    val totalExpense: Long,
    val totalIncome: Long,
    val transactions: List<TransactionDetail>,
)

sealed interface LedgerUiState {
    data object Loading : LedgerUiState

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
    ) : LedgerUiState
}
