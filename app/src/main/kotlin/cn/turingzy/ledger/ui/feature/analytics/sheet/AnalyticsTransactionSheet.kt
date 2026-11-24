package cn.turingzy.ledger.ui.feature.analytics.sheet

import cn.turingzy.ledger.ui.feature.analytics.AnalyticsTransaction
import cn.turingzy.ledger.ui.feature.analytics.component.AnalyticsCardTitle
import cn.turingzy.ledger.ui.component.biz.TransactionGroupHeaderBiz
import cn.turingzy.ledger.ui.component.biz.TransactionGroupRowBiz
import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.ui.component.base.ModalBottomSheetBase
import cn.turingzy.ledger.ui.component.biz.LocalCurrencyBiz
import cn.turingzy.ledger.ui.component.biz.currencySymbolBiz
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.model.transaction.TransactionType
import cn.turingzy.ledger.ui.component.base.BottomSheetHeaderBase
import kotlinx.coroutines.launch
import cn.turingzy.ledger.util.toFormattedAmount
import java.time.format.DateTimeFormatter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import cn.turingzy.ledger.R
import androidx.hilt.navigation.compose.hiltViewModel
import cn.turingzy.ledger.ui.component.navigation.LocalBottomBarContentPadding
import cn.turingzy.ledger.ui.feature.analytics.AnalyticsTransactionRows
import cn.turingzy.ledger.ui.feature.analytics.LocalAnalyticsBillViewModel
import cn.turingzy.ledger.ui.feature.bill.detail.TransactionDetailsScreen
import cn.turingzy.ledger.ui.feature.bill.detail.TransactionDetailsViewModel

/**
 * 统计分析交易明细底部抽屉。
 *
 * 用于在点击统计卡片中的分类或月份时，弹出展示对应筛选条件下的交易列表及分组汇总。
 *
 * @param title 抽屉标题（如分类名称或月份）。
 * @param subtitle 抽屉副标题（如统计日期范围）。
 * @param transactions 关联的交易列表。
 * @param onDismiss 关闭抽屉时的回调。
 * @param groupByDate 是否按日期分组展示（默认为 true；为 false 时按金额降序单组展示）。
 * @param summaryType 仅汇总展示特定交易类型（支出/收入）；传 null 则自动展示有金额的类型。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AnalyticsTransactionSheet(
    title: String,
    subtitle: String?,
    transactions: List<AnalyticsTransaction>,
    onDismiss: () -> Unit,
    groupByDate: Boolean = true,
    summaryType: TransactionType? = null,
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val height = LocalConfiguration.current.screenHeightDp.dp * 2 / 3
    val colors = MaterialTheme.colorScheme
    val groups = remember(transactions, groupByDate) {
        if (groupByDate) transactions.groupBy { it.date }.toSortedMap(reverseOrder()).entries.map { it.key to it.value }
        else listOf(null to transactions.sortedByDescending { it.amount })
    }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val shortDateFormatter = remember { DateTimeFormatter.ofPattern("MM.dd") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val billViewModel = LocalAnalyticsBillViewModel.current
    val transactionViewModel: TransactionDetailsViewModel? = if (billViewModel != null) hiltViewModel() else null
    ModalBottomSheetBase(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(Modifier.fillMaxWidth().height(height).testTag("analyticsTransactionDrawer")) {
            BottomSheetHeaderBase(
                stringResource(R.string.ui_close_transactions),
                onClose = {
                    scope.launch {
                        sheetState.hide()
                        if (!sheetState.isVisible) onDismiss()
                    }
                },
            ) {
                AnalyticsCardTitle(title)
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.small)) {
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                Text(
                    analyticsTransactionSummary(transactions, summaryType, showTotalLabel = true),
                    Modifier.weight(1f),
                    style = LedgerStyle.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (transactions.isEmpty()) {
                Text(textResources.getString(R.string.ui_no_transactions), Modifier.padding(LedgerStyle.spacing.extraLarge), color = colors.onSurfaceVariant)
            } else Box(Modifier.fillMaxWidth().weight(1f)) {
                AnalyticsTransactionRows(
                    transactions = transactions,
                    timeText = { entry ->
                        val time = entry.time?.format(timeFormatter)
                        if (groupByDate) time.orEmpty()
                        else listOfNotNull(entry.date.format(shortDateFormatter), time).joinToString(" ")
                    },
                    viewModel = billViewModel,
                    transactionViewModel = transactionViewModel,
                    snackbarHostState = snackbarHostState,
                ) { transactionRow ->
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = LedgerStyle.spacing.pageHorizontal,
                            end = LedgerStyle.spacing.pageHorizontal,
                            bottom = LedgerStyle.spacing.sectionGap + LocalBottomBarContentPadding.current,
                        ),
                    ) {
                        groups.forEachIndexed { groupIndex, (date, entries) ->
                            if (date != null) item(key = "date:$date") {
                                TransactionGroupHeaderBiz(
                                    date,
                                    analyticsTransactionSummary(entries, summaryType),
                                    Modifier.padding(
                                        top = if (groupIndex == 0) LedgerStyle.spacing.none else LedgerStyle.spacing.sectionGap,
                                        bottom = LedgerStyle.spacing.medium,
                                    ),
                                )
                            }
                            itemsIndexed(entries, key = { _, entry -> entry.id }) { index, entry ->
                                val first = index == 0
                                val last = index == entries.lastIndex
                                TransactionGroupRowBiz(first, last) {
                                    transactionRow(entry, first, last)
                                }
                            }
                        }
                    }
                }
                SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
            }
            if (transactionViewModel != null) TransactionDetailsScreen(
                onLoadFailure = { message -> scope.launch { snackbarHostState.showSnackbar(message) } },
                viewModel = transactionViewModel,
            )
        }
    }
}

/**
 * 统计交易列表的收支总额汇总文本。
 *
 * @param transactions 交易列表。
 * @param summaryType 指定过滤汇总的交易类型。
 * @param showTotalLabel 是否在标签前追加“总”（例如“总支出” vs “支出”）。
 */
@Composable
private fun analyticsTransactionSummary(
    transactions: List<AnalyticsTransaction>,
    summaryType: TransactionType?,
    showTotalLabel: Boolean = false,
): String {
    val currencySymbol = currencySymbolBiz(LocalCurrencyBiz.current)
    val expense = remember(transactions) { transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.primaryAmount } }
    val income = remember(transactions) { transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.primaryAmount } }
    val showExpense = summaryType == TransactionType.EXPENSE || (summaryType == null && (expense > 0L || income == 0L))
    val showIncome = summaryType == TransactionType.INCOME || (summaryType == null && income > 0L)
    val expenseLabel = stringResource(if (showTotalLabel) R.string.ui_total_expenses else R.string.ui_expense)
    val incomeLabel = stringResource(if (showTotalLabel) R.string.ui_total_income else R.string.ui_income)
    return buildList {
        if (showExpense) add("$expenseLabel ${expense.toFormattedAmount(true, symbol = currencySymbol)}")
        if (showIncome) add("$incomeLabel ${income.toFormattedAmount(true, symbol = currencySymbol)}")
    }.joinToString("  ")
}
