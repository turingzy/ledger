package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.util.toFormattedAmount
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.roundToInt

@Composable
internal fun TrendLineChartCard(
    points: List<AnalyticsTrendPoint>,
    scope: AnalyticsTimeScope,
    modifier: Modifier = Modifier
) {
    var showExpense by rememberSaveable { mutableStateOf(true) }
    var showIncome by rememberSaveable { mutableStateOf(true) }
    var selectedIndex by remember(points) { mutableIntStateOf((points.size - 1).coerceAtLeast(0)) }
    val expenseAlpha by animateFloatAsState(
        if (showExpense) 1f else 0f,
        label = "expenseVisibility"
    )
    val incomeAlpha by animateFloatAsState(if (showIncome) 1f else 0f, label = "incomeVisibility")
    val selected = points.getOrNull(selectedIndex)
    val maximum = points.maxOfOrNull { maxOf(it.expense, it.income) }?.coerceAtLeast(100L) ?: 100L
    val hasData = points.any { it.expense > 0L || it.income > 0L }
    val colors = MaterialTheme.colorScheme
    AnalyticsChartCard(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("收支走势", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TrendLegend("支出", colors.error, showExpense) { showExpense = it }
                TrendLegend("收入", colors.tertiary, showIncome) { showIncome = it }
            }
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceVariant.copy(alpha = 0.4f)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                selected?.date?.format(DateTimeFormatter.ofPattern(if (scope == AnalyticsTimeScope.YEAR) "yyyy年M月" else "yyyy年M月d日"))
                    ?: "所选周期", color = colors.onSurfaceVariant, fontSize = 12.sp
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (showExpense) Text(
                    "支出 ${(selected?.expense ?: 0L).toFormattedAmount(true)}",
                    color = colors.error,
                    fontSize = 13.sp
                )
                if (showIncome) Text(
                    "收入 ${(selected?.income ?: 0L).toFormattedAmount(true)}",
                    color = colors.tertiary,
                    fontSize = 13.sp
                )
                if (!showExpense && !showIncome) Text(
                    "点击上方图例显示折线",
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariant
                )
            }
        }
        if (!hasData || (!showExpense && !showIncome)) {
            Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                Text(
                    if (hasData) "折线已隐藏" else "这个周期暂无收支记录",
                    color = colors.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        } else {
            Row(
                Modifier.fillMaxWidth().height(180.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Canvas(
                    Modifier.weight(1f).fillMaxHeight()
                        .semantics {
                            contentDescription = "收支趋势图，点击或左右滑动查看日期对应的收支金额"
                        }
                        .pointerInput(points) {
                            detectTapGestures { position ->
                                selectedIndex =
                                    ((position.x - 6.dp.toPx()) / (size.width - 12.dp.toPx()) * (points.size - 1)).roundToInt()
                                        .coerceIn(points.indices)
                            }
                        }
                        .pointerInput(points) {
                            detectHorizontalDragGestures { change, _ ->
                                change.consume()
                                selectedIndex =
                                    ((change.position.x - 6.dp.toPx()) / (size.width - 12.dp.toPx()) * (points.size - 1)).roundToInt()
                                        .coerceIn(points.indices)
                            }
                        },
                ) {
                    val inset = 6.dp.toPx()
                    val width = size.width - inset * 2
                    val height = size.height - inset * 2
                    repeat(5) { index ->
                        val y = inset + height * index / 4
                        drawLine(
                            colors.outline.copy(alpha = 0.25f),
                            Offset(inset, y),
                            Offset(inset + width, y),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                        )
                    }
                    fun drawSeries(amounts: List<Long>, color: Color, alpha: Float) {
                        if (alpha <= 0f) return
                        val positions = amounts.mapIndexed { index, amount ->
                            Offset(
                                inset + width * index / (points.size - 1).coerceAtLeast(1),
                                inset + height * (1 - analyticsRatio(amount, maximum))
                            )
                        }
                        val path = Path().apply {
                            positions.forEachIndexed { index, point ->
                                if (index == 0) moveTo(point.x, point.y) else {
                                    val previous = positions[index - 1]
                                    val midX = (previous.x + point.x) / 2
                                    cubicTo(midX, previous.y, midX, point.y, point.x, point.y)
                                }
                            }
                        }
                        val fill = Path().apply {
                            addPath(path)
                            lineTo(positions.last().x, inset + height)
                            lineTo(positions.first().x, inset + height)
                            close()
                        }
                        drawPath(
                            fill,
                            Brush.verticalGradient(
                                listOf(
                                    color.copy(alpha = 0.16f * alpha),
                                    color.copy(alpha = 0f)
                                )
                            )
                        )
                        drawPath(
                            path,
                            color.copy(alpha = alpha),
                            style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                        val point = positions[selectedIndex]
                        drawCircle(colors.surface, 5.dp.toPx(), point)
                        drawCircle(
                            color.copy(alpha = alpha),
                            5.dp.toPx(),
                            point,
                            style = Stroke(2.dp.toPx())
                        )
                    }

                    val selectedX =
                        inset + width * selectedIndex / (points.size - 1).coerceAtLeast(1)
                    drawLine(
                        colors.onSurfaceVariant.copy(alpha = 0.3f),
                        Offset(selectedX, inset),
                        Offset(selectedX, inset + height),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
                    )
                    drawSeries(points.map { it.expense }, colors.error, expenseAlpha)
                    drawSeries(points.map { it.income }, colors.tertiary, incomeAlpha)
                }
                Column(
                    Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    (4 downTo 0).forEach { level ->
                        Text(
                            axisAmount(maximum / 4 * level),
                            fontSize = 10.sp,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val indices = (0..4).map { it * (points.size - 1) / 4 }.distinct()
                indices.forEach { index ->
                    Text(
                        points[index].date.format(DateTimeFormatter.ofPattern("M/d")),
                        fontSize = 10.sp,
                        color = colors.onSurfaceVariant
                    )
                }
            }
            Text(
                "点击图例显隐 · 点击或滑动图表查看金额",
                fontSize = 11.sp,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TrendLegend(
    label: String,
    color: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val foreground = if (checked) color else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier.clip(RoundedCornerShape(12.dp))
            .background(if (checked) color.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(Modifier.size(6.dp).background(foreground, CircleShape))
        Text(label, fontSize = 12.sp, color = foreground, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategoryBreakdownCard(
    uiState: AnalyticsUiState.Success,
    onCategoryTypeChange: (CategoryType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expandedId by remember(
        uiState.categoryType,
        uiState.periodStart,
        uiState.timeScope
    ) { mutableStateOf<Long?>(null) }
    var detailId by remember(
        uiState.categoryType,
        uiState.periodStart,
        uiState.timeScope
    ) { mutableStateOf<Long?>(null) }
    val colors = MaterialTheme.colorScheme
    val palette = listOf(
        colors.primary,
        colors.tertiary,
        colors.error,
        colors.secondary,
        colors.tertiaryContainer
    )
    val total =
        if (uiState.categoryType == CategoryType.EXPENSE) uiState.totalExpense else uiState.totalIncome
    val selected = uiState.rankings.find { it.categoryId == expandedId }
    val detail = uiState.rankings.find { it.categoryId == detailId }
    if (detail != null) {
        ModalBottomSheet(onDismissRequest = { detailId = null }) {
            Text(
                detail.categoryName,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${detail.transactions.size} 笔 · ${detail.totalAmount.toFormattedAmount(true)}",
                modifier = Modifier.padding(horizontal = 20.dp),
                color = colors.onSurfaceVariant
            )
            LazyColumn(
                Modifier.fillMaxWidth().heightIn(max = 480.dp).padding(horizontal = 20.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(
                    detail.transactions,
                    key = { it.id }) { transaction -> AnalyticsTransactionRow(transaction) }
            }
        }
    }
    AnalyticsChartCard(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("分类占比", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            AnalyticsSegmentedControl(
                labels = listOf("支出", "收入"),
                selectedIndex = if (uiState.categoryType == CategoryType.EXPENSE) 0 else 1,
                onSelect = { onCategoryTypeChange(if (it == 0) CategoryType.EXPENSE else CategoryType.INCOME) },
                modifier = Modifier.width(112.dp),
                height = 32.dp,
                fontSize = 12.sp,
            )
        }
        if (uiState.rankings.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                Text(
                    "这个周期暂无分类记录",
                    color = colors.onSurfaceVariant
                )
            }
        } else {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                    Canvas(
                        Modifier.fillMaxSize()
                        .semantics {
                            contentDescription = "分类占比环形图，点击扇区展开对应分类明细"
                        }
                        .pointerInput(uiState.rankings) {
                            detectTapGestures { position ->
                                val dx = position.x - size.width / 2f
                                val dy = position.y - size.height / 2f
                                val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                                val radius = size.width / 2f - 12.dp.toPx()
                                if (distance < radius - 16.dp.toPx() || distance > radius + 16.dp.toPx()) return@detectTapGestures
                                val angle =
                                    ((atan2(dy, dx) * 180f / Math.PI.toFloat()) + 450f) % 360f
                                var accumulated = 0f
                                val hit = uiState.rankings.firstOrNull {
                                    accumulated += it.percentage * 360f
                                    angle < accumulated
                                }
                                expandedId = hit?.categoryId
                            }
                        }) {
                        val inset = 12.dp.toPx()
                        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                        var angle = -90f
                        uiState.rankings.forEachIndexed { index, ranking ->
                            val sweep = ranking.percentage * 360f
                            val active = expandedId == null || expandedId == ranking.categoryId
                            drawArc(
                                color = palette[index % palette.size].copy(alpha = if (active) 1f else 0.25f),
                                startAngle = angle + minOf(1f, sweep / 4),
                                sweepAngle = (sweep - minOf(2f, sweep / 2)).coerceAtLeast(0f),
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = arcSize,
                                style = Stroke(
                                    if (expandedId == ranking.categoryId) 23.dp.toPx() else 18.dp.toPx(),
                                    cap = StrokeCap.Butt
                                ),
                            )
                            angle += sweep
                        }
                    }
                    Column(
                        Modifier.width(96.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            selected?.categoryName
                                ?: if (uiState.categoryType == CategoryType.EXPENSE) "总支出" else "总收入",
                            fontSize = 11.sp,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            axisAmount(selected?.totalAmount ?: total),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                        if (selected != null) Text(
                            percent(selected.percentage),
                            fontSize = 11.sp,
                            color = colors.primary
                        )
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    uiState.rankings.take(4).forEachIndexed { index, ranking ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable {
                                expandedId =
                                    if (expandedId == ranking.categoryId) null else ranking.categoryId
                            }.padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                Modifier.size(7.dp)
                                    .background(palette[index % palette.size], CircleShape)
                            )
                            Text(
                                ranking.categoryName,
                                Modifier.weight(1f),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                percent(ranking.percentage),
                                fontSize = 12.sp,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                    if (uiState.rankings.size > 4) Text(
                        "另有 ${uiState.rankings.size - 4} 个分类，见下方",
                        fontSize = 10.sp,
                        color = colors.onSurfaceVariant
                    )
                }
            }
            Text("点击分类或环形扇区展开明细", fontSize = 11.sp, color = colors.onSurfaceVariant)
            uiState.rankings.forEachIndexed { index, ranking ->
                key(ranking.categoryId) {
                    val expanded = expandedId == ranking.categoryId
                    Column(
                        Modifier.fillMaxWidth().animateContentSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable {
                                expandedId = if (expanded) null else ranking.categoryId
                            }.padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    Modifier.size(8.dp)
                                        .background(palette[index % palette.size], CircleShape)
                                )
                                Text(
                                    ranking.categoryName,
                                    Modifier.weight(1f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        ranking.totalAmount.toFormattedAmount(true),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "${percent(ranking.percentage)} · ${ranking.transactions.size}笔",
                                        fontSize = 11.sp,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    if (expanded) "收起分类明细" else "展开分类明细",
                                    Modifier.size(18.dp),
                                    tint = colors.onSurfaceVariant
                                )
                            }
                            LinearProgressIndicator(
                                progress = { ranking.percentage },
                                modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                                color = palette[index % palette.size],
                                trackColor = colors.surfaceVariant,
                                drawStopIndicator = {})
                        }
                        AnimatedVisibility(expanded) {
                            Column(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                    .background(colors.surfaceVariant.copy(alpha = 0.35f))
                                    .padding(horizontal = 12.dp)
                            ) {
                                ranking.transactions.take(5).forEach { AnalyticsTransactionRow(it) }
                                if (ranking.transactions.size > 5) TextButton(
                                    onClick = {
                                        detailId = ranking.categoryId
                                    },
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) { Text("查看全部 ${ranking.transactions.size} 笔") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsTransactionRow(transaction: AnalyticsTransaction) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(transaction.note, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${transaction.date.format(DateTimeFormatter.ofPattern("M月d日"))} · ${transaction.accountName}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            transaction.amount.toFormattedAmount(true),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun percent(ratio: Float): String =
    String.format(Locale.getDefault(), "%.1f%%", ratio * 100)

@Composable
private fun AnalyticsChartCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

private fun axisAmount(amount: Long): String = when {
    amount >= 10_000_000_000L -> BigDecimal.valueOf(amount).movePointLeft(10)
        .setScale(1, RoundingMode.HALF_UP).toPlainString() + "亿"

    amount >= 1_000_000L -> BigDecimal.valueOf(amount).movePointLeft(6)
        .setScale(1, RoundingMode.HALF_UP).toPlainString() + "万"

    else -> amount.toFormattedAmount()
}
