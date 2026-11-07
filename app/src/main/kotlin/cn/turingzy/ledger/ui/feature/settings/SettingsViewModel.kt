package cn.turingzy.ledger.ui.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.turingzy.ledger.data.repository.CategoryRepository
import cn.turingzy.ledger.data.repository.IconRepository
import cn.turingzy.ledger.model.Category
import cn.turingzy.ledger.model.CategoryType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val iconRepository: IconRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        categoryRepository.getCategoriesStream(),
        iconRepository.getIconsStream(),
    ) { categories, icons ->
        SettingsUiState.Success(
            categories = categories,
            availableIcons = icons,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState.Loading,
    )

    fun createCategory(
        name: String,
        type: CategoryType,
        iconId: Long,
    ) {
        viewModelScope.launch {
            val category = Category(
                name = name.trim(),
                type = type,
                iconId = iconId,
                isSystem = false,
            )
            categoryRepository.createCategory(category)
        }
    }

    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(categoryId)
        }
    }
}
