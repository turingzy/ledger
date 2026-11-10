package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
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
    val expenseAlpha by animateFloatAsState(if (showExpense) 1f else 0f, label = "expenseVisibility")
    val incomeAlpha by animateFloatAsState(if (showIncome) 1f else 0f, label = "incomeVisibility")
    val balanceAlpha by animateFloatAsState(if (showBalance) 1f else 0f, label = "balanceVisibility")
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
            Text("收支走势", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TrendLegend("支出", colors.error, showExpense) { showExpense = it }
                TrendLegend("收入", colors.tertiary, showIncome) { showIncome = it }
                TrendLegend("净结余", colors.primary, showBalance) { showBalance = it }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Canvas(
                        Modifier.fillMaxWidth().height(208.dp).semantics {
                            contentDescription = "收支走势图，支出、收入和净结余共用金额坐标；圆环标记相对异常支出，共${anomalies.size}处"
                        },
                    ) {
                        val inset = 10.dp.toPx()
                        val width = (size.width - inset - 4.dp.toPx()).coerceAtLeast(0f)
                        val height = (size.height - inset * 2).coerceAtLeast(0f)
                        fun y(amount: Long) = inset + height * (1 - scale.fraction(amount))
                        fun x(index: Int) = inset + width * if (points.size == 1) 0.5f else
                            index.toFloat() / points.lastIndex.coerceAtLeast(1)
                        repeat(5) { index ->
                            val gridY = inset + height * index / 4
                            drawLine(
                                colors.outlineVariant, Offset(inset, gridY), Offset(inset + width, gridY),
                                strokeWidth = 1.dp.toPx(),
                            )
                        }
                        ticks.forEach { tick ->
                            val gridX = inset + width * tick.fraction
                            drawLine(
                                colors.outlineVariant,
                                Offset(gridX, inset), Offset(gridX, inset + height),
                                strokeWidth = 1.dp.toPx(),
                            )
                        }
                        drawLine(
                            colors.outlineVariant,
                            Offset(inset, y(0L)), Offset(inset + width, y(0L)),
                            strokeWidth = 1.dp.toPx(),
                        )
                        fun drawSeries(amounts: List<Long>, color: Color, alpha: Float) {
                            if (alpha <= 0f || amounts.isEmpty()) return
                            val path = Path().apply {
                                amounts.forEachIndexed { index, amount ->
                                    if (index == 0) moveTo(x(index), y(amount)) else lineTo(x(index), y(amount))
                                }
                            }
                            drawPath(
                                path, color.copy(alpha = alpha),
                                style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                            )
                            amounts.forEachIndexed { index, amount ->
                                if (amounts.size <= 12) drawCircle(color.copy(alpha = alpha), 2.5.dp.toPx(), Offset(x(index), y(amount)))
                            }
                        }
                        drawSeries(points.map { it.income - it.expense }, colors.primary, balanceAlpha)
                        drawSeries(points.map { it.income }, colors.tertiary, incomeAlpha)
                        drawSeries(points.map { it.expense }, colors.error, expenseAlpha)
                        if (expenseAlpha > 0f) {
                            anomalies.forEach { index ->
                                val center = Offset(x(index), y(points[index].expense))
                                drawCircle(colors.error.copy(alpha = 0.12f * expenseAlpha), 9.dp.toPx(), center)
                                drawCircle(colors.surface.copy(alpha = expenseAlpha), 5.dp.toPx(), center)
                                drawCircle(colors.error.copy(alpha = expenseAlpha), 5.dp.toPx(), center, style = Stroke(2.dp.toPx()))
                            }
                        }
                    }
                    TrendTimeAxis(ticks)
                }
                TrendAmountAxis(scale, amountUnit)
            }
        }
    }
}

