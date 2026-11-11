package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import com.patrykandpatrick.vico.compose.pie.PieChart
import com.patrykandpatrick.vico.compose.pie.PieChartHost
import com.patrykandpatrick.vico.compose.pie.PieSize
import com.patrykandpatrick.vico.compose.pie.data.PieChartModelProducer
import com.patrykandpatrick.vico.compose.pie.data.pieModel
import com.patrykandpatrick.vico.compose.pie.rememberPieChart
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.min

private val CategorySegmentsKey = ExtraStore.Key<List<CategoryStackSegment>>()

@Composable
internal fun VicoCategoryChart(
    rankings: List<CategoryExpenseRanking>,
    palette: List<Color>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    require(palette.isNotEmpty())
    val producer = remember { PieChartModelProducer() }
    val segments = remember(rankings) { categoryStackSegments(rankings) }
    val currentOnSelect = rememberUpdatedState(onSelect)
    LaunchedEffect(rankings, segments) {
        producer.runTransaction {
            if (segments.isNotEmpty()) pieModel { series(rankings.filter { it.totalAmount > 0L }.map { it.totalAmount }) }
            extras { it[CategorySegmentsKey] = segments }
        }
    }
    val activeSlices = remember(palette) { palette.map { PieChart.Slice(fill = Fill(it)) } }
    val mutedSlices = remember(palette) { palette.map { PieChart.Slice(fill = Fill(it.copy(alpha = 0.3f))) } }
    val slices = remember(activeSlices, mutedSlices, selectedId) {
        PieChart.SliceProvider { _, index, extraStore ->
            val segment = extraStore.getOrNull(CategorySegmentsKey)?.getOrNull(index)
            val colorIndex = (segment?.colorIndex ?: index) % activeSlices.size
            val active = selectedId == null || selectedId == segment?.categoryId
            if (active) activeSlices[colorIndex] else mutedSlices[colorIndex]
        }
    }
    PieChartHost(
        chart = rememberPieChart(
            sliceProvider = slices,
            outerSize = PieSize.Outer.fixed(160.dp),
            innerSize = PieSize.Inner.fixed(96.dp),
        ),
        modelProducer = producer,
        modifier = modifier.fillMaxWidth().height(160.dp)
            .semantics { contentDescription = "分类占比环形图，点击分类展开明细" }
            .pointerInput(segments, selectedId) {
                detectTapGestures { position ->
                    val x = position.x - size.width / 2f
                    val y = position.y - size.height / 2f
                    val outerRadius = min(size.width, size.height) / 2f
                    val innerRadius = 48.dp.toPx()
                    val distanceSquared = x * x + y * y
                    if (distanceSquared < innerRadius * innerRadius || distanceSquared > outerRadius * outerRadius) return@detectTapGestures
                    val angle = (atan2(y, x) + PI / 2 + 2 * PI) % (2 * PI)
                    val fraction = (angle / (2 * PI)).toFloat()
                    val hit = segments.firstOrNull { fraction >= it.start && fraction < it.end } ?: return@detectTapGestures
                    currentOnSelect.value(if (selectedId == hit.categoryId) null else hit.categoryId)
                }
            },
        animationSpec = tween(180),
        animateIn = false,
    )
}
