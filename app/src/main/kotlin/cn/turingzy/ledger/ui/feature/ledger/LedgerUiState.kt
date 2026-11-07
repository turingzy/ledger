package cn.turingzy.ledger.ui.feature.ledger

import cn.turingzy.ledger.model.TransactionDetail

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
        val monthDisplay: String,
        val monthIncome: Long,
        val monthExpense: Long,
        val monthBalance: Long,
        val dayGroups: List<DayTransactionGroup>,
    ) : LedgerUiState
}
