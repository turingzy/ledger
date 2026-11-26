package cn.turingzy.ledger.ui.feature.wallet.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.turingzy.ledger.R
import cn.turingzy.ledger.model.transaction.DateFilterType
import cn.turingzy.ledger.model.transaction.TransactionDetail
import cn.turingzy.ledger.model.transaction.TransactionType
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.base.CenteredEmptyStateBase
import cn.turingzy.ledger.ui.component.base.PageScaffoldBase
import cn.turingzy.ledger.ui.component.biz.LocalCurrencyBiz
import cn.turingzy.ledger.ui.component.biz.TransactionGroupHeaderBiz
import cn.turingzy.ledger.ui.component.biz.TransactionGroupRowBiz
import cn.turingzy.ledger.ui.component.biz.TransactionRowBiz
import cn.turingzy.ledger.ui.component.biz.categoryGlyphBiz
import cn.turingzy.ledger.ui.component.input.SegmentedControl
import cn.turingzy.ledger.ui.component.picker.PeriodPickerPill
import cn.turingzy.ledger.ui.component.picker.PeriodPickerPillHeight
import cn.turingzy.ledger.ui.component.picker.PeriodNavigationArrow
import cn.turingzy.ledger.ui.component.picker.PeriodPickerSheet
import cn.turingzy.ledger.ui.component.picker.periodLabel
import cn.turingzy.ledger.ui.feature.analytics.component.StatisticsMetric
import cn.turingzy.ledger.ui.feature.bill.detail.TransactionDetailsScreen
import cn.turingzy.ledger.ui.feature.bill.detail.TransactionDetailsViewModel
import cn.turingzy.ledger.ui.feature.wallet.WalletSectionHeading
import cn.turingzy.ledger.ui.theme.LedgerStyle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlinx.coroutines.launch

/** 钱包的全屏账户子页，统一按所选月或年组织余额和原币交易。 */
@Composable
internal fun AccountActivityScreen(
    onBack: () -> Unit,
    viewModel: AccountActivityViewModel = hiltViewModel(),
    detailViewModel: TransactionDetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Box(Modifier.fillMaxSize()) {
        AccountActivityContent(state, onBack, detailViewModel::selectTransaction)
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
    TransactionDetailsScreen(
        onLoadFailure = { message -> scope.launch { snackbar.showSnackbar(message) } },
        viewModel = detailViewModel,
    )
}

/** 渲染账户的周期、汇总、走势图和流水，日期选择独立于当前读数。 */
@Composable
internal fun AccountActivityContent(
    state: AccountActivityState?,
    onBack: () -> Unit,
    onTransactionClick: (Long) -> Unit = {},
) {
    val account = state?.account
    val list = rememberLazyListState()
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    var anchor by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    var period by rememberSaveable { mutableStateOf(DateFilterType.MONTH) }
    var selectingDate by rememberSaveable { mutableStateOf(false) }
    val date = LocalDate.ofEpochDay(anchor)
    val yearly = period == DateFilterType.YEAR
    val activity = remember(state, period, date, today, zone) {
        if (account == null) null else accountPeriodActivity(account, state.transactions, period, date, today, zone)
    }

    PageScaffoldBase(
        title = account?.name ?: stringResource(R.string.ui_account),
        onBack = onBack,
        closeInsteadOfBack = true,
        scrolled = list.firstVisibleItemIndex > 0 || list.firstVisibleItemScrollOffset > 0,
    ) { pagePadding ->
        val spacing = LedgerStyle.spacing
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = spacing.pageHorizontal, end = spacing.pageHorizontal,
                top = pagePadding.calculateTopPadding() + spacing.extraSmall, bottom = spacing.block,
            ),
        ) {
            if (activity == null || account == null) {
                if (state != null) item {
                    CenteredEmptyStateBase(stringResource(R.string.wallet_account_unavailable), Modifier.height(160.dp))
                }
                return@LazyColumn
            }
            item(key = "account_sections") {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sectionGap)) {
                    AccountPeriodControls(period, date, onPeriodChange = { period = it }, onDateClick = { selectingDate = true }) { step ->
                        anchor = (if (yearly) date.plusYears(step) else date.plusMonths(step)).toEpochDay()
                    }
                    AccountPeriodSummary(activity, account.currency)
                    AccountBalanceChart(activity.balances, account.currency, yearly)
                    WalletSectionHeading(stringResource(R.string.wallet_account_transactions))
                }
            }
            if (activity.groupedTransactions.isEmpty()) item(key = "empty_transactions") {
                CenteredEmptyStateBase(stringResource(R.string.ui_no_transactions), Modifier.height(88.dp))
            }
            activity.groupedTransactions.entries.forEachIndexed { index, (day, rows) ->
                item(key = "date:$day") {
                    TransactionGroupHeaderBiz(
                        day, summary = "",
                        modifier = Modifier.padding(top = if (index == 0) spacing.none else spacing.sectionGap, bottom = spacing.medium),
                    )
                }
                itemsIndexed(rows, key = { _, detail -> detail.transaction.id }) { index, detail ->
                    TransactionGroupRowBiz(index == 0, index == rows.lastIndex) {
                        AccountActivityTransaction(detail, account.id, account.currency, zone) { onTransactionClick(detail.transaction.id) }
                    }
                }
            }
        }
    }
    if (selectingDate && account != null) {
        PeriodPickerSheet(
            type = period,
            initialDate = date,
            onDismiss = { selectingDate = false },
            onSelect = { _, selected -> anchor = if (yearly) date.withYear(selected.year).toEpochDay() else selected.toEpochDay() },
            showViewSelector = false,
        )
    }
}

