package cn.turingzy.ledger.ui.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.turingzy.ledger.data.repository.TransactionRepository
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
)

@HiltViewModel
class AnalyticsDashboardViewModel @Inject constructor(
    transactions: TransactionRepository,
    @Dispatcher(LedgerDispatchers.Default) dispatcher: CoroutineDispatcher,
) : ViewModel() {
    val uiState = transactions.getTransactionsBetween(Long.MIN_VALUE, Long.MAX_VALUE)
        .map { AnalyticsDashboardState(analyticsDays(it, ZoneId.systemDefault()), loading = false) }
        .flowOn(dispatcher)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsDashboardState())
}
