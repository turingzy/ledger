package cn.turingzy.ledger.ui.feature.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.turingzy.ledger.data.repository.CardTemplateRepository
import cn.turingzy.ledger.data.repository.AccountRepository
import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.util.Dispatcher
import cn.turingzy.ledger.util.LedgerDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import cn.turingzy.ledger.model.AccountType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val cardTemplateRepository: CardTemplateRepository,
    @Dispatcher(LedgerDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    init {
        viewModelScope.launch(defaultDispatcher) {
            accountCurrencyCodes
        }
    }

    val uiState: StateFlow<WalletUiState> = combine(
        accountRepository.getTotalAsset(),
        accountRepository.getTotalCreditDebt(),
        accountRepository.getAccountsStream(),
        cardTemplateRepository.getTemplatesStream(),
    ) { totalAsset, totalDebt, accounts, templates ->
        WalletUiState.Success(
            totalAsset = totalAsset,
            totalDebt = totalDebt,
            accounts = accounts,
            templates = templates,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WalletUiState.Loading,
    )

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

    fun updateAccountInfo(account: Account) {
        viewModelScope.launch {
            accountRepository.updateAccountInfo(account)
        }
    }

    fun adjustBalance(accountId: Long, newBalance: Long) {
        viewModelScope.launch {
            accountRepository.adjustBalance(accountId, newBalance)
        }
    }

    fun deleteAccount(accountId: Long) {
        viewModelScope.launch {
            accountRepository.deleteAccount(accountId)
        }
    }
}
