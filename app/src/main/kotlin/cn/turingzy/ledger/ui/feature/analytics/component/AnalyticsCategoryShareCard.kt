package cn.turingzy.ledger.ui.feature.analytics.component

import cn.turingzy.ledger.ui.feature.analytics.AnalyticsUiState
import cn.turingzy.ledger.ui.feature.analytics.calculation.analyticsRatio
import cn.turingzy.ledger.ui.feature.analytics.sheet.AnalyticsTransactionSheet
import cn.turingzy.ledger.ui.component.biz.LocalCurrencyBiz
import cn.turingzy.ledger.ui.component.biz.currencySymbolBiz
import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import cn.turingzy.ledger.ui.component.picker.CalendarSelection
import cn.turingzy.ledger.ui.interaction.uiClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.constrainHeight
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.turingzy.ledger.model.category.CategoryType
import cn.turingzy.ledger.ui.component.input.SegmentedControl
import cn.turingzy.ledger.util.toFormattedAmount
import cn.turingzy.ledger.ui.theme.LedgerStyle
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 分类占比卡片。
 *
 * 展示当前周期的分类收支占比情况，包含支出/收入切换器、交互式分段占比进度条、
 * 分类图例流动标签，以及点击分类弹出查看其明细账单列表的功能。
 *
 * @param state 统计分析成功状态数据。
 * @param onCategoryTypeChange 切换分类类型（支出/收入）回调。
 * @param modifier 修饰符。
 */
