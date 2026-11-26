package cn.turingzy.ledger.ui.feature.wallet

import cn.turingzy.ledger.ui.feature.wallet.sheet.accountCurrencyCodes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.turingzy.ledger.data.repository.account.CardTemplateRepository
import cn.turingzy.ledger.data.repository.transaction.TransactionRepository
import cn.turingzy.ledger.data.repository.account.AccountRepository
import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.currency.primaryAmount
import cn.turingzy.ledger.model.currency.primaryBalance
import cn.turingzy.ledger.di.Dispatcher
import cn.turingzy.ledger.di.LedgerDispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * 钱包页面业务逻辑 ViewModel。
 *
 * 聚合账户、卡片模板、全量交易及币种汇率流，提供资产统计与账户增删改等管理能力。
 */
@HiltViewModel
class WalletViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val cardTemplateRepository: CardTemplateRepository,
    @Dispatcher(LedgerDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher,
    private val transactionRepository: TransactionRepository,
    private val currencies: cn.turingzy.ledger.data.repository.currency.CurrencyRepository,
) : ViewModel() {

    init {
        viewModelScope.launch(defaultDispatcher) {
            accountCurrencyCodes
        }
    }

    /**
     * 钱包页面 UI 状态流，订阅时保持活跃并在退火 5 秒后超时。
     */
    val uiState: StateFlow<WalletUiState> = combine(
        accountRepository.getAccountsStream(includeCardDetails = true),
        cardTemplateRepository.getTemplatesStream(),
        transactionRepository.getTransactionsBetween(Long.MIN_VALUE, Long.MAX_VALUE),
        currencies.selection,
    ) { accounts, templates, transactions, selection ->
        val totals = walletCurrencyTotals(accounts, transactions, selection.currency)
        val primary = totals.first()

        WalletUiState.Success(
            totalAsset = primary.assets,
            amountsAvailable = accounts.filter { it.account.includeInTotal }.all { it.account.primaryBalance(selection.currency) != null } && transactions.filter { it.transaction.type != cn.turingzy.ledger.model.transaction.TransactionType.TRANSFER }.all { it.primaryAmount(selection.currency) != null },
            totalDebt = primary.creditBalance,
            currencyTotals = totals,
            accounts = accounts,
            templates = templates,
        )
    }.flowOn(defaultDispatcher).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WalletUiState.Loading,
    )

    /** 保存已通过表单校验的新账户和银行卡信息。 */
    fun createAccount(account: Account) = saveAccount { accountRepository.createAccount(account) }

    /** 更新账户属性和银行卡信息，保留当前余额。 */
    fun updateAccountInfo(account: Account) = saveAccount { accountRepository.updateAccountInfo(account) }

    private val _accountSaveErrors = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val accountSaveErrors = _accountSaveErrors.asSharedFlow()
    private val accountWrites = Mutex()

    /** 将保存失败交给钱包页面显示，避免记录敏感表单内容。 */
    private fun saveAccount(action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                accountWrites.withLock { action() }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _accountSaveErrors.emit(Unit)
            }
        }
    }

    /**
     * 调整指定账户的当前余额，并自动生成余额校准记录。
     *
     * @param accountId 目标账户 ID
     * @param newBalance 调整后的新余额（单位：分）
     */
    fun adjustBalance(accountId: Long, newBalance: Long) {
        viewModelScope.launch {
            accountRepository.adjustBalance(accountId, newBalance)
        }
    }

    /**
     * 删除指定账户。
     *
     * @param accountId 目标账户 ID
     */
    fun deleteAccount(accountId: Long) = saveAccount { accountRepository.deleteAccount(accountId) }
}
