package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.core.animateFloatAsState
import cn.turingzy.ledger.util.toFormattedAmount
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.YearMonth
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
internal fun MonthlyCashflowCard(days: Map<LocalDate, AnalyticsDay>, month: YearMonth, modifier: Modifier = Modifier, transactions: List<AnalyticsTransaction> = emptyList()) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier.fillMaxWidth().padding(top = AnalyticsCardTopSpacing), shape = RoundedCornerShape(20.dp),
        color = colors.surface, border = BorderStroke(0.5.dp, colors.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnalyticsCardTitle("收支与结余", Modifier.weight(1f))
                CashflowLegend("支出", colors.error)
                CashflowLegend("收入", colors.tertiary)
                CashflowLegend("结余", colors.primary, line = true)
            }
            MonthlyCashflow(days, month, transactions)
        }
    }
}

@Composable
private fun CashflowLegend(label: String, color: Color, line: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        if (line) Canvas(Modifier.size(width = 20.dp, height = 11.dp)) {
            drawLine(color, Offset(0f, center.y), Offset(size.width, center.y), 1.5.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(color, 3.dp.toPx(), center)
        } else Box(Modifier.size(9.dp).background(color, RoundedCornerShape(1.dp)))
        Text(label, fontSize = 12.sp, lineHeight = 14.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun MonthlyCashflow(days: Map<LocalDate, AnalyticsDay>, month: YearMonth, transactions: List<AnalyticsTransaction>) {
    val months = remember(days, month) { monthlyAnalytics(days, month) }
    var selectedMonth by remember(month, months) { mutableStateOf<YearMonth?>(null) }
    var showDetails by remember(month, months) { mutableStateOf(false) }
    val focus = months.map { item -> animateFloatAsState(if (selectedMonth == null || selectedMonth == item.month) 1f else 0.22f, label = "month focus").value }
    val colors = MaterialTheme.colorScheme
    val selected = months.firstOrNull { it.month == selectedMonth }
    if (selected != null && showDetails) {
        AnalyticsTransactionSheet("${selected.month.year}年${selected.month.monthValue}月账单",
            "${selected.month.atDay(1)} 至 ${selected.month.atEndOfMonth()}",
            transactions.filter { YearMonth.from(it.date) == selected.month }, { showDetails = false; selectedMonth = null })
    }
    val maximum = months.maxOf { maxOf(it.income, it.expense) }.coerceAtLeast(100L)
    val minimum = minOf(0L, months.minOf { it.balance })
    val scroll = rememberScrollState()
    LaunchedEffect(selectedMonth, scroll.isScrollInProgress, showDetails) {
        if (selectedMonth != null && !scroll.isScrollInProgress && !showDetails) {
            delay(5000)
            selectedMonth = null
        }
    }
    LaunchedEffect(months.size, month) { scroll.scrollTo(scroll.maxValue) }
    BoxWithConstraints(Modifier.fillMaxWidth()
        .chartSelectionBoundary { selectedMonth = null }
        .pointerInput(Unit) { detectTapGestures { selectedMonth = null } },
    ) {
        val viewportWidth = maxWidth
        val slot = viewportWidth / 6
        val slotPx = with(LocalDensity.current) { slot.toPx() }
        val viewportPx = with(LocalDensity.current) { maxWidth.toPx() }
        LaunchedEffect(scroll, slotPx, months) {
            snapshotFlow { scroll.value to scroll.isScrollInProgress }.collect { (offset, moving) ->
                if (moving) {
                    val index = ((offset + viewportPx / 2) / slotPx).toInt().coerceIn(months.indices)
                    selectedMonth = months[index].month
                }
            }
        }
        Box {
            Column(Modifier.horizontalScroll(scroll)) {
                Canvas(Modifier.width(slot * months.size).height(AnalyticsChartPlotHeight + 6.dp).semantics { contentDescription = "收入支出双柱与结余折线组合图" }.pointerInput(months, slotPx) {
                    detectTapGestures { point ->
                        val tappedMonth = months[(point.x / slotPx).toInt().coerceIn(months.indices)].month
                        selectedMonth = tappedMonth.takeUnless { it == selectedMonth }
                    }
                }) {
                    val plotTop = 6.dp.toPx()
                    val plotBottom = size.height
                    fun balanceY(amount: Long): Float {
                        val ratio = BigDecimal.valueOf(amount).subtract(BigDecimal.valueOf(minimum))
                            .divide(BigDecimal.valueOf(maximum).subtract(BigDecimal.valueOf(minimum)), 6, RoundingMode.HALF_UP).toFloat()
                        return plotTop + (plotBottom - plotTop) * (1f - ratio)
                    }
                    val width = size.width / months.size
                    // The signed balance range must not lift the columns off their zero baseline.
                    fun barY(amount: Long) = plotBottom - (plotBottom - plotTop) * analyticsRatio(amount, maximum)
                    for (tick in 0..AnalyticsChartGridIntervals) {
                        val gridY = plotTop + (plotBottom - plotTop) * tick / AnalyticsChartGridIntervals
                        drawLine(colors.onSurfaceVariant.copy(alpha = 0.35f), Offset(0f, gridY), Offset(size.width, gridY), 0.7.dp.toPx())
                    }
                    val points = months.mapIndexed { index, item ->
                        val x = width * (index + 0.5f)
                        val barWidth = width * 0.23f
                        listOf(item.income to colors.tertiary.copy(alpha = 0.4f), item.expense to colors.error.copy(alpha = 0.85f)).forEachIndexed { bar, (amount, color) ->
                            drawRoundRect(color.copy(alpha = color.alpha * focus[index]), Offset(x + (bar - 1) * barWidth, barY(amount)),
                                Size(barWidth, (plotBottom - barY(amount)).coerceAtLeast(0f)), CornerRadius(2.dp.toPx()))
                        }
                        Offset(x, balanceY(item.balance))
                    }
                    points.zipWithNext().forEachIndexed { index, (from, to) ->
                        drawLine(colors.primary.copy(alpha = minOf(focus[index], focus[index + 1])), from, to,
                            1.7.dp.toPx(), cap = StrokeCap.Round)
                    }
                    points.forEachIndexed { index, point ->
                        drawCircle(colors.primary.copy(alpha = focus[index]),
                            (if (months[index].month == selectedMonth) 4.5.dp else AnalyticsChartNodeRadius).toPx(), point)
                    }

                }
                Row {
                    months.forEachIndexed { index, item ->
                        Column(
                            Modifier.width(slot).height(36.dp).clickable { selectedMonth = item.month.takeUnless { it == selectedMonth } }.semantics { contentDescription = "${item.month.year}年${item.month.monthValue}月"; this.selected = selectedMonth == item.month },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("${item.month.monthValue}月", fontSize = 12.sp, lineHeight = 16.sp, color = colors.onSurface)
                            if (index == 0 || item.month.monthValue == 1) Text(item.month.year.toString(), fontSize = 12.sp, lineHeight = 14.sp, color = colors.onSurfaceVariant)
                        }
                    }
                }
            }
            if (selected != null) {
                val summaryWidth = minOf(132.dp, viewportWidth)
                val visibleX = slot * (months.indexOf(selected) + 0.5f) - with(LocalDensity.current) { scroll.value.toDp() }
                val summaryX = (visibleX - summaryWidth / 2).coerceIn(0.dp, viewportWidth - summaryWidth)
                Surface(onClick = { showDetails = true }, shape = RoundedCornerShape(10.dp),
                    color = analyticsChartInfoColor(),
                    modifier = Modifier.offset(x = summaryX, y = 4.dp).width(summaryWidth)
                        .semantics { contentDescription = "${selected.month.year}年${selected.month.monthValue}月收支汇总" },
                ) {
                    Column(Modifier.padding(horizontal = 7.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        Text("${selected.month.year}年${selected.month.monthValue}月", fontSize = 12.sp, lineHeight = 14.sp, color = colors.onSurfaceVariant)
                        Spacer(Modifier.height(3.dp))
                        CashflowSummary("收入", selected.income.toFormattedAmount(true))
                        CashflowSummary("支出", selected.expense.toFormattedAmount(true))
                        CashflowSummary("净结余", selected.balance.toFormattedAmount(true))
                    }
                }
            }
        }
    }
}

@Composable
private fun CashflowSummary(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, Modifier.weight(1f), fontSize = 12.sp, lineHeight = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 12.sp, lineHeight = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
