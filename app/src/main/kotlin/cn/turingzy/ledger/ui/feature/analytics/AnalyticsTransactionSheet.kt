package cn.turingzy.ledger.ui.feature.analytics

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
import cn.turingzy.ledger.ui.component.LedgerIconButton
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.TransactionRow
import cn.turingzy.ledger.ui.component.categoryGlyph
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
) {
    val height = LocalConfiguration.current.screenHeightDp.dp * 2 / 3
    val colors = MaterialTheme.colorScheme
    val groups = remember(transactions, groupByDate) {
        if (groupByDate) transactions.groupBy { it.date }.toSortedMap(reverseOrder()).entries.map { it.key to it.value }
        else listOf(null to transactions.sortedByDescending { it.amount })
    }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MM/dd E", Locale.CHINESE) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val shortDateFormatter = remember { DateTimeFormatter.ofPattern("MM.dd") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(Modifier.fillMaxWidth().height(height).testTag("analyticsTransactionDrawer").navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                LedgerIconButton(showPressFeedback = false, onClick = onDismiss) {
                    Icon(MaterialSymbols.Close, "关闭交易明细", tint = colors.onSurfaceVariant)
                }
            }
            HorizontalDivider(color = colors.outlineVariant, thickness = 0.5.dp)
            Text(subtitle, Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            if (transactions.isEmpty()) Text("暂无交易记录", Modifier.padding(16.dp), color = colors.onSurfaceVariant)
            else LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                groups.forEach { (date, entries) ->
                    if (date != null) item(key = "date:$date") {
                        Text(date.format(dateFormatter), Modifier.padding(start = 4.dp, top = 8.dp, bottom = 7.dp),
                            fontSize = 13.sp, lineHeight = 18.sp, color = colors.onSurfaceVariant)
                    }
                    itemsIndexed(entries, key = { _, entry -> entry.id }) { index, entry ->
                        val categoryName = entry.category?.category?.name ?: entry.note
                        val supportingText = listOfNotNull(entry.accountName, entry.note.takeIf { it.isNotBlank() && it != categoryName }).joinToString(" · ")
                        val topRadius = if (index == 0) 20.dp else 0.dp
                        val bottomRadius = if (index == entries.lastIndex) 20.dp else 0.dp
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(
                            topStart = topRadius, topEnd = topRadius, bottomStart = bottomRadius, bottomEnd = bottomRadius,
                        )).background(colors.surface)) {
                            if (index > 0) HorizontalDivider(Modifier.padding(start = 62.dp, end = 14.dp),
                                thickness = 0.5.dp, color = colors.outlineVariant)
                            TransactionRow(
                                categoryName = categoryName,
                                supportingText = supportingText,
                                timeText = if (groupByDate) entry.time?.format(timeFormatter).orEmpty()
                                else listOfNotNull(entry.date.format(shortDateFormatter), entry.time?.format(timeFormatter)).joinToString(" "),
                                amount = entry.amount,
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
