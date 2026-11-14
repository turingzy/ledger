package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.Icon
import androidx.compose.ui.text.style.TextOverflow
import cn.turingzy.ledger.ui.component.MaterialSymbols
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt
import androidx.compose.foundation.Canvas
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
    var selectedIndex by remember(data) { mutableStateOf<Int?>(null) }
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, lineHeight = 12.sp, color = colors.onSurfaceVariant)
    val description = "累计支出对比，$start 至 $end，红色实线为本期，黑色虚线为上期"
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val canvasWidth = with(density) { maxWidth.toPx() }
        val labels = (0..4).map { tick ->
            val amount = BigDecimal.valueOf(data.scale).multiply(BigDecimal.valueOf(tick.toLong())).divide(BigDecimal.valueOf(4), 0, RoundingMode.HALF_UP).longValueExact()
            measurer.measure(expenseAxisLabel(amount), labelStyle)
        }
        val tickIndices = expensePaceTickIndices(data.dates.size, scope)
        val initialTicks = tickIndices.associateWith { index -> measurer.measure(expensePaceDateLabel(scope, data.dates[index]), labelStyle) }
        val initialLabelWidth = initialTicks.values.maxOfOrNull { it.size.width }?.toFloat() ?: 0f
        val gridLeft = labels.maxOf { it.size.width }.toFloat() + with(density) { 8.dp.toPx() }
        val yearLabelScale = if (scope == AnalyticsTimeScope.YEAR && tickIndices.size > 1 && initialLabelWidth > 0f) {
            ((canvasWidth - gridLeft - initialLabelWidth - with(density) { 4.dp.toPx() }) / (tickIndices.size - 1) / initialLabelWidth).coerceIn(0.75f, 1f)
        } else 1f
        val tickLabels = if (yearLabelScale == 1f) initialTicks else tickIndices.associateWith { index ->
            measurer.measure(expensePaceDateLabel(scope, data.dates[index]), labelStyle.copy(fontSize = 10.sp * yearLabelScale))
        }
        val gridRight = canvasWidth
        val left = gridLeft
        val right = gridRight
        fun select(x: Float) {
            if (data.current.isEmpty() || right <= left) return
            selectedIndex = (((x - left) / (right - left)) * (data.dates.size - 1))
                .roundToInt().coerceIn(data.current.indices)
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            val readoutIndex = selectedIndex ?: data.current.lastIndex.takeIf { it >= 0 }
            if (readoutIndex != null) {
                ExpensePaceReadout(data.dates[readoutIndex], data.current[readoutIndex], data.previous[readoutIndex],
                    Modifier.fillMaxWidth().padding(start = with(density) { left.toDp() }, end = with(density) { (canvasWidth - right).toDp() }))
            } else {
                Spacer(Modifier.height(18.dp))
            }
            Canvas(Modifier.fillMaxWidth().height(AnalyticsChartPlotHeight + 30.dp).semantics { contentDescription = description }
                .pointerInput(data, left, right) { detectTapGestures { select(it.x) } }
                .pointerInput(data, left, right) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { select(it.x) },
                        onDrag = { change, _ -> change.consume(); select(change.position.x) },
                    )
                }) {
                val top = 6.dp.toPx()
                val bottom = size.height - 24.dp.toPx()
                fun point(index: Int, value: Long): Offset = Offset(
                    if (data.dates.size == 1) (left + right) / 2 else left + (right - left) * index / (data.dates.size - 1),
                    bottom - (bottom - top) * analyticsRatio(value, data.scale),
                )
                for (tick in 0..AnalyticsChartGridIntervals) {
                    val y = bottom - (bottom - top) * tick / AnalyticsChartGridIntervals
                    drawLine(colors.onSurfaceVariant.copy(alpha = 0.35f), Offset(gridLeft, y), Offset(gridRight, y), 0.7.dp.toPx())
                    if (tick % 2 == 0) drawText(labels[tick / 2], topLeft = Offset(0f, y - labels[tick / 2].size.height / 2))
                }
                val previous = data.previous.mapIndexed { index, value -> point(index, value) }
                val actual = data.current.mapIndexed { index, value -> point(index, value) }
                if (actual.size > 1) {
                    val upper = smoothChartPath(actual)
                    val shade = Path().apply {
                        addPath(upper)
                        lineTo(actual.last().x, bottom)
                        lineTo(actual.first().x, bottom)
                        close()
                    }
                    drawPath(shade, Brush.verticalGradient(listOf(colors.error.copy(alpha = 0.08f), colors.error.copy(alpha = 0.025f))))
                }
                val comparisonColor = colors.onSurface.copy(alpha = 0.65f)
                val dashed = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx())))
                drawPath(smoothChartPath(previous), comparisonColor, style = dashed)
                if (previous.size == 1) drawCircle(comparisonColor, AnalyticsChartNodeRadius.toPx(), previous.first())
                drawPath(smoothChartPath(actual), colors.error, style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round))
                if (actual.size == 1) drawCircle(colors.error, AnalyticsChartNodeRadius.toPx(), actual.first())
                selectedIndex?.let { index ->
                    val currentPoint = point(index, data.current[index])
                    drawLine(colors.onSurface.copy(alpha = 0.6f), Offset(currentPoint.x, top), Offset(currentPoint.x, bottom), 1.dp.toPx())
                    drawCircle(colors.error, AnalyticsChartNodeRadius.toPx(), currentPoint)
                    drawCircle(comparisonColor, AnalyticsChartNodeRadius.toPx(), point(index, data.previous[index]))
                }
                tickLabels.forEach { (index, label) ->
                    val x = (point(index, 0L).x - label.size.width / 2f)
                        .coerceIn(gridLeft, (gridRight - label.size.width).coerceAtLeast(gridLeft))
                    drawText(label, topLeft = Offset(x, bottom + 5.dp.toPx()))
                }
            }
        }
    }

}

@Composable
private fun ExpensePaceReadout(date: LocalDate, current: Long, previous: Long, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val direction = current.compareTo(previous)
    val changeColor = when {
        previous == 0L || direction == 0 -> colors.onSurfaceVariant
        direction > 0 -> colors.error
        else -> colors.tertiary
    }
    Surface(color = analyticsChartInfoColor(), shape = RoundedCornerShape(5.dp),
        modifier = modifier.wrapContentWidth(Alignment.CenterHorizontally).height(18.dp).semantics {
            contentDescription = "$date，本期支出${current.toFormattedAmount(true)}，上期支出${previous.toFormattedAmount(true)}${if (previous > 0L) "，${expensePaceChangeLabel(current, previous)}" else ""}"
        }) {
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${date.monthValue}.${date.dayOfMonth}", fontSize = 10.sp, lineHeight = 12.sp, color = colors.onSurfaceVariant)
            Text("本期 ${current.toFormattedAmount(true)}", fontSize = 10.sp, lineHeight = 12.sp,
                color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("上期 ${previous.toFormattedAmount(true)}", fontSize = 10.sp, lineHeight = 12.sp,
                    color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (previous > 0L) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    if (direction != 0) Icon(
                        if (direction > 0) MaterialSymbols.TrendingUp else MaterialSymbols.TrendingDown,
                        if (direction > 0) "环比增长" else "环比下降", Modifier.size(10.dp), tint = changeColor,
                    )
                    Text(expensePaceChangeLabel(current, previous).removePrefix("增长 ").removePrefix("下降 "),
                        fontSize = 10.sp, lineHeight = 12.sp, color = changeColor, maxLines = 1)
                }
            }
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
