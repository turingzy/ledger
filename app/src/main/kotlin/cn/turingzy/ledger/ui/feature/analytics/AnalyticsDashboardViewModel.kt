package cn.turingzy.ledger.ui.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.turingzy.ledger.model.primaryAmount
import cn.turingzy.ledger.data.repository.TransactionRepository
import cn.turingzy.ledger.data.repository.CurrencyRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import cn.turingzy.ledger.util.Dispatcher
import cn.turingzy.ledger.util.LedgerDispatchers
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class AnalyticsDashboardState(
    val days: Map<LocalDate, AnalyticsDay> = emptyMap(),
    val loading: Boolean = true,
    val transactions: List<AnalyticsTransaction> = emptyList(),
    val currency: String = "CNY",
    val amountsAvailable: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsDashboardViewModel @Inject constructor(
    transactions: TransactionRepository,
    currencies: CurrencyRepository,
    @Dispatcher(LedgerDispatchers.Default) dispatcher: CoroutineDispatcher,
) : ViewModel() {
    val uiState = currencies.selection.flatMapLatest { currency ->
        transactions.getTransactionsBetween(Long.MIN_VALUE, Long.MAX_VALUE, currency.currency).map { entries ->
                val zone = ZoneId.systemDefault()
                AnalyticsDashboardState(currency = currency.currency, days = analyticsDays(entries, zone), loading = false,
                    transactions = analyticsTransactions(entries, zone), amountsAvailable = entries.filter { it.transaction.type != cn.turingzy.ledger.model.TransactionType.TRANSFER }.all { it.primaryAmount(currency.currency) != null })
            }
        }
        .flowOn(dispatcher)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsDashboardState())
}
