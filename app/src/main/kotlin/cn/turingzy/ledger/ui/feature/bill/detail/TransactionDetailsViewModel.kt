package cn.turingzy.ledger.ui.feature.bill.detail

import cn.turingzy.ledger.ui.feature.bill.sheet.RecordFormInput
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.turingzy.ledger.data.repository.account.AccountRepository
import cn.turingzy.ledger.data.repository.category.CategoryRepository
import cn.turingzy.ledger.data.repository.transaction.TransactionRepository
import cn.turingzy.ledger.model.account.AccountWithTemplate
import cn.turingzy.ledger.model.category.CategoryWithIcon
import cn.turingzy.ledger.model.transaction.Transaction
import cn.turingzy.ledger.model.currency.CurrencyAmountOverride
import cn.turingzy.ledger.model.transaction.TransactionDetail
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

/**
 * 交易详情与编辑页面的 UI 状态。
 */
internal sealed interface TransactionDetailsUiState {
    /** 交易详情数据加载中。 */
    data object Loading : TransactionDetailsUiState
    /** 交易不存在或已被删除。 */
    data class Unavailable(val transactionId: Long) : TransactionDetailsUiState
    /** 加载交易详情发生异常。 */
    data class Error(val transactionId: Long) : TransactionDetailsUiState
    /**
     * 交易详情及相关分类、账户候选数据加载成功。
     *
     * @property detail 交易详情数据 [TransactionDetail]。
     * @property categories 可用分类列表。
     * @property accounts 可用账户列表。
     */
    data class Success(
        val detail: TransactionDetail,
        val categories: List<CategoryWithIcon>,
        val accounts: List<AccountWithTemplate>,
    ) : TransactionDetailsUiState
}

/**
 * 交易保存/修改的执行状态。
 *
 * @property saving 是否正在异步保存中。
 * @property saved 是否保存成功。
 * @property error 保存失败时的错误提示信息。
 */
internal data class TransactionSaveState(
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
)

/**
 * 交易详情及编辑的 ViewModel。
 *
 * 管理当前选中的交易 ID，加载完整的交易详情、关联分类和账户，
 * 校验表单修改并提交更新。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionDetailsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: TransactionRepository,
    categoryRepository: CategoryRepository,
    accountRepository: AccountRepository,
    private val exchangeRates: cn.turingzy.ledger.data.repository.currency.ExchangeRateRepository,
) : ViewModel() {
    /** 当前选中的交易 ID 状态流。 */
    internal val selectedTransactionId = savedStateHandle.getStateFlow<Long?>("editingTransactionId", null)
    private val _saveState = MutableStateFlow(TransactionSaveState())
    /** 保存/更新状态流。 */
    internal val saveState = _saveState.asStateFlow()

    /** 交易详情加载状态流。 */
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

    /**
     * 查询指定日期下两种货币之间的折算汇率。
     *
     * @param from 源币种。
     * @param to 目标币种。
     * @param time 时间戳（毫秒）。
     * @return 格式化后的汇率字符串。
     */
    internal suspend fun exchangeRate(from: String, to: String, time: Long): String = exchangeRates.quote(from, to,
        java.time.Instant.ofEpochMilli(time).atZone(java.time.ZoneId.systemDefault()).toLocalDate()).rate

    /**
     * 选中或取消选中待编辑的交易。
     *
     * @param id 目标交易 ID，为 null 时收起详情编辑界面。
     */
    internal fun selectTransaction(id: Long?) {
        if (_saveState.value.saving || id == selectedTransactionId.value) return
        savedStateHandle["editingTransactionId"] = id
        _saveState.value = TransactionSaveState()
    }

    /**
     * 保存对当前交易的修改。
     *
     * @param transaction 修改后的交易实体数据。
     * @param amountOverride 自定义币种金额折算覆盖。
     */
    internal fun save(transaction: Transaction, amountOverride: CurrencyAmountOverride? = null) {
        if (_saveState.value.saving || _saveState.value.saved) return
        val state = uiState.value as? TransactionDetailsUiState.Success ?: return
        val transactionId = selectedTransactionId.value ?: return
        if (state.detail.transaction.id != transactionId) return
        val validated = RecordFormInput(
            transaction.type, transaction.amount.toFormattedAmount(), transaction.accountId, transaction.toAccountId,
            transaction.categoryId, transaction.transactionTime, transaction.note.orEmpty(), transaction.transferRate,
        ).toTransactionOrNull(state.accounts, state.categories)
        if (validated == null) {
            _saveState.value = TransactionSaveState(error = "请检查金额、分类和账户")
            return
        }
        val sourceCurrency = state.accounts.firstOrNull { it.account.id == transaction.accountId }?.account?.currency
        val targetCurrency = state.accounts.firstOrNull { it.account.id == transaction.toAccountId }?.account?.currency
        val toAmount = if (sourceCurrency == targetCurrency) validated.toAmount else transaction.toAmount ?: validated.toAmount
        persist("保存失败，请重试") {
            repository.updateTransaction(validated.copy(id = transactionId, toAmount = toAmount), amountOverride)
        }
    }

    private fun persist(failureMessage: String, action: suspend () -> Unit) {
        if (_saveState.value.saving || _saveState.value.saved) return
        _saveState.value = TransactionSaveState(saving = true)
        viewModelScope.launch {
            try {
                action()
                _saveState.value = TransactionSaveState(saved = true)
            } catch (error: CancellationException) {
                _saveState.value = TransactionSaveState()
                throw error
            } catch (error: Exception) {
                Timber.e(error, "Unable to persist transaction %s", selectedTransactionId.value)
                _saveState.value = TransactionSaveState(error = failureMessage)
            }
        }
    }
}
