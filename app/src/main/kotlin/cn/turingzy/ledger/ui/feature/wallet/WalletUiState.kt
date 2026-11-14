package cn.turingzy.ledger.ui.feature.wallet

import cn.turingzy.ledger.model.CardTemplate
import cn.turingzy.ledger.model.AccountWithTemplate

sealed interface WalletUiState {
    data object Loading : WalletUiState

    data class Success(
        val totalAsset: Long,
        val totalDebt: Long,
        val accounts: List<AccountWithTemplate>,
        val currencyTotals: List<WalletCurrencyTotals> = emptyList(),
        val templates: List<CardTemplate> = emptyList(),
    ) : WalletUiState
}
