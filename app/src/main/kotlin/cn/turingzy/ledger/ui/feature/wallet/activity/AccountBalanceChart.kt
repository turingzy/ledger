package cn.turingzy.ledger.ui.feature.wallet.activity

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.turingzy.ledger.R
import cn.turingzy.ledger.ui.component.base.CenteredEmptyStateBase
import cn.turingzy.ledger.ui.component.base.SurfaceBase
import cn.turingzy.ledger.ui.component.biz.currencySymbolBiz
import cn.turingzy.ledger.ui.feature.analytics.AnalyticsTimeScope
import cn.turingzy.ledger.ui.feature.analytics.component.AnalyticsChartGridIntervals
import cn.turingzy.ledger.ui.feature.analytics.component.AnalyticsChartAxisWidth
import cn.turingzy.ledger.ui.feature.analytics.component.AnalyticsChartNodeRadius
import cn.turingzy.ledger.ui.feature.analytics.component.AnalyticsChartPlotHeight
import cn.turingzy.ledger.ui.feature.analytics.component.DashboardCard
import cn.turingzy.ledger.ui.feature.analytics.component.analyticsChartInfoColor
import cn.turingzy.ledger.ui.feature.analytics.component.drawChartGrid
import cn.turingzy.ledger.ui.feature.analytics.component.expenseAxisLabel
import cn.turingzy.ledger.ui.feature.analytics.component.expensePaceTickIndices
import cn.turingzy.ledger.ui.feature.analytics.component.measureChartAxisLabel
import cn.turingzy.ledger.ui.feature.analytics.component.smoothChartPath
import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.util.toFormattedAmount
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** 显示日结或月结余额，点按和按住拖动共用一个日期读数。 */
@Composable
internal fun AccountBalanceChart(points: List<AccountBalancePoint>, currency: String, yearly: Boolean) {
    val title = stringResource(R.string.wallet_balance_chart)
    DashboardCard(title) {
        val available = points.mapNotNull { it.balance }
        if (available.isEmpty()) {
            CenteredEmptyStateBase(stringResource(R.string.wallet_no_balance_points), Modifier.height(AnalyticsChartPlotHeight))
        } else {
            AccountBalancePlot(title, points, available, currency, yearly)
        }
    }
}

