package cn.turingzy.ledger.ui.feature.analytics

import cn.turingzy.ledger.ui.feature.analytics.calculation.AnalyticsDay
import cn.turingzy.ledger.ui.feature.analytics.calculation.analyticsDays
import cn.turingzy.ledger.ui.feature.analytics.calculation.analyticsTransactions
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.turingzy.ledger.model.currency.primaryAmount
import cn.turingzy.ledger.data.repository.transaction.TransactionRepository
import cn.turingzy.ledger.data.repository.currency.CurrencyRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import cn.turingzy.ledger.di.Dispatcher
import cn.turingzy.ledger.di.LedgerDispatchers
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * 统计看板的 UI 状态。
 *
 * @property days 按日期映射的每日统计明细。
 * @property loading 是否处于数据加载中状态。
 * @property transactions 转换后的分析用账单交易列表。
 * @property currency 当前主币种代码。
 * @property amountsAvailable 是否所有非转账交易的主币种金额都可用（即汇率估值是否完备）。
 */
data class AnalyticsDashboardState(
    val days: Map<LocalDate, AnalyticsDay> = emptyMap(),
    val loading: Boolean = true,
    val transactions: List<AnalyticsTransaction> = emptyList(),
    val currency: String = "CNY",
    val amountsAvailable: Boolean = true,
)

/**
 * 统计看板 ViewModel。
 *
 * 监听主币种设置和全量交易数据变更，在后台派生并提供统计看板所需的每日数据聚合与交易列表。
 */
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
                    transactions = analyticsTransactions(entries, zone), amountsAvailable = entries.filter { it.transaction.type != cn.turingzy.ledger.model.transaction.TransactionType.TRANSFER }.all { it.primaryAmount(currency.currency) != null })
            }
        }
        .flowOn(dispatcher)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsDashboardState())
}