/** 周期胶囊与月年切换对应同一份日期选择。 */
@Composable
private fun AccountPeriodControls(
    period: DateFilterType,
    date: LocalDate,
    onPeriodChange: (DateFilterType) -> Unit,
    onDateClick: () -> Unit,
    onStep: (Long) -> Unit,
) {
    val month = period == DateFilterType.MONTH
    Row(
        Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PeriodPickerPill(
                periodLabel(period, date), onDateClick,
                modifier = Modifier.weight(1f, fill = false).height(PeriodPickerPillHeight),
                description = stringResource(if (month) R.string.ui_select_month else R.string.ui_select_year),
                trailingIcon = MaterialSymbols.DateRange,
            )
            PeriodNavigationArrow(false, { onStep(-1L) })
            PeriodNavigationArrow(true, { onStep(1L) })
        }
        SegmentedControl(
            labels = listOf(stringResource(R.string.ui_month), stringResource(R.string.ui_year)),
            selectedIndex = if (month) 0 else 1,
            onSelect = { onPeriodChange(if (it == 0) DateFilterType.MONTH else DateFilterType.YEAR) },
            modifier = Modifier.width(94.dp),
            height = PeriodPickerPillHeight,
            emphasizeSelection = false,
        )
    }
}

/** 汇总按账户原币显示期间收支和两者差额。 */
@Composable
private fun AccountPeriodSummary(activity: AccountPeriodActivity, currency: String) {
    CompositionLocalProvider(LocalCurrencyBiz provides currency) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.small)) {
            listOf(R.string.ui_expense to activity.expense, R.string.ui_income to activity.income,
                R.string.ui_balance to activity.income - activity.expense).forEach { (label, value) ->
                StatisticsMetric(
                    stringResource(label), value, Modifier.weight(1f),
                    background = LedgerStyle.colors.surface, useGrouping = true,
                )
            }
        }
    }
}

/** 流水按当前账户的资金方向显示金额，转入使用目标账户的到账金额。 */
@Composable
private fun AccountActivityTransaction(detail: TransactionDetail, accountId: Long, currency: String, zone: ZoneId, onClick: () -> Unit) {
    val tx = detail.transaction
    val delta = tx.accountDelta(accountId)
    val clock = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val type = if (tx.type != TransactionType.TRANSFER) tx.type else if (tx.toAccountId == accountId) TransactionType.INCOME else TransactionType.EXPENSE
    TransactionRowBiz(
        categoryName = detail.category.category.name,
        timeText = Instant.ofEpochMilli(tx.transactionTime).atZone(zone).format(clock),
        note = tx.note,
        amount = abs(delta),
        type = type,
        icon = detail.category.icon.categoryGlyphBiz(),
        currency = currency,
        primaryCurrency = currency,
        onClick = onClick,
    )
}