/** 与累计支出图共用平滑路径，余额刻度覆盖负数并只画实际日期。 */
@Composable
private fun AccountBalancePlot(
    title: String,
    points: List<AccountBalancePoint>,
    balances: List<Long>,
    currency: String,
    yearly: Boolean,
) {
    val colors = LedgerStyle.colors
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val labelStyle = LedgerStyle.typography.chartLabel.copy(color = scheme.onSurfaceVariant, lineHeight = 12.sp)
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale, yearly) { DateTimeFormatter.ofPattern(if (yearly) "MMM" else "M.d", locale) }
    val symbol = currencySymbolBiz(currency)
    var selected by remember(points.first().date, yearly) { mutableStateOf<Int?>(null) }
    val low = balances.min().toDouble()
    val high = balances.max().toDouble()
    val padding = ((high - low) * .08).takeIf { it > 0.0 } ?: maxOf(100.0, kotlin.math.abs(high) * .02)
    val axisStart = low - padding
    val axisEnd = high + padding
    val yLabelTexts = (0..AnalyticsChartGridIntervals step 2).map { tick ->
        val value = (axisStart + (axisEnd - axisStart) * tick / AnalyticsChartGridIntervals).roundToLong()
        if (value < 0) "-" + expenseAxisLabel(-value.coerceAtLeast(-Long.MAX_VALUE), symbol)
            else expenseAxisLabel(value, symbol)
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width = with(density) { maxWidth.toPx() }
        // 月视图和年视图共用纵轴宽度，刻度金额变化时绘图区仍保持对齐。
        val left = with(density) { AnalyticsChartAxisWidth.toPx() }.coerceAtMost(width * .40f)
        val right = width.coerceAtLeast(left + 1f)
        val labelWidth = (left - with(density) { LedgerStyle.spacing.tiny.toPx() }).coerceAtLeast(1f)
        val yLabels = yLabelTexts.map { text -> measurer.measureChartAxisLabel(text, labelStyle, labelWidth) }
        val lastAvailable = points.indexOfLast { it.balance != null }
        val chosen = selected?.coerceIn(0, lastAvailable)
        fun select(x: Float) {
            selected = (((x - left) / (right - left)) * points.lastIndex).roundToInt().coerceIn(0, lastAvailable)
        }
        fun xAt(index: Int): Float = left + (right - left) * index / points.lastIndex.coerceAtLeast(1)
        val ticks = expensePaceTickIndices(points.size, if (yearly) AnalyticsTimeScope.YEAR else AnalyticsTimeScope.MONTH)
        val monthSlot = (right - left) / ticks.size.coerceAtLeast(1)
        val tickLabels = ticks.associateWith { index ->
            val text = points[index].date.format(formatter)
            val label = measurer.measure(text, labelStyle)
            if (yearly && label.size.width > monthSlot - 1f) {
                val scale = ((monthSlot - 1f) / label.size.width).coerceIn(.1f, 1f)
                measurer.measure(text, labelStyle.copy(fontSize = labelStyle.fontSize * scale))
            } else label
        }
        Column {
            if (chosen == null) {
                Spacer(Modifier.height(18.dp))
            } else {
                val point = points[chosen]
                SurfaceBase(
                    color = analyticsChartInfoColor(), shape = LedgerStyle.shapes.chartTooltip,
                    modifier = Modifier.fillMaxWidth().padding(start = with(density) { left.toDp() })
                        .wrapContentWidth(Alignment.CenterHorizontally).height(18.dp),
                ) {
                    Row(
                        Modifier.padding(horizontal = LedgerStyle.spacing.extraSmall),
                        horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(point.date.format(formatter), style = labelStyle)
                        Text(requireNotNull(point.balance).toFormattedAmount(true, symbol, useGrouping = true), style = labelStyle.copy(color = scheme.onSurface))
                    }
                }
            }
            Canvas(
                Modifier.fillMaxWidth().height(AnalyticsChartPlotHeight + 30.dp)
                    .semantics { contentDescription = title }
                    .pointerInput(points, left, right) { detectTapGestures { select(it.x) } }
                    .pointerInput(points, left, right) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { select(it.x) },
                            onDrag = { change, _ -> change.consume(); select(change.position.x) },
                        )
                    },
            ) {
                val top = 6.dp.toPx()
                val bottom = size.height - 24.dp.toPx()
                drawChartGrid(top, bottom, left, right, scheme.onSurfaceVariant, yLabels)
                val positions = points.mapIndexedNotNull { index, entry -> entry.balance?.let { value ->
                    val ratio = ((value.toDouble() - axisStart) / (axisEnd - axisStart)).toFloat()
                    Offset(xAt(index), bottom - (bottom - top) * ratio)
                } }
                val line = smoothChartPath(positions)
                val green = colors.income
                if (positions.size > 1) {
                    val fill = Path().apply {
                        addPath(line)
                        lineTo(positions.last().x, bottom)
                        lineTo(positions.first().x, bottom)
                        close()
                    }
                    drawPath(fill, Brush.verticalGradient(listOf(green.copy(alpha = .08f), green.copy(alpha = .025f))))
                    drawPath(line, green, style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round))
                } else {
                    drawCircle(green, AnalyticsChartNodeRadius.toPx(), positions.single())
                }
                if (chosen != null) {
                    val marker = positions[chosen]
                    drawLine(scheme.onSurface.copy(alpha = .60f), Offset(marker.x, top), Offset(marker.x, bottom), 1.dp.toPx())
                    drawCircle(green, AnalyticsChartNodeRadius.toPx(), marker)
                }
                val gap = 4.dp.toPx()
                var nextLeft = right + gap
                ticks.asReversed().forEach { index ->
                    val label = tickLabels.getValue(index)
                    val center = if (yearly) left + (index + .5f) * monthSlot else xAt(index)
                    val x = (center - label.size.width / 2f).coerceIn(left, (right - label.size.width).coerceAtLeast(left))
                    if (yearly || x + label.size.width + gap <= nextLeft) {
                        drawText(label, topLeft = Offset(x, bottom + 5.dp.toPx()))
                        nextLeft = x
                    }
                }
            }
        }
    }
}
