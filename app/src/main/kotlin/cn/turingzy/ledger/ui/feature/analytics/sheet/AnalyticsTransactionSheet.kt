package cn.turingzy.ledger.ui.feature.analytics.sheet

import cn.turingzy.ledger.ui.feature.analytics.AnalyticsTransaction
import cn.turingzy.ledger.ui.component.biz.BusinessTransactionRow
import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.ui.component.base.BaseModalBottomSheet
import cn.turingzy.ledger.ui.component.biz.LocalLedgerCurrency
import cn.turingzy.ledger.ui.component.biz.currencySymbol
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.turingzy.ledger.model.transaction.TransactionType
import cn.turingzy.ledger.ui.component.base.BaseIconButton
import cn.turingzy.ledger.ui.component.base.BaseDivider
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.biz.categoryGlyph
import cn.turingzy.ledger.util.toFormattedAmount
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AnalyticsTransactionSheet(
    title: String,
    subtitle: String,
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
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MM/dd E", Locale.CHINESE) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val shortDateFormatter = remember { DateTimeFormatter.ofPattern("MM.dd") }
    BaseModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.fillMaxWidth().height(height).testTag("analyticsTransactionDrawer")) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                BaseIconButton(MaterialSymbols.Close, textResources.getString(cn.turingzy.ledger.R.string.ui_close_transaction_details),
                    onDismiss, showPressFeedback = false)
            }
            BaseDivider()
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.large)) {
                Text(subtitle, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                AnalyticsTransactionTotals(transactions, summaryType)
            }
            if (transactions.isEmpty()) Text(textResources.getString(cn.turingzy.ledger.R.string.ui_no_transactions), Modifier.padding(LedgerStyle.spacing.extraLarge), color = colors.onSurfaceVariant)
            else LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                groups.forEach { (date, entries) ->
                    if (date != null) item(key = "date:$date") {
                        Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp, bottom = 7.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.large)) {
                            Text(date.format(dateFormatter), Modifier.weight(1f), fontSize = LedgerStyle.typography.bodySmall.fontSize, lineHeight = 18.sp, color = colors.onSurfaceVariant)
                            AnalyticsTransactionTotals(entries, summaryType)
                        }
                    }
                    itemsIndexed(entries, key = { _, entry -> entry.id }) { index, entry ->
                        val categoryName = entry.category?.category?.name ?: entry.note
                        val supportingText = listOfNotNull(entry.accountName, entry.note.takeIf { it.isNotBlank() && it != categoryName }).joinToString(" · ")
                        val topRadius = if (index == 0) 20.dp else 0.dp
                        val bottomRadius = if (index == entries.lastIndex) 20.dp else 0.dp
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(
                            topStart = topRadius, topEnd = topRadius, bottomStart = bottomRadius, bottomEnd = bottomRadius,
                        )).background(colors.surface)) {
                            if (index > 0) BaseDivider(Modifier.padding(start = 62.dp, end = 14.dp))
                            BusinessTransactionRow(
                                categoryName = categoryName,
                                supportingText = supportingText,
                                timeText = if (groupByDate) entry.time?.format(timeFormatter).orEmpty()
                                else listOfNotNull(entry.date.format(shortDateFormatter), entry.time?.format(timeFormatter)).joinToString(" "),
                                amount = entry.amount,
                                currency = entry.currency,
                                type = entry.type,
                                icon = entry.category?.icon?.categoryGlyph() ?: MaterialSymbols.Payments,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsTransactionTotals(transactions: List<AnalyticsTransaction>, summaryType: TransactionType?) {
    val currencySymbol = currencySymbol(LocalLedgerCurrency.current)
    val expense = remember(transactions) { transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.primaryAmount } }
    val income = remember(transactions) { transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.primaryAmount } }
    val showExpense = summaryType == TransactionType.EXPENSE || (summaryType == null && (expense > 0L || income == 0L))
    val showIncome = summaryType == TransactionType.INCOME || (summaryType == null && income > 0L)
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.tiny)) {
        if (showExpense) Text("总支出 ${expense.toFormattedAmount(true, symbol = currencySymbol)}", fontSize = LedgerStyle.typography.label.fontSize, lineHeight = 16.sp,
            color = MaterialTheme.colorScheme.onSurface)
        if (showIncome) Text("总收入 ${income.toFormattedAmount(true, symbol = currencySymbol)}", fontSize = LedgerStyle.typography.label.fontSize, lineHeight = 16.sp,
            color = MaterialTheme.colorScheme.onSurface)
    }
}
