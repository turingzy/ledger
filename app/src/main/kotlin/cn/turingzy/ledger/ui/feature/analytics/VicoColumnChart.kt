package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt

private data class ColumnMetadata(val labels: List<String>, val unit: TrendAmountUnit)
private val ColumnMetadataKey = ExtraStore.Key<ColumnMetadata>()

@Composable
internal fun VicoColumnChart(
    dataSeries: List<List<Long>>,
    labels: List<String>,
    seriesColors: List<Color>,
    description: String,
    modifier: Modifier = Modifier,
    colorByCategory: Boolean = false,
) {
    require(seriesColors.isNotEmpty())
    require(dataSeries.all { it.size == labels.size })
    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(dataSeries, labels) {
        producer.runTransaction {
            val amounts = dataSeries.flatten()
            val unit = trendAmountUnit(TrendValueScale(amounts.minOrNull() ?: 0L, amounts.maxOrNull() ?: 0L))
            if (labels.isNotEmpty() && dataSeries.isNotEmpty()) {
                columnModel {
                    dataSeries.forEachIndexed { index, values -> series(values, key = index) }
                }
            }
            extras { it[ColumnMetadataKey] = ColumnMetadata(labels, unit) }
        }
    }
    val columns = seriesColors.map {
        rememberLineComponent(Fill(it), thickness = 18.dp, shape = RoundedCornerShape(4.dp))
    }
    val provider = remember(columns, colorByCategory) {
        if (!colorByCategory) ColumnCartesianLayer.ColumnProvider.series(columns)
        else object : ColumnCartesianLayer.ColumnProvider {
            override fun getColumn(entry: ColumnCartesianLayerModel.Entry, extraStore: ExtraStore) =
                columns[Math.floorMod(entry.x.roundToInt(), columns.size)]

            override fun getWidestSeriesColumn(seriesKey: Any, seriesIndex: Int, extraStore: ExtraStore) = columns.first()
        }
    }
    val colors = MaterialTheme.colorScheme
    val axisLabel = rememberAxisLabelComponent(style = TextStyle(color = colors.onSurfaceVariant, fontSize = 10.sp))
    val amountFormatter = remember {
        CartesianValueFormatter { context, coordinate, _ ->
            val cents = BigDecimal.valueOf(coordinate).setScale(0, RoundingMode.HALF_UP)
                .coerceIn(BigDecimal.valueOf(Long.MIN_VALUE), BigDecimal.valueOf(Long.MAX_VALUE)).longValueExact()
            (context.model.extraStore.getOrNull(ColumnMetadataKey)?.unit ?: TrendAmountUnit.YUAN).format(cents)
        }
    }
    val categoryFormatter = remember {
        CartesianValueFormatter { context, coordinate, _ ->
            context.model.extraStore.getOrNull(ColumnMetadataKey)?.labels?.getOrNull(coordinate.roundToInt()).orEmpty()
        }
    }
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(columnProvider = provider),
            endAxis = VerticalAxis.rememberEnd(
                line = null, tick = null, label = axisLabel,
                guideline = rememberAxisGuidelineComponent(fill = Fill(colors.outlineVariant)),
                valueFormatter = amountFormatter,
                itemPlacer = remember { VerticalAxis.ItemPlacer.count({ 4 }) },
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                line = null, tick = null, label = axisLabel, guideline = null,
                valueFormatter = categoryFormatter,
            ),
        ),
        modelProducer = producer,
        modifier = modifier.fillMaxWidth().height(180.dp).semantics { contentDescription = description },
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content),
        animationSpec = tween(180),
        initialAnimationSpec = null,
    )
}
