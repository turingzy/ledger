package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
internal fun MonthlyCashflowCard(days: Map<LocalDate, AnalyticsDay>, month: YearMonth, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier.fillMaxWidth().padding(top = AnalyticsCardTopSpacing), shape = RoundedCornerShape(20.dp),
        color = colors.surface, border = BorderStroke(0.5.dp, colors.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("收支与结余", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                CashflowLegend("支出", colors.error)
                CashflowLegend("收入", colors.tertiary)
                CashflowLegend("结余", colors.primary, line = true)
            }
            MonthlyCashflow(days, month)
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
private fun MonthlyCashflow(days: Map<LocalDate, AnalyticsDay>, month: YearMonth) {
    val months = remember(days, month) { monthlyAnalytics(days, month) }
    val colors = MaterialTheme.colorScheme
    val maximum = months.maxOf { maxOf(it.income, it.expense) }.coerceAtLeast(100L)
    val minimum = minOf(0L, months.minOf { it.balance })
    val scroll = rememberScrollState()
    LaunchedEffect(months.size, month) { scroll.scrollTo(scroll.maxValue) }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val slot = maxWidth / 6
        Column(Modifier.horizontalScroll(scroll)) {
            Canvas(Modifier.width(slot * months.size).height(180.dp).semantics { contentDescription = "收入支出双柱与结余折线组合图" }) {
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
                for (tick in 0..4) {
                    val gridY = plotTop + (plotBottom - plotTop) * tick / 4
                    drawLine(colors.onSurfaceVariant.copy(alpha = 0.35f), Offset(0f, gridY), Offset(size.width, gridY), 0.7.dp.toPx())
                }
                val points = months.mapIndexed { index, item ->
                    val x = width * (index + 0.5f)
                    val barWidth = width * 0.23f
                    listOf(item.income to colors.tertiary.copy(alpha = 0.4f), item.expense to colors.error.copy(alpha = 0.85f)).forEachIndexed { bar, (amount, color) ->
                        drawRoundRect(color, Offset(x + (bar - 1) * barWidth, barY(amount)),
                            Size(barWidth, (plotBottom - barY(amount)).coerceAtLeast(0f)), CornerRadius(2.dp.toPx()))
                    }
                    Offset(x, balanceY(item.balance))
                }
                val balanceLine = Path().apply {
                    points.firstOrNull()?.let { moveTo(it.x, it.y) }
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(balanceLine, colors.primary, style = Stroke(1.7.dp.toPx(), cap = StrokeCap.Round))
                points.forEach { drawCircle(colors.primary, 2.5.dp.toPx(), it) }

            }
            Row {
                months.forEachIndexed { index, item ->
                    Column(
                        Modifier.width(slot).height(30.dp).semantics { contentDescription = "${item.month.year}年${item.month.monthValue}月" },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("${item.month.monthValue}月", fontSize = 12.sp, lineHeight = 16.sp, color = colors.onSurface)
                        if (index == 0 || item.month.monthValue == 1) Text(item.month.year.toString(), fontSize = 9.sp, lineHeight = 11.sp, color = colors.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
