package cn.turingzy.ledger.ui.feature.analytics

import cn.turingzy.ledger.ui.component.LedgerSegmentedControl
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import cn.turingzy.ledger.ui.component.MaterialSymbols
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.util.toFormattedAmount
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun TrendLineChartCard(
    points: List<AnalyticsTrendPoint>,
    periodStart: LocalDate,
    periodEnd: LocalDate,
    scope: AnalyticsTimeScope,
    modifier: Modifier = Modifier,
) {
    var showExpense by rememberSaveable { mutableStateOf(true) }
    var showIncome by rememberSaveable { mutableStateOf(true) }
    var showBalance by rememberSaveable { mutableStateOf(true) }
    val scale = remember(points) { trendValueScale(points) }
    val amountUnit = remember(scale) { trendAmountUnit(scale) }
    val ticks = remember(points, periodStart, periodEnd, scope) {
        trendTimeTicks(points, periodStart, periodEnd, scope)
    }
    val anomalies = remember(points) { expenseAnomalyIndices(points) }
    val colors = MaterialTheme.colorScheme
    AnalyticsChartCard(modifier, contentSpacing = 10.dp) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnalyticsCardTitle("收支走势")
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TrendLegend("支出", colors.error, showExpense) { showExpense = it }
                TrendLegend("收入", colors.tertiary, showIncome) { showIncome = it }
                TrendLegend("净结余", colors.primary, showBalance) { showBalance = it }
            }
        }
        VicoTrendChart(
            points = points,
            ticks = ticks,
            amountUnit = amountUnit,
            anomalies = anomalies,
            showExpense = showExpense,
            showIncome = showIncome,
            showBalance = showBalance,
        )
    }
}

@Composable
private fun TrendLegend(
    label: String,
    color: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val alpha = if (checked) 1f else 0.25f
    val surface = MaterialTheme.colorScheme.surface
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(36.dp)
                .clip(CircleShape)
                .semantics { contentDescription = label }
                .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.width(20.dp).height(2.dp).background(color.copy(alpha = alpha)))
            Box(
                Modifier.size(8.dp).clip(CircleShape).background(surface)
                    .border(2.dp, color.copy(alpha = alpha), CircleShape),
            )
        }
        Text(
            label,
            fontSize = 11.sp,
            color = if (checked) color else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
internal fun CategoryBreakdownCard(
    uiState: AnalyticsUiState.Success,
    onCategoryTypeChange: (CategoryType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expandedId by remember(
        uiState.categoryType,
        uiState.periodStart,
        uiState.timeScope
    ) { mutableStateOf<Long?>(null) }
    val colors = MaterialTheme.colorScheme
    val palette = listOf(
        colors.primary,
        colors.tertiary,
        colors.error,
        colors.secondary,
        colors.tertiaryContainer
    )
    val total =
        if (uiState.categoryType == CategoryType.EXPENSE) uiState.totalExpense else uiState.totalIncome
    val selected = uiState.rankings.find { it.categoryId == expandedId }
    AnalyticsChartCard(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnalyticsCardTitle("分类占比")
            LedgerSegmentedControl(
                labels = listOf("支出", "收入"),
                selectedIndex = if (uiState.categoryType == CategoryType.EXPENSE) 0 else 1,
                onSelect = { onCategoryTypeChange(if (it == 0) CategoryType.EXPENSE else CategoryType.INCOME) },
                modifier = Modifier.width(96.dp),
                height = 28.dp,
                fontSize = 11.sp,
                emphasizeSelection = false,
            )
        }
        if (uiState.rankings.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                Text(
                    "这个周期暂无分类记录",
                    color = colors.onSurfaceVariant
                )
            }
        } else {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    selected?.categoryName ?: if (uiState.categoryType == CategoryType.EXPENSE) "总支出" else "总收入",
                    modifier = Modifier.weight(1f),
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    (selected?.totalAmount ?: total).toFormattedAmount(true),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            VicoCategoryChart(
                rankings = uiState.rankings,
                palette = palette,
                selectedId = selected?.takeIf { it.totalAmount > 0L }?.categoryId,
                onSelect = { expandedId = it },
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).semantics { contentDescription = "分类列表" },
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                uiState.rankings.forEachIndexed { index, ranking ->
                    val expanded = expandedId == ranking.categoryId
                    item(key = "category-${ranking.categoryId}") {
                        Row(
                            Modifier.fillMaxWidth()
                                .semantics { stateDescription = if (expanded) "已展开" else "已收起" }
                                .clickable(
                                    role = Role.Button,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClickLabel = if (expanded) "收起分类明细" else "展开分类明细",
                                ) { expandedId = if (expanded) null else ranking.categoryId }
                                .padding(start = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(Modifier.size(8.dp).background(palette[index % palette.size], CircleShape))
                            Text(
                                "${ranking.categoryName}（${percent(ranking.percentage)}）",
                                Modifier.weight(1f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                ranking.totalAmount.toFormattedAmount(true),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                            Icon(
                                imageVector = if (expanded) MaterialSymbols.ExpandMore else MaterialSymbols.ChevronRight,
                                contentDescription = if (expanded) "收起${ranking.categoryName}明细" else "展开${ranking.categoryName}明细",
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    if (expanded) {
                        items(ranking.transactions, key = { "transaction-${it.id}" }) { transaction ->
                            AnalyticsTransactionRow(
                                transaction = transaction,
                                showYear = uiState.periodStart.year != uiState.periodEnd.year,
                                modifier = Modifier.padding(start = 16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsTransactionRow(
    transaction: AnalyticsTransaction,
    showYear: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(5.dp).background(colors.onSurface, CircleShape))
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                transaction.accountName,
                Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                transaction.date.format(DateTimeFormatter.ofPattern(if (showYear) "yyyy.MM.dd" else "MM.dd")),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurface,
                maxLines = 1,
            )
        }
        Text(
            transaction.amount.toFormattedAmount(true),
            modifier = Modifier.width(96.dp),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = colors.onSurface,
            maxLines = 1,
        )
    }
}

private fun percent(ratio: Float): String =
    String.format(Locale.getDefault(), "%.1f%%", ratio * 100)

@Composable
internal fun AnalyticsCardTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        modifier = modifier,
        fontFamily = FontFamily.SansSerif,
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
internal fun AnalyticsChartCard(
    modifier: Modifier = Modifier,
    contentSpacing: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(contentSpacing),
            content = content
        )
    }
}
