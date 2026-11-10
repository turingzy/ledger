package cn.turingzy.ledger.ui.feature.settings

import cn.turingzy.ledger.model.AccountWithTemplate
import cn.turingzy.ledger.model.CardTemplate
import cn.turingzy.ledger.model.CategoryWithIcon

sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Success(
        val appVersion: String = "1.0.0",
        val categories: List<CategoryWithIcon>,
        val cardTemplates: List<CardTemplate> = emptyList(),
        val accounts: List<AccountWithTemplate> = emptyList(),
    ) : SettingsUiState
}
