package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
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
    val palette = remember(rankings.size, colors.tertiary, colors.surface, colors.onSurfaceVariant) { categoryShareColors(rankings.size, colors.tertiary, colors.surface, colors.onSurfaceVariant) }
    var sheetId by remember(state.categoryType, state.periodStart, state.periodEnd, rankings) { mutableStateOf<Long?>(null) }
    Surface(
        modifier.fillMaxWidth().padding(top = AnalyticsCardTopSpacing), shape = RoundedCornerShape(20.dp),
        color = colors.surface, border = BorderStroke(0.5.dp, colors.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnalyticsCardTitle("分类占比", Modifier.weight(1f))
                LedgerSegmentedControl(
                    listOf("支出", "收入"), if (income) 1 else 0,
                    { onCategoryTypeChange(if (it == 1) CategoryType.INCOME else CategoryType.EXPENSE) },
                    Modifier.width(88.dp), height = 26.dp, fontSize = 12.sp, emphasizeSelection = false,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (income) "总收入" else "总支出", Modifier.weight(1f),
                        fontSize = 14.sp, lineHeight = 18.sp, color = colors.onSurfaceVariant)
                    AnalyticsAmountText(periodTotal, textAlign = TextAlign.End)
                }
                Canvas(Modifier.fillMaxWidth().height(40.dp).semantics {
                    contentDescription = "${if (income) "收入" else "支出"}分类占比条，总额${periodTotal.toFormattedAmount(true)}"
                }) {
                    val barTop = 6.dp.toPx()
                    val barHeight = size.height - barTop * 2
                    val track = Rect(0f, barTop, size.width, barTop + barHeight)
                    val trackPath = Path().apply { addRoundRect(RoundRect(track, CornerRadius(5.dp.toPx()))) }
                    var accumulated = 0L
                    val segments = rankings.map { item ->
                        val left = analyticsRatio(accumulated, total) * size.width
                        accumulated += item.totalAmount
                        left to analyticsRatio(accumulated, total) * size.width
                    }
                    clipPath(trackPath) {
                        drawRect(colors.surfaceVariant, track.topLeft, track.size)
                        segments.forEachIndexed { index, (left, right) ->
                            drawRect(palette[index], Offset(left, barTop), Size(right - left, barHeight))
                        }
                    }

                }
            }
            if (rankings.isEmpty()) {
                Text(if (income) "暂无收入记录" else "暂无支出记录", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            } else Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                rankings.forEachIndexed { index, item ->
                    key(item.categoryId) {
                        Row(Modifier.fillMaxWidth().semantics { selected = sheetId == item.categoryId }
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (sheetId == item.categoryId) colors.primary.copy(alpha = 0.08f) else Color.Transparent)
                            .clickable { sheetId = item.categoryId }
                            .padding(horizontal = 6.dp, vertical = 3.dp),
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
    rankings.firstOrNull { it.categoryId == sheetId }?.let { category ->
        AnalyticsTransactionSheet(category.categoryName, "${state.periodStart} 至 ${state.periodEnd}",
            category.transactions, { sheetId = null }, groupByDate = false)
    }
}

internal fun categorySharePercentage(amount: Long, total: Long): String =
    if (total <= 0L) "0%" else BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%"

internal fun categoryShareColors(
    count: Int, seed: Color, surface: Color,
    neutral: Color = Color(seed.luminance(), seed.luminance(), seed.luminance()),
): List<Color> {
    val maximum = maxOf(seed.red, seed.green, seed.blue)
    val minimum = minOf(seed.red, seed.green, seed.blue)
    val difference = maximum - minimum
    val hue = if (difference == 0f) 0f else when (maximum) {
        seed.red -> 60f * ((seed.green - seed.blue) / difference % 6f)
        seed.green -> 60f * ((seed.blue - seed.red) / difference + 2f)
        else -> 60f * ((seed.red - seed.green) / difference + 4f)
    }
    val saturation = if (maximum == 0f) 0.65f else (difference / maximum).coerceIn(0.55f, 0.75f)
    val coloredCount = (count * 2 + 2) / 3
    return List(count) { index ->
        if (index >= coloredCount) {
            val progress = (index - coloredCount).toFloat() / (count - coloredCount - 1).coerceAtLeast(1)
            return@List lerp(neutral, surface, 0.08f + progress * 0.64f)
        }
        Color.hsv(((hue + index * 137.50776f) % 360f + 360f) % 360f, saturation,
            if (surface.luminance() < 0.5f) 0.92f else 0.78f)
    }
}
