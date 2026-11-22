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
import kotlinx.coroutines.CoroutineDispatcher
import cn.turingzy.ledger.model.account.AccountType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
        accountRepository.getAccountsStream(),
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

    /**
     * 创建新账户。
     *
     * @param name 账户名称
     * @param type 账户类型
     * @param initialBalance 初始余额（单位：分）
     * @param cardNumber 卡号或账号（可选）
     * @param includeInTotal 是否计入总资产
     * @param currency 币种代码，默认为 "CNY"
     * @param templateId 关联的银行卡卡面模板 ID（可选）
     */
    fun createAccount(
        name: String,
        type: AccountType,
        initialBalance: Long,
        cardNumber: String?,
        includeInTotal: Boolean = true,
        currency: String = "CNY",
        templateId: Long? = null,
    ) {
        viewModelScope.launch {
            val account = Account(
                name = name.trim(),
                type = type,
                initialBalance = initialBalance,
                currentBalance = initialBalance,
                cardNumber = cardNumber?.trim()?.takeIf { it.isNotEmpty() },
                includeInTotal = includeInTotal,
                currency = currency,
                templateId = templateId,
            )
            accountRepository.createAccount(account)
        }
    }

    /**
     * 更新已有账户的基本信息。
     *
     * @param account 更新后的账户实体
     */
    fun updateAccountInfo(account: Account) {
        viewModelScope.launch {
            accountRepository.updateAccountInfo(account)
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
    fun deleteAccount(accountId: Long) {
        viewModelScope.launch {
            accountRepository.deleteAccount(accountId)
        }
    }
}
