package cn.turingzy.ledger.ui.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.turingzy.ledger.data.repository.AccountRepository
import cn.turingzy.ledger.data.repository.CardTemplateRepository
import cn.turingzy.ledger.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val cardTemplateRepository: CardTemplateRepository,
    private val accountRepository: AccountRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        categoryRepository.getCategoriesStream(),
        cardTemplateRepository.getTemplatesStream(),
        accountRepository.getAccountsStream(),
    ) { categories, templates, accounts ->
        SettingsUiState.Success(
            categories = categories,
            cardTemplates = templates,
            accounts = accounts,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState.Loading,
    )

}
