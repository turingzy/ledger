package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.ui.component.LedgerSegmentedControl
import cn.turingzy.ledger.util.toFormattedAmount
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.atan2
import kotlin.math.sqrt

@Composable
internal fun AnalyticsCategoryShareCard(
    state: AnalyticsUiState.Success,
    onCategoryTypeChange: (CategoryType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val income = state.categoryType == CategoryType.INCOME
    val rankings = state.rankings
    val total = rankings.sumOf { it.totalAmount }
    val periodTotal = if (income) state.totalIncome else state.totalExpense
    val palette = remember(rankings.size, colors.tertiary, colors.surface) { categoryShareColors(rankings.size, colors.tertiary, colors.surface) }
    var selectedId by remember(state.categoryType, state.periodStart, state.periodEnd, rankings) { mutableStateOf<Long?>(null) }
    val listState = rememberLazyListState()
    LaunchedEffect(state.categoryType, state.periodStart, state.periodEnd) { listState.scrollToItem(0) }
    LaunchedEffect(selectedId) {
        val index = rankings.indexOfFirst { it.categoryId == selectedId }
        if (index >= 0) listState.animateScrollToItem(index)
    }
    Surface(
        modifier.fillMaxWidth().padding(top = AnalyticsCardTopSpacing), shape = RoundedCornerShape(20.dp),
        color = colors.surface, border = BorderStroke(0.5.dp, colors.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("分类占比", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                LedgerSegmentedControl(
                    listOf("支出", "收入"), if (income) 1 else 0,
                    { onCategoryTypeChange(if (it == 1) CategoryType.INCOME else CategoryType.EXPENSE) },
                    Modifier.width(88.dp), height = 26.dp, fontSize = 12.sp, emphasizeSelection = false,
                )
            }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.size(188.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize().semantics {
                        contentDescription = "${if (income) "收入" else "支出"}分类环形图，总额${periodTotal.toFormattedAmount(true)}"
                    }.pointerInput(rankings) {
                        detectTapGestures { point ->
                            val radius = minOf(size.width, size.height) / 2f - 13.dp.toPx()
                            val dx = point.x - size.width / 2f
                            val dy = point.y - size.height / 2f
                            if (sqrt(dx * dx + dy * dy) !in (radius - 12.dp.toPx())..(radius + 10.dp.toPx())) return@detectTapGestures
                            val angle = (Math.toDegrees(atan2(dy, dx).toDouble()).toFloat() + 450f) % 360f
                            var start = 0f
                            rankings.forEach { item ->
                                val sweep = analyticsRatio(item.totalAmount, total) * 360f
                                if (angle >= start && angle < start + sweep) selectedId = if (selectedId == item.categoryId) null else item.categoryId
                                start += sweep
                            }
                        }
                    }) {
                        val radius = size.minDimension / 2f - 13.dp.toPx()
                        val origin = center - Offset(radius, radius)
                        if (total == 0L) drawArc(colors.surfaceVariant, 0f, 360f, false, origin, Size(radius * 2, radius * 2), style = Stroke(20.dp.toPx()))
                        var start = -90f
                        rankings.forEachIndexed { index, item ->
                            val sweep = analyticsRatio(item.totalAmount, total) * 360f
                            drawArc(palette[index].copy(alpha = if (selectedId == null || selectedId == item.categoryId) 1f else 0.3f),
                                start, sweep, false, origin, Size(radius * 2, radius * 2),
                                style = Stroke(20.dp.toPx()))
                            start += sweep
                        }
                    }
                    Column(Modifier.width(132.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(if (income) "总收入" else "总支出", fontSize = 12.sp, lineHeight = 15.sp, color = colors.onSurfaceVariant)
                        AnalyticsAmountText(periodTotal, textAlign = TextAlign.Center)
                    }
                }
            }
            if (rankings.isEmpty()) {
                Text(if (income) "暂无收入记录" else "暂无支出记录", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            } else LazyColumn(
                Modifier.fillMaxWidth().heightIn(max = 180.dp), state = listState,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                itemsIndexed(rankings, key = { _, item -> item.categoryId }) { index, item ->
                    Row(Modifier.fillMaxWidth().semantics { selected = selectedId == item.categoryId }
                        .clickable { selectedId = if (selectedId == item.categoryId) null else item.categoryId }
                        .padding(vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(Modifier.size(8.dp).background(palette[index], CircleShape))
                        Text(buildAnnotatedString {
                            append(item.categoryName)
                            append(" ")
                            withStyle(SpanStyle(color = colors.onSurfaceVariant, fontSize = 12.sp)) {
                                append(categorySharePercentage(item.totalAmount, total))
                            }
                        }, Modifier.weight(1f), fontSize = 13.sp, lineHeight = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(item.totalAmount.toFormattedAmount(true), Modifier.widthIn(min = 60.dp, max = 88.dp),
                            fontSize = 13.sp, lineHeight = 18.sp, textAlign = TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

internal fun categorySharePercentage(amount: Long, total: Long): String =
    if (total <= 0L) "0%" else BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%"

internal fun categoryShareColors(count: Int, seed: Color, surface: Color): List<Color> {
    val maximum = maxOf(seed.red, seed.green, seed.blue)
    val minimum = minOf(seed.red, seed.green, seed.blue)
    val difference = maximum - minimum
    val hue = if (difference == 0f) 0f else when (maximum) {
        seed.red -> 60f * ((seed.green - seed.blue) / difference % 6f)
        seed.green -> 60f * ((seed.blue - seed.red) / difference + 2f)
        else -> 60f * ((seed.red - seed.green) / difference + 4f)
    }
    val saturation = if (maximum == 0f) 0.65f else (difference / maximum).coerceIn(0.55f, 0.75f)
    return List(count) { index ->
        Color.hsv(((hue + index * 137.50776f) % 360f + 360f) % 360f, saturation,
            if (surface.luminance() < 0.5f) 0.92f else 0.78f)
    }
}
