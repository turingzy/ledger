package cn.turingzy.ledger.ui.feature.settings

import cn.turingzy.ledger.model.CategoryWithIcon
import cn.turingzy.ledger.model.Icon

sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Success(
        val appVersion: String = "1.0.0",
        val categories: List<CategoryWithIcon>,
        val availableIcons: List<Icon>,
    ) : SettingsUiState
}
