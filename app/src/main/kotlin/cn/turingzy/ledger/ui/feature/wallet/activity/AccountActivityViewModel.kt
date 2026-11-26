package cn.turingzy.ledger.ui.feature.wallet.activity

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import cn.turingzy.ledger.data.repository.account.AccountRepository
import cn.turingzy.ledger.data.repository.transaction.TransactionRepository
import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.transaction.TransactionDetail
import cn.turingzy.ledger.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** 账户子页读取账户原币余额和交易历史。 */
@HiltViewModel
internal class AccountActivityViewModel @Inject constructor(
    savedState: SavedStateHandle,
    accounts: AccountRepository,
    transactions: TransactionRepository,
) : ViewModel() {
    private val id = savedState.toRoute<Route.WalletAccount>().accountId

    val state = combine(accounts.getAccount(id), transactions.getTransactionsByAccount(id)) { account, rows ->
        val rowAccount = rows.firstOrNull()?.let { if (it.account.id == id) it.account else it.toAccount }
        // 同一次 Room 查询中的账户余额和流水属于同一个数据库快照。
        AccountActivityState(account?.let { rowAccount ?: it.account }, rows)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** 已加载的账户子页数据，账户为空时由页面显示不可用状态。 */
internal data class AccountActivityState(val account: Account?, val transactions: List<TransactionDetail>)
