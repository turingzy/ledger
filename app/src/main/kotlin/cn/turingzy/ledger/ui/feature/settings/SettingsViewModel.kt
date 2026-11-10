package cn.turingzy.ledger.ui.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.turingzy.ledger.data.repository.AccountRepository
import cn.turingzy.ledger.data.repository.CardTemplateRepository
import cn.turingzy.ledger.data.repository.CategoryRepository
import cn.turingzy.ledger.data.repository.IconRepository
import cn.turingzy.ledger.model.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val iconRepository: IconRepository,
    private val cardTemplateRepository: CardTemplateRepository,
    private val accountRepository: AccountRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        categoryRepository.getCategoriesStream(),
        iconRepository.getIconsStream(),
        cardTemplateRepository.getTemplatesStream(),
        accountRepository.getAccountsStream(),
    ) { categories, icons, templates, accounts ->
        SettingsUiState.Success(
            categories = categories,
            availableIcons = icons,
            cardTemplates = templates,
            accounts = accounts,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState.Loading,
    )

    suspend fun saveCategory(category: Category): Boolean {
        if (category.name.isBlank()) return false
        return changeCategory {
            val value = category.copy(name = category.name.trim())
            if (value.id == 0L) categoryRepository.createCategory(value)
            else categoryRepository.updateCategory(value)
        }
    }

    suspend fun deleteManagedCategory(category: Category): Boolean {
        if (category.isSystem || category.id == 0L) return false
        return changeCategory { categoryRepository.deleteCategory(category.id) }
    }

    private suspend fun changeCategory(change: suspend () -> Unit): Boolean = try {
        change()
        true
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Timber.e(error, "Unable to save category changes")
        false
    }

}
