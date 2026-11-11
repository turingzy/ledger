package cn.turingzy.ledger.ui.feature.ledger

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.turingzy.ledger.data.repository.AccountRepository
import cn.turingzy.ledger.data.repository.CategoryRepository
import cn.turingzy.ledger.data.repository.TransactionRepository
import cn.turingzy.ledger.model.AccountWithTemplate
import cn.turingzy.ledger.model.CategoryWithIcon
import cn.turingzy.ledger.model.Transaction
import cn.turingzy.ledger.model.TransactionDetail
import cn.turingzy.ledger.util.toFormattedAmount
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

internal sealed interface TransactionDetailsUiState {
    data object Loading : TransactionDetailsUiState
    data class Unavailable(val transactionId: Long) : TransactionDetailsUiState
    data class Error(val transactionId: Long) : TransactionDetailsUiState
    data class Success(
        val detail: TransactionDetail,
        val categories: List<CategoryWithIcon>,
        val accounts: List<AccountWithTemplate>,
    ) : TransactionDetailsUiState
}

internal data class TransactionSaveState(
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionDetailsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: TransactionRepository,
    categoryRepository: CategoryRepository,
    accountRepository: AccountRepository,
) : ViewModel() {
    internal val selectedTransactionId = savedStateHandle.getStateFlow<Long?>("editingTransactionId", null)
    private val _saveState = MutableStateFlow(TransactionSaveState())
    internal val saveState = _saveState.asStateFlow()

    internal val uiState: StateFlow<TransactionDetailsUiState> = selectedTransactionId.flatMapLatest { id ->
        if (id == null) return@flatMapLatest flowOf<TransactionDetailsUiState>(TransactionDetailsUiState.Loading)
        combine<TransactionDetail?, List<CategoryWithIcon>, List<AccountWithTemplate>, TransactionDetailsUiState>(
            repository.getTransaction(id),
            categoryRepository.getCategoriesStream(),
            accountRepository.getAccountsStream(),
        ) { detail, categories, accounts ->
            if (detail == null || detail.transaction.isDeleted) TransactionDetailsUiState.Unavailable(id)
            else TransactionDetailsUiState.Success(detail, categories, accounts)
        }.onStart { emit(TransactionDetailsUiState.Loading) }.catch { error ->
            if (error is CancellationException) throw error
            Timber.e(error, "Unable to load transaction %s", id)
            emit(TransactionDetailsUiState.Error(id))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionDetailsUiState.Loading)

    internal fun selectTransaction(id: Long?) {
        if (_saveState.value.saving || id == selectedTransactionId.value) return
        _saveState.value = TransactionSaveState()
        savedStateHandle["editingTransactionId"] = id
    }

    internal fun save(transaction: Transaction) {
        if (_saveState.value.saving || _saveState.value.saved) return
        val state = uiState.value as? TransactionDetailsUiState.Success ?: return
        val transactionId = selectedTransactionId.value ?: return
        if (state.detail.transaction.id != transactionId) return
        val validated = RecordFormInput(
            transaction.type, transaction.amount.toFormattedAmount(), transaction.accountId, transaction.toAccountId,
            transaction.categoryId, transaction.transactionTime, transaction.note.orEmpty(),
        ).toTransactionOrNull(state.accounts, state.categories)
        if (validated == null) {
            _saveState.value = TransactionSaveState(error = "请检查金额、分类和账户")
            return
        }
        _saveState.value = TransactionSaveState(saving = true)
        viewModelScope.launch {
            try {
                repository.updateTransaction(validated.copy(id = transactionId))
                _saveState.value = TransactionSaveState(saved = true)
            } catch (error: CancellationException) {
                _saveState.value = TransactionSaveState()
                throw error
            } catch (error: Exception) {
                Timber.e(error, "Unable to update transaction %s", transactionId)
                _saveState.value = TransactionSaveState(error = "保存失败，请重试")
            }
        }
    }
}