@Composable
private fun TrendAmountAxis(scale: TrendValueScale, unit: TrendAmountUnit) {
    val amounts = remember(scale) { listOf(0L) + scale.ticks.filter { it != 0L } }
    val inset = with(LocalDensity.current) { 10.dp.toPx() }
    Layout(
        content = {
            amounts.forEach { amount ->
                Text(
                    if (amount == 0L) "0 ${unit.label}" else unit.format(amount),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        },
        modifier = Modifier.height(208.dp),
    ) { measurables, constraints ->
        val labels = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val width = labels.maxOf { it.width }.coerceIn(constraints.minWidth, constraints.maxWidth)
        layout(width, constraints.maxHeight) {
            val height = constraints.maxHeight - inset * 2
            val zeroCenter = inset + height * (1 - scale.fraction(0L))
            labels.forEachIndexed { index, label ->
                val center = inset + height * (1 - scale.fraction(amounts[index]))
                if (index == 0 || kotlin.math.abs(center - zeroCenter) >= (label.height + labels.first().height) / 2f) {
                    label.placeRelative(width - label.width, (center - label.height / 2).toInt())
                }
            }
        }
    }
}

@Composable
private fun TrendTimeAxis(ticks: List<TrendTimeTick>) {
    val inset = with(LocalDensity.current) { 10.dp.roundToPx() }
    Layout(
        content = {
            ticks.forEach { tick ->
                Text(tick.label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        },
        modifier = Modifier.fillMaxWidth().height(20.dp),
    ) { measurables, constraints ->
        val labels = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            labels.forEachIndexed { index, label ->
                val rightInset = 4.dp.toPx()
                val center = inset + (constraints.maxWidth - inset - rightInset) * ticks[index].fraction
                val left = (center - label.width / 2).toInt().coerceIn(0, (constraints.maxWidth - label.width).coerceAtLeast(0))
                label.placeRelative(left, 0)
            }
        }
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
        Canvas(
            Modifier.size(36.dp)
                .clip(CircleShape)
                .semantics { contentDescription = label }
                .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange),
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            drawLine(
                color.copy(alpha = alpha),
                Offset(8.dp.toPx(), center.y), Offset(size.width - 8.dp.toPx(), center.y),
                strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round,
            )
            drawCircle(surface, 4.dp.toPx(), center)
            drawCircle(color.copy(alpha = alpha), 4.dp.toPx(), center, style = Stroke(2.dp.toPx()))
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
            Text("分类占比", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            AnalyticsSegmentedControl(
                labels = listOf("支出", "收入"),
                selectedIndex = if (uiState.categoryType == CategoryType.EXPENSE) 0 else 1,
                onSelect = { onCategoryTypeChange(if (it == 0) CategoryType.EXPENSE else CategoryType.INCOME) },
                modifier = Modifier.width(112.dp),
                height = 32.dp,
                fontSize = 12.sp,
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
            val segments = remember(uiState.rankings) { categoryStackSegments(uiState.rankings) }
            Canvas(
                Modifier.fillMaxWidth().height(28.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .semantics { contentDescription = "分类占比堆叠条，点击色块展开对应分类明细" }
                    .pointerInput(segments) {
                        detectTapGestures { position ->
                            val fraction = position.x / size.width
                            val hit = segments.firstOrNull { fraction >= it.start && fraction < it.end }
                            expandedId = if (expandedId == hit?.categoryId) null else hit?.categoryId
                        }
                    },
            ) {
                drawRect(colors.surfaceVariant)
                segments.forEach { segment ->
                    val active = expandedId == null || expandedId == segment.categoryId
                    drawRect(
                        color = palette[segment.colorIndex % palette.size].copy(alpha = if (active) 1f else 0.3f),
                        topLeft = Offset(size.width * segment.start, 0f),
                        size = Size(size.width * (segment.end - segment.start), size.height),
                    )
                }
                segments.zipWithNext().forEach { (left, right) ->
                    val separatorWidth = minOf(
                        1.dp.toPx(),
                        size.width * (left.end - left.start) / 3,
                        size.width * (right.end - right.start) / 3,
                    )
                    val x = size.width * left.end
                    drawLine(colors.surface, Offset(x, 0f), Offset(x, size.height), strokeWidth = separatorWidth)
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).semantics { contentDescription = "分类列表" },
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                uiState.rankings.forEachIndexed { index, ranking ->
                    val expanded = expandedId == ranking.categoryId
                    item(key = "category-${ranking.categoryId}") {
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                .semantics { stateDescription = if (expanded) "已展开" else "已收起" }
                                .clickable(
                                    role = Role.Button,
                                    onClickLabel = if (expanded) "收起分类明细" else "展开分类明细",
                                ) { expandedId = if (expanded) null else ranking.categoryId }
                                .padding(start = 4.dp, top = 10.dp, bottom = 10.dp),
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
                        }
                    }
                    if (expanded) {
                        items(ranking.transactions, key = { "transaction-${it.id}" }) { transaction ->
                            AnalyticsTransactionRow(
                                transaction = transaction,
                                showYear = uiState.periodStart.year != uiState.periodEnd.year,
                                modifier = Modifier.padding(start = 24.dp),
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
        modifier.fillMaxWidth().padding(vertical = 10.dp),
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