@Composable
internal fun AnalyticsCategoryShareCard(
    state: AnalyticsUiState.Success,
    onCategoryTypeChange: (CategoryType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val currencySymbol = currencySymbolBiz(LocalCurrencyBiz.current)
    val colors = MaterialTheme.colorScheme
    val income = state.categoryType == CategoryType.INCOME
    val rankings = remember(state.rankings) { state.rankings.sortedByDescending { it.totalAmount } }
    val total = rankings.sumOf { it.totalAmount }
    val periodTotal = if (income) state.totalIncome else state.totalExpense
    val palette = remember(rankings.size, colors.tertiary, colors.surface, colors.onSurfaceVariant) { categoryShareColors(rankings.size, colors.tertiary, colors.surface, colors.onSurfaceVariant) }
    var sheetId by remember(state.categoryType, state.periodStart, state.periodEnd, rankings) { mutableStateOf<Long?>(null) }
    var selectedBarId by remember(state.categoryType, state.periodStart, state.periodEnd, rankings) { mutableStateOf<Long?>(null) }
    val selectedBarCategory = rankings.firstOrNull { it.categoryId == selectedBarId }
    val segmentAlphas = rankings.map { category ->
        animateFloatAsState(
            when (selectedBarId) {
                null -> 1f
                category.categoryId -> 0.88f
                else -> 0.38f
            },
            animationSpec = tween(180), label = "Category segment emphasis",
        )
    }
    val segmentEnds = remember(rankings, total) {
        var accumulated = 0L
        rankings.map { item ->
            accumulated += item.totalAmount
            analyticsRatio(accumulated, total)
        }
    }
    fun selectBarCategory(id: Long) {
        selectedBarId = id.takeUnless { it == selectedBarId }
    }
    DashboardCard(
        title = textResources.getString(cn.turingzy.ledger.R.string.ui_category_breakdown),
        modifier = modifier,
        contentSpacing = LedgerStyle.spacing.extraLarge,
        headerActions = {
            SegmentedControl(
                listOf(textResources.getString(cn.turingzy.ledger.R.string.ui_expense), textResources.getString(cn.turingzy.ledger.R.string.ui_income)), if (income) 1 else 0,
                { onCategoryTypeChange(if (it == 1) CategoryType.INCOME else CategoryType.EXPENSE) },
                Modifier.width(88.dp), height = 26.dp, fontSize = LedgerStyle.typography.label.fontSize, emphasizeSelection = false,
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.extraSmall)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val totalLabel = if (income) cn.turingzy.ledger.R.string.ui_total_income else cn.turingzy.ledger.R.string.ui_total_expenses
                Text(selectedBarCategory?.categoryName ?: textResources.getString(totalLabel), Modifier.weight(1f),
                    fontSize = LedgerStyle.typography.bodyMedium.fontSize, lineHeight = 18.sp,
                    color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                AnalyticsAmountText(selectedBarCategory?.totalAmount ?: periodTotal, textAlign = TextAlign.End)
            }
            Canvas(Modifier.fillMaxWidth().height(40.dp)
                .pointerInput(rankings, segmentEnds) {
                    val barInset = 6.dp.toPx()
                    detectTapGestures { position ->
                        if (size.width == 0 || position.y !in barInset..(size.height - barInset)) return@detectTapGestures
                        val fraction = position.x / size.width
                        rankings.getOrNull(segmentEnds.indexOfFirst { fraction < it })?.let { selectBarCategory(it.categoryId) }
                    }
                }
                .semantics {
                    contentDescription = "${if (income) "收入" else "支出"}分类占比条，总额${periodTotal.toFormattedAmount(true, symbol = currencySymbol)}"
                    customActions = rankings.map { category ->
                        CustomAccessibilityAction(category.categoryName) {
                            selectBarCategory(category.categoryId)
                            true
                        }
                    }
                }) {
                val barTop = 6.dp.toPx()
                val barHeight = size.height - barTop * 2
                val track = Rect(0f, barTop, size.width, barTop + barHeight)
                val trackPath = Path().apply { addRoundRect(RoundRect(track, CornerRadius(5.dp.toPx()))) }
                clipPath(trackPath) {
                    drawRect(colors.surfaceVariant, track.topLeft, track.size)
                    var left = 0f
                    segmentEnds.forEachIndexed { index, end ->
                        val right = end * size.width
                        drawRect(palette[index], Offset(left, barTop), Size(right - left, barHeight), alpha = segmentAlphas[index].value)
                        left = right
                    }
                }

            }
        }
        if (rankings.isEmpty()) {
            Text(if (income) textResources.getString(cn.turingzy.ledger.R.string.ui_no_income) else textResources.getString(cn.turingzy.ledger.R.string.ui_no_expenses), Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        } else CategoryLegendLayout(Modifier.fillMaxWidth()) {
            rankings.forEachIndexed { index, item ->
                key(item.categoryId) {
                    Row(Modifier.semantics { selected = sheetId == item.categoryId || selectedBarId == item.categoryId }
                        .clip(LedgerStyle.shapes.bankCard)
                        .background(if (sheetId == item.categoryId || selectedBarId == item.categoryId) analyticsChartInfoColor() else Color.Transparent)
                        .uiClickable { sheetId = item.categoryId }
                        .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Box(Modifier.size(8.dp).background(palette[index], CircleShape))
                        Text(buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Medium)) { append(item.categoryName) }
                            append(" ")
                            withStyle(SpanStyle(color = colors.onSurfaceVariant, fontSize = LedgerStyle.typography.label.fontSize)) {
                                append(categorySharePercentage(item.totalAmount, total))
                            }
                        }, fontSize = LedgerStyle.typography.bodySmall.fontSize, lineHeight = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
    rankings.firstOrNull { it.categoryId == sheetId }?.let { category ->
        AnalyticsTransactionSheet(category.categoryName, CalendarSelection(state.periodStart, state.periodEnd).displayRange(),
            category.transactions, { sheetId = null }, groupByDate = false,
            summaryType = if (income) cn.turingzy.ledger.model.transaction.TransactionType.INCOME else cn.turingzy.ledger.model.transaction.TransactionType.EXPENSE)
    }
}

/**
 * 自定义流式换行的分类图例布局组件。
 *
 * @param modifier 修饰符。
 * @param content 子可组合组件项。
 */
@Composable
private fun CategoryLegendLayout(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val horizontalSpacing = 16.dp.roundToPx()
        val verticalSpacing = 4.dp.roundToPx()
        val itemConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        var x = 0
        var y = 0
        var rowHeight = 0
        val items = measurables.map { measurable ->
            val placeable = measurable.measure(itemConstraints)
            if (x > 0 && x + placeable.width > constraints.maxWidth) {
                x = 0
                y += rowHeight + verticalSpacing
                rowHeight = 0
            }
            val position = IntOffset(x, y)
            x += placeable.width + horizontalSpacing
            rowHeight = maxOf(rowHeight, placeable.height)
            placeable to position
        }
        layout(constraints.maxWidth, constraints.constrainHeight(y + rowHeight)) {
            items.forEach { (placeable, position) -> placeable.placeRelative(position.x, position.y) }
        }
    }
}

/**
 * 格式化分类金额在总额中的百分比显示文本。
 *
 * @param amount 分类金额（分）。
 * @param total 总金额（分）。
 * @return 格式化后的百分比字符串（如 "25%" 或 "12.34%"）。
 */
internal fun categorySharePercentage(amount: Long, total: Long): String =
    if (total <= 0L) "0%" else BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%"

/**
 * 为多个分类动态生成辨识度高的图表配色序列。
 *
 * 采用黄金分割角度分布 HSV 色相，并在靠后分类中采用柔和中性色过渡。
 *
 * @param count 分类数量。
 * @param seed 主题种子基准色。
 * @param surface 界面表面背景色（用于明暗适配）。
 * @param neutral 中性色基准。
 * @return 颜色列表。
 */
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
