package cn.turingzy.ledger.ui.feature.settings

import cn.turingzy.ledger.model.account.AccountWithTemplate
import cn.turingzy.ledger.model.account.CardTemplate
import cn.turingzy.ledger.model.category.CategoryWithIcon

sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Success(
        val rules: List<cn.turingzy.ledger.model.transfer.BillParseRule> = emptyList(),
        val message: String? = null,
        val appVersion: String = "1.0.0",
        val categories: List<CategoryWithIcon>,
        val cardTemplates: List<CardTemplate> = emptyList(),
        val accounts: List<AccountWithTemplate> = emptyList(),
    ) : SettingsUiState
}
