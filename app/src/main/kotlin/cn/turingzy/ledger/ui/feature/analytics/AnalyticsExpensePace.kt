package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.turingzy.ledger.util.toFormattedAmount
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Composable
internal fun ExpensePace(days: Map<LocalDate, AnalyticsDay>, scope: AnalyticsTimeScope, start: LocalDate, end: LocalDate, today: LocalDate) {
    val data = remember(days, scope, start, end, today) { expensePaceData(days, scope, start, end, today) }
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, lineHeight = 12.sp, color = colors.onSurfaceVariant)
    val description = "累计支出对比，$start 至 $end，红色实线为本期，黑色虚线为上期"
    Canvas(Modifier.fillMaxWidth().height(200.dp).semantics { contentDescription = description }) {
        val labels = (0..4).map { tick ->
            val amount = BigDecimal.valueOf(data.scale).multiply(BigDecimal.valueOf(tick.toLong())).divide(BigDecimal.valueOf(4), 0, RoundingMode.HALF_UP).longValueExact()
            measurer.measure(expenseAxisLabel(amount), labelStyle)
        }
        val tickIndices = expensePaceTickIndices(data.dates.size, scope)
        val initialTicks = tickIndices.associateWith { index -> measurer.measure(expensePaceDateLabel(scope, data.dates[index]), labelStyle) }
        val initialLabelWidth = initialTicks.values.maxOfOrNull { it.size.width }?.toFloat() ?: 0f
        val gridLeft = labels.maxOf { it.size.width }.toFloat() + 8.dp.toPx()
        val yearLabelScale = if (scope == AnalyticsTimeScope.YEAR && tickIndices.size > 1 && initialLabelWidth > 0f) {
            ((size.width - gridLeft - initialLabelWidth - 4.dp.toPx()) / (tickIndices.size - 1) / initialLabelWidth).coerceIn(0.75f, 1f)
        } else 1f
        val tickLabels = if (yearLabelScale == 1f) initialTicks else tickIndices.associateWith { index ->
            measurer.measure(expensePaceDateLabel(scope, data.dates[index]), labelStyle.copy(fontSize = 10.sp * yearLabelScale))
        }
        val horizontalInset = (tickLabels.values.maxOfOrNull { it.size.width } ?: 0) / 2f
        val gridRight = size.width
        val left = gridLeft + horizontalInset
        val right = gridRight - horizontalInset - 4.dp.toPx()
        val top = 8.dp.toPx()
        val bottom = size.height - 24.dp.toPx()
        fun point(index: Int, value: Long): Offset = Offset(
            if (data.dates.size == 1) (left + right) / 2 else left + (right - left) * index / (data.dates.size - 1),
            bottom - (bottom - top) * analyticsRatio(value, data.scale),
        )
        for (tick in 0..4) {
            val y = bottom - (bottom - top) * tick / 4
            drawLine(colors.onSurfaceVariant.copy(alpha = 0.35f), Offset(gridLeft, y), Offset(gridRight, y), 0.7.dp.toPx())
            drawText(labels[tick], topLeft = Offset(0f, y - labels[tick].size.height / 2))
        }
        val previous = data.previous.mapIndexed { index, value -> point(index, value) }
        val actual = data.current.mapIndexed { index, value -> point(index, value) }
        if (actual.size > 1) {
            val upper = smoothChartPath(actual)
            val lower = previous.take(actual.size).asReversed()
            // Follow both curves so the shaded area never extends down to the axis.
            val shade = Path().apply {
                addPath(upper)
                lineTo(lower.first().x, lower.first().y)
                lower.zipWithNext().forEach { (from, to) ->
                    val x = (from.x + to.x) / 2
                    cubicTo(x, from.y, x, to.y, to.x, to.y)
                }
                close()
            }
            drawPath(shade, Brush.verticalGradient(listOf(colors.error.copy(alpha = 0.08f), colors.error.copy(alpha = 0.025f))))
        }
        val dashed = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())))
        drawPath(smoothChartPath(previous), colors.onSurface, style = dashed)
        if (previous.size == 1) drawCircle(colors.onSurface, 2.dp.toPx(), previous.first())
        drawPath(smoothChartPath(actual), colors.error, style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round))
        if (actual.size == 1) drawCircle(colors.error, 2.dp.toPx(), actual.first())
        tickLabels.forEach { (index, label) ->
            val x = point(index, 0L).x - label.size.width / 2f
            drawText(label, topLeft = Offset(x, bottom + 5.dp.toPx()))
        }
    }
}

internal fun smoothChartPath(points: List<Offset>): Path = Path().apply {
    if (points.isNotEmpty()) moveTo(points.first().x, points.first().y)
    points.zipWithNext().forEach { (from, to) ->
        val middle = (from.x + to.x) / 2
        cubicTo(middle, from.y, middle, to.y, to.x, to.y)
    }
}

internal fun expenseAxisLabel(cents: Long): String {
    if (cents < 100_000L) return cents.toFormattedAmount(showSymbol = true).removeSuffix(".00")
    val thousands = BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100_000L), 2, RoundingMode.HALF_UP)
    return "¥${thousands.stripTrailingZeros().toPlainString()}k"
}

internal fun expensePaceTickIndices(count: Int, scope: AnalyticsTimeScope? = null): List<Int> {
    if (count <= 0) return emptyList()
    if (scope == AnalyticsTimeScope.YEAR || count <= 7) return (0 until count).toList()
    val minimumStep = (count - 1 + 4) / 5
    val step = listOf(1, 2, 3, 5, 7, 10).firstOrNull { it >= minimumStep } ?: ((minimumStep + 9) / 10 * 10)
    return (0 until count step step).toList()
}

internal fun expensePaceDateLabel(scope: AnalyticsTimeScope, date: LocalDate): String = when (scope) {
    AnalyticsTimeScope.WEEK -> listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")[date.dayOfWeek.value - 1]
    AnalyticsTimeScope.YEAR -> "${date.monthValue}月"
    AnalyticsTimeScope.ALL -> date.year.toString()
    else -> "${date.monthValue}.${date.dayOfMonth}"
}
