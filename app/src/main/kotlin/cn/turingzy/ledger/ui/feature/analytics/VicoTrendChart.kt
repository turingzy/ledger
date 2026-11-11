package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.CartesianMeasuringContext
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.CartesianLayerDimensions
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.DashedShape
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt

private data class TrendMetadata(
    val labels: Map<Int, String>,
    val amountUnit: TrendAmountUnit,
    val anomalies: Set<Int>,
    val pointCount: Int,
)

private val TrendMetadataKey = ExtraStore.Key<TrendMetadata>()

@Composable
internal fun VicoTrendChart(
    points: List<AnalyticsTrendPoint>,
    ticks: List<TrendTimeTick>,
    amountUnit: TrendAmountUnit,
    anomalies: Set<Int>,
    showExpense: Boolean,
    showIncome: Boolean,
    showBalance: Boolean,
    modifier: Modifier = Modifier,
) {
    val producer = remember { CartesianChartModelProducer() }
    val metadata = remember(points.size, ticks, amountUnit, anomalies) {
        TrendMetadata(
            ticks.associate { (it.fraction * points.lastIndex.coerceAtLeast(0)).roundToInt() to it.label },
            amountUnit, anomalies, points.size,
        )
    }
    LaunchedEffect(points, metadata) {
        producer.runTransaction {
            if (points.isNotEmpty()) {
                lineModel {
                    series(points.map { it.income - it.expense }, key = "balance")
                    series(points.map { it.income }, key = "income")
                    series(points.map { it.expense }, key = "expense")
                }
            }
            extras { it[TrendMetadataKey] = metadata }
        }
    }
    val colors = MaterialTheme.colorScheme
    val label = rememberAxisLabelComponent(style = TextStyle(color = colors.onSurfaceVariant, fontSize = 10.sp))
    val guideline = rememberAxisGuidelineComponent(
        fill = Fill(colors.outlineVariant), thickness = 1.dp,
        shape = DashedShape(dashLength = 3.dp, gapLength = 4.dp),
    )
    val amountFormatter = remember {
        CartesianValueFormatter { context, coordinate, _ ->
            // Vico exposes drawing coordinates as Double; series and displayed amounts remain integer cents.
            val cents = BigDecimal.valueOf(coordinate).setScale(0, RoundingMode.HALF_UP)
                .coerceIn(BigDecimal.valueOf(Long.MIN_VALUE), BigDecimal.valueOf(Long.MAX_VALUE)).longValueExact()
            (context.model.extraStore.getOrNull(TrendMetadataKey)?.amountUnit ?: TrendAmountUnit.YUAN).format(cents)
        }
    }
    val timeFormatter = remember {
        CartesianValueFormatter { context, coordinate, _ ->
            context.model.extraStore.getOrNull(TrendMetadataKey)?.labels?.get(coordinate.roundToInt()).orEmpty()
        }
    }
    val lines = listOf(
        rememberTrendLine(colors.primary, showBalance),
        rememberTrendLine(colors.tertiary, showIncome),
        rememberTrendLine(colors.error, showExpense, highlightAnomalies = true),
    )
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(lineProvider = LineCartesianLayer.LineProvider.series(lines)),
            endAxis = VerticalAxis.rememberEnd(
                line = null, tick = null, label = label, guideline = guideline,
                valueFormatter = amountFormatter,
                itemPlacer = remember { VerticalAxis.ItemPlacer.count({ 5 }) },
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                line = null, tick = null, label = label, guideline = guideline,
                valueFormatter = timeFormatter,
                itemPlacer = remember { TrendTimeItemPlacer() },
            ),
        ),
        modelProducer = producer,
        modifier = modifier.fillMaxWidth().height(228.dp).semantics {
            contentDescription = "收支走势图，支出、收入和净结余共用金额坐标；圆环标记相对异常支出，共${anomalies.size}处"
        },
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content),
        animationSpec = tween(180),
        initialAnimationSpec = null,
        placeholder = {
            if (points.isEmpty()) Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                Text("这个周期暂无收支记录", color = colors.onSurfaceVariant, fontSize = 13.sp)
            }
        },
    )
}

@Composable
private fun rememberTrendLine(color: Color, visible: Boolean, highlightAnomalies: Boolean = false): LineCartesianLayer.Line {
    val drawColor = if (visible) color else color.copy(alpha = 0f)
    val point = LineCartesianLayer.Point(rememberShapeComponent(Fill(drawColor), CircleShape), 5.dp)
    val ring = LineCartesianLayer.Point(
        rememberShapeComponent(
            Fill(MaterialTheme.colorScheme.surface), CircleShape,
            strokeFill = Fill(drawColor), strokeThickness = 2.dp,
        ),
        10.dp,
    )
    val pointProvider = remember(point, ring, visible, highlightAnomalies) {
        object : LineCartesianLayer.PointProvider {
            override fun getPoint(
                entry: LineCartesianLayerModel.Entry,
                extraStore: ExtraStore,
            ): LineCartesianLayer.Point? {
                if (!visible) return null
                val metadata = extraStore.getOrNull(TrendMetadataKey) ?: return null
                return when {
                    highlightAnomalies && entry.x.roundToInt() in metadata.anomalies -> ring
                    metadata.pointCount <= 12 -> point
                    else -> null
                }
            }

            override fun getLargestPoint(extraStore: ExtraStore) = if (highlightAnomalies) ring else point
        }
    }
    return LineCartesianLayer.rememberLine(
        fill = LineCartesianLayer.LineFill.single(Fill(drawColor)),
        stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 2.dp, cap = StrokeCap.Round),
        pointProvider = pointProvider,
    )
}

private class TrendTimeItemPlacer : HorizontalAxis.ItemPlacer by HorizontalAxis.ItemPlacer.aligned() {
    private fun values(context: CartesianMeasuringContext) =
        context.model.extraStore.getOrNull(TrendMetadataKey)?.labels?.keys.orEmpty().map { it.toDouble() }

    override fun getLabelValues(
        context: CartesianDrawingContext,
        visibleXRange: ClosedFloatingPointRange<Double>,
        fullXRange: ClosedFloatingPointRange<Double>,
        maxLabelWidth: Float,
    ) = values(context)

    override fun getWidthMeasurementLabelValues(
        context: CartesianMeasuringContext,
        layerDimensions: CartesianLayerDimensions,
        fullXRange: ClosedFloatingPointRange<Double>,
    ) = values(context)

    override fun getHeightMeasurementLabelValues(
        context: CartesianMeasuringContext,
        layerDimensions: CartesianLayerDimensions,
        fullXRange: ClosedFloatingPointRange<Double>,
        maxLabelWidth: Float,
    ) = values(context)

    override fun getFirstLabelValue(context: CartesianMeasuringContext, maxLabelWidth: Float) = values(context).firstOrNull()

    override fun getLastLabelValue(context: CartesianMeasuringContext, maxLabelWidth: Float) = values(context).lastOrNull()
}
