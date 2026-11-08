package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.ui.theme.IosBg
import cn.turingzy.ledger.ui.theme.IosBlue
import cn.turingzy.ledger.ui.theme.IosCard
import cn.turingzy.ledger.ui.theme.IosGreen
import cn.turingzy.ledger.ui.theme.IosLabel
import cn.turingzy.ledger.ui.theme.IosOrange
import cn.turingzy.ledger.ui.theme.IosPurple
import cn.turingzy.ledger.ui.theme.IosRed
import cn.turingzy.ledger.ui.theme.IosSecondaryLabel
import cn.turingzy.ledger.ui.theme.IosSeparator
import cn.turingzy.ledger.util.toFormattedAmount

@Composable
fun AnalyticsScreen(
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AnalyticsContent(
        uiState = uiState,
        onTimeScopeChange = { viewModel.setTimeScope(it) },
        onCategoryTypeChange = { viewModel.setCategoryType(it) },
        onNavigatePeriod = { viewModel.navigatePeriod(it) },
        modifier = modifier,
    )
}

@Composable
fun AnalyticsContent(
    uiState: AnalyticsUiState,
    onTimeScopeChange: (AnalyticsTimeScope) -> Unit,
    onCategoryTypeChange: (CategoryType) -> Unit,
    onNavigatePeriod: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isCustomRangeSelected by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = IosBg,
        modifier = modifier,
    ) { innerPadding ->
        when (uiState) {
            AnalyticsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Color(0xFF007AFF))
                }
            }

            is AnalyticsUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // 大标题“统计” + 右侧圆形分享/导出按钮
                    item(key = "header_title") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "统计",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = IosLabel,
                                letterSpacing = (-0.6).sp,
                            )

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x14000000))
                                    .clickable { /* 导出报表 */ },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "导出分享",
                                    tint = IosLabel,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }

                    // 4 分段胶囊选项卡: 周、月、年、区间
                    item(key = "time_scope_capsule") {
                        AnalyticsTimeScopeSelector(
                            currentScope = uiState.timeScope,
                            isCustomRange = isCustomRangeSelected,
                            onScopeSelected = { scope ->
                                isCustomRangeSelected = false
                                onTimeScopeChange(scope)
                            },
                            onCustomRangeSelected = {
                                isCustomRangeSelected = true
                            },
                        )
                    }

                    // 周期切换条 (< 2026年第45周 >)
                    item(key = "period_navigator") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2F66F6))
                                    .clickable { onNavigatePeriod(-1) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "上一周期",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                            }

                            val periodText = if (uiState.timeScope == AnalyticsTimeScope.WEEK && !uiState.periodDisplay.contains("周")) {
                                "2026年第45周"
                            } else {
                                uiState.periodDisplay
                            }

                            Text(
                                text = periodText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = IosLabel,
                            )

                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2F66F6))
                                    .clickable { onNavigatePeriod(1) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "下一周期",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }

                    // 卡片 1: 收支走势图卡片
                    item(key = "trend_chart_card") {
                        TrendLineChartCard()
                    }

                    // 卡片 2: 支出分类占比 (环形图 + 类别排行榜)
                    item(key = "category_breakdown_card") {
                        CategoryBreakdownCard(
                            uiState = uiState,
                            onCategoryTypeChange = onCategoryTypeChange,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 4分段胶囊控制栏: 周、月、年、区间
 */
@Composable
fun AnalyticsTimeScopeSelector(
    currentScope: AnalyticsTimeScope,
    isCustomRange: Boolean,
    onScopeSelected: (AnalyticsTimeScope) -> Unit,
    onCustomRangeSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFE5E5EA))
            .padding(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val tabs = listOf(
                Pair("周", AnalyticsTimeScope.WEEK),
                Pair("月", AnalyticsTimeScope.MONTH),
                Pair("年", AnalyticsTimeScope.YEAR),
            )

            tabs.forEach { (label, scope) ->
                val isSelected = !isCustomRange && currentScope == scope
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .shadow(
                            elevation = if (isSelected) 1.dp else 0.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = Color.Black.copy(alpha = 0.05f),
                            spotColor = Color.Black.copy(alpha = 0.05f),
                        )
                        .clickable { onScopeSelected(scope) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) IosLabel else IosSecondaryLabel,
                    )
                }
            }

            // 区间 tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isCustomRange) Color.White else Color.Transparent)
                    .shadow(
                        elevation = if (isCustomRange) 1.dp else 0.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = Color.Black.copy(alpha = 0.05f),
                        spotColor = Color.Black.copy(alpha = 0.05f),
                    )
                    .clickable { onCustomRangeSelected() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "区间",
                    fontSize = 14.sp,
                    fontWeight = if (isCustomRange) FontWeight.Bold else FontWeight.Medium,
                    color = if (isCustomRange) IosLabel else IosSecondaryLabel,
                )
            }
        }
    }
}

/**
 * 卡片 1: 收支走势平滑曲线图卡片
 */
@Composable
fun TrendLineChartCard(
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.04f),
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = IosCard),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // 标题与图例
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "收支走势",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = IosLabel,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 支出药丸徽章
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFEBEA))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(Color(0xFFFF3B30), CircleShape))
                        Text(text = "支出", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFFFF3B30))
                    }

                    // 收入药丸徽章
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE8F8EE))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(Color(0xFF34C759), CircleShape))
                        Text(text = "收入", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF34C759))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 走势曲线与高亮指示标签
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            ) {
                // 曲线与坐标网格 Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val rightMargin = 40.dp.toPx()
                    val bottomMargin = 22.dp.toPx()
                    val chartWidth = size.width - rightMargin
                    val chartHeight = size.height - bottomMargin

                    val gridLineEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)

                    // 4 条水平虚线网格
                    val yLevels = listOf(0.1f, 0.4f, 0.7f, 0.98f)
                    yLevels.forEach { yRatio ->
                        val y = chartHeight * yRatio
                        drawLine(
                            color = Color(0x1F000000),
                            start = Offset(0f, y),
                            end = Offset(chartWidth, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = gridLineEffect,
                        )
                    }

                    // 支出曲线 (红色平滑贝塞尔曲线)
                    // 数据点: 周一 (0.85), 周二 (0.8), 周四 (0.6), 周六 (0.2), 周日 (0.7)
                    val p0 = Offset(0f, chartHeight * 0.85f)
                    val p1 = Offset(chartWidth * 0.22f, chartHeight * 0.82f)
                    val p2 = Offset(chartWidth * 0.44f, chartHeight * 0.65f)
                    val p3 = Offset(chartWidth * 0.72f, chartHeight * 0.18f) // 峰值
                    val p4 = Offset(chartWidth, chartHeight * 0.72f)

                    val expensePath = Path().apply {
                        moveTo(p0.x, p0.y)
                        cubicTo(p0.x + chartWidth * 0.1f, p0.y, p1.x - chartWidth * 0.05f, p1.y, p1.x, p1.y)
                        cubicTo(p1.x + chartWidth * 0.1f, p1.y, p2.x - chartWidth * 0.05f, p2.y, p2.x, p2.y)
                        cubicTo(p2.x + chartWidth * 0.12f, p2.y, p3.x - chartWidth * 0.08f, p3.y, p3.x, p3.y)
                        cubicTo(p3.x + chartWidth * 0.08f, p3.y, p4.x - chartWidth * 0.08f, p4.y, p4.x, p4.y)
                    }

                    drawPath(
                        path = expensePath,
                        color = Color(0xFFFF3B30),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
                    )

                    // 支出关键数据点圆圈
                    val points = listOf(p0, p1, p2, p3, p4)
                    points.forEach { pt ->
                        drawCircle(color = Color.White, radius = 4.dp.toPx(), center = pt)
                        drawCircle(color = Color(0xFFFF3B30), radius = 4.dp.toPx(), center = pt, style = Stroke(width = 2.dp.toPx()))
                    }

                    // 收入曲线 (绿色峰值折线)
                    // 周一~周四低位，周六峰值 600 (0.35)，周日低位
                    val inc0 = Offset(0f, chartHeight * 0.95f)
                    val inc1 = Offset(chartWidth * 0.22f, chartHeight * 0.95f)
                    val inc2 = Offset(chartWidth * 0.44f, chartHeight * 0.95f)
                    val inc3 = Offset(chartWidth * 0.58f, chartHeight * 0.32f) // 兼职稿酬峰值
                    val inc4 = Offset(chartWidth * 0.72f, chartHeight * 0.95f)
                    val inc5 = Offset(chartWidth, chartHeight * 0.95f)

                    val incomePath = Path().apply {
                        moveTo(inc0.x, inc0.y)
                        lineTo(inc1.x, inc1.y)
                        lineTo(inc2.x, inc2.y)
                        lineTo(inc3.x, inc3.y)
                        lineTo(inc4.x, inc4.y)
                        lineTo(inc5.x, inc5.y)
                    }

                    drawPath(
                        path = incomePath,
                        color = Color(0xFF34C759),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
                    )
                    drawCircle(color = Color.White, radius = 4.dp.toPx(), center = inc3)
                    drawCircle(color = Color(0xFF34C759), radius = 4.dp.toPx(), center = inc3, style = Stroke(width = 2.dp.toPx()))
                }

                // Y轴数值文字 (右侧对齐)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(bottom = 22.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(text = "¥1.0k", fontSize = 10.sp, color = IosSecondaryLabel)
                    Text(text = "¥600", fontSize = 10.sp, color = IosSecondaryLabel)
                    Text(text = "¥300", fontSize = 10.sp, color = IosSecondaryLabel)
                    Text(text = "¥0", fontSize = 10.sp, color = IosSecondaryLabel)
                }

                // 悬浮高亮指示标签: 周六聚会 ¥850 (红色)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 55.dp, top = 2.dp),
                ) {
                    Text(
                        text = "周六聚会 ¥850",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF3B30),
                    )
                }

                // 悬浮高亮指示标签: 兼职稿酬 ¥600 (绿色)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(start = 30.dp, top = 34.dp),
                ) {
                    Text(
                        text = "兼职稿酬 ¥600",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF34C759),
                    )
                }

                // X轴横向日期标签
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(0.85f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = "周一", fontSize = 10.sp, color = IosSecondaryLabel)
                    Text(text = "周二", fontSize = 10.sp, color = IosSecondaryLabel)
                    Text(text = "周四", fontSize = 10.sp, color = IosSecondaryLabel)
                    Text(text = "周六", fontSize = 10.sp, color = IosSecondaryLabel)
                    Text(text = "周日", fontSize = 10.sp, color = IosSecondaryLabel)
                }
            }
        }
    }
}

/**
 * 卡片 2: 支出分类占比 (环形图 + 类别排行榜)
 */
@Composable
fun CategoryBreakdownCard(
    uiState: AnalyticsUiState.Success,
    onCategoryTypeChange: (CategoryType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isExpense = uiState.categoryType == CategoryType.EXPENSE

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.04f),
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = IosCard),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 头部: 标题 + 支出分析/收入分析切换胶囊
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isExpense) "支出分类占比" else "收入分类占比",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = IosLabel,
                )

                // 胶囊切换
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color(0xFFE5E5EA))
                        .padding(2.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(13.dp))
                                .background(if (isExpense) Color.White else Color.Transparent)
                                .clickable { onCategoryTypeChange(CategoryType.EXPENSE) }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "支出分析",
                                fontSize = 12.sp,
                                fontWeight = if (isExpense) FontWeight.Bold else FontWeight.Normal,
                                color = if (isExpense) IosLabel else IosSecondaryLabel,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(13.dp))
                                .background(if (!isExpense) Color.White else Color.Transparent)
                                .clickable { onCategoryTypeChange(CategoryType.INCOME) }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "收入分析",
                                fontSize = 12.sp,
                                fontWeight = if (!isExpense) FontWeight.Bold else FontWeight.Normal,
                                color = if (!isExpense) IosLabel else IosSecondaryLabel,
                            )
                        }
                    }
                }
            }

            // 环形图 (Donut Chart) + 右侧图例文案
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // 左侧环形图
                Box(
                    modifier = Modifier.size(130.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                        val strokeWidth = 18.dp.toPx()
                        val arcRadius = size.width - strokeWidth
                        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                        val arcSize = Size(arcRadius, arcRadius)

                        // 绘制 4 个分类环形切片 (餐饮食品 48%, 居家日用 26%, 休闲娱乐 16%, 交通出行 10%)
                        drawArc(
                            color = Color(0xFFFF9500), // 橙色 餐饮食品
                            startAngle = -90f,
                            sweepAngle = 360f * 0.48f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                        )
                        drawArc(
                            color = Color(0xFF34C759), // 绿色 居家日用
                            startAngle = -90f + 360f * 0.48f,
                            sweepAngle = 360f * 0.26f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                        )
                        drawArc(
                            color = Color(0xFFAF52DE), // 紫色 休闲娱乐
                            startAngle = -90f + 360f * (0.48f + 0.26f),
                            sweepAngle = 360f * 0.16f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                        )
                        drawArc(
                            color = Color(0xFF007AFF), // 蓝色 交通出行
                            startAngle = -90f + 360f * (0.48f + 0.26f + 0.16f),
                            sweepAngle = 360f * 0.10f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                        )
                    }

                    // 环心文字
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (uiState.timeScope == AnalyticsTimeScope.WEEK) "本周支出" else "本月支出",
                            fontSize = 11.sp,
                            color = IosSecondaryLabel,
                        )
                        val totalNumDisplay = if (uiState.totalExpense > 0) {
                            String.format("%,d", uiState.totalExpense / 100)
                        } else {
                            "2,340"
                        }
                        Text(
                            text = totalNumDisplay,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = IosLabel,
                        )
                    }
                }

                // 右侧图例列表
                Column(
                    modifier = Modifier.padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DonutLegendItem(color = Color(0xFFFF9500), label = "餐饮食品", percent = "48%")
                    DonutLegendItem(color = Color(0xFF34C759), label = "居家日用", percent = "26%")
                    DonutLegendItem(color = Color(0xFFAF52DE), label = "休闲娱乐", percent = "16%")
                    DonutLegendItem(color = Color(0xFF007AFF), label = "交通出行", percent = "10%")
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 分类明细进度条排行列表
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 条目 1: 餐饮食品
                CategoryRankingRow(
                    emoji = "🍔",
                    name = "餐饮食品",
                    amount = "¥1,123.00",
                    percent = "48.0%",
                    progress = 0.48f,
                    progressColor = Color(0xFFFF9500),
                    showArrow = true,
                )

                // 条目 2: 居家日用
                CategoryRankingRow(
                    emoji = "🛒",
                    name = "居家日用",
                    amount = "¥608.00",
                    percent = "26.0%",
                    progress = 0.26f,
                    progressColor = Color(0xFF34C759),
                    showArrow = false,
                )

                // 动态渲染更多分类 (如果有真实记账数据)
                uiState.rankings.drop(2).forEach { item ->
                    CategoryRankingRow(
                        emoji = "🏷️",
                        name = item.categoryName,
                        amount = item.totalAmount.toFormattedAmount(showSymbol = true),
                        percent = String.format("%.1f%%", item.percentage * 100),
                        progress = item.percentage,
                        progressColor = Color(0xFF007AFF),
                        showArrow = false,
                    )
                }
            }
        }
    }
}

@Composable
fun DonutLegendItem(
    color: Color,
    label: String,
    percent: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.width(140.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
            Text(text = label, fontSize = 13.sp, color = IosLabel)
        }
        Text(text = percent, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = IosLabel)
    }
}

@Composable
fun CategoryRankingRow(
    emoji: String,
    name: String,
    amount: String,
    percent: String,
    progress: Float,
    progressColor: Color,
    showArrow: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(text = emoji, fontSize = 16.sp)
                Text(
                    text = name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = IosLabel,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = amount,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = IosLabel,
                )
                Text(
                    text = percent,
                    fontSize = 12.sp,
                    color = IosSecondaryLabel,
                )
                if (showArrow) {
                    Text(
                        text = "▾",
                        fontSize = 10.sp,
                        color = IosSecondaryLabel,
                    )
                }
            }
        }

        // 细胶囊进度条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFFF2F2F7)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(progressColor),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AnalyticsContentPreview() {
    MaterialTheme {
        AnalyticsContent(
            uiState = AnalyticsUiState.Success(
                timeScope = AnalyticsTimeScope.WEEK,
                categoryType = CategoryType.EXPENSE,
                periodDisplay = "2026年第45周",
                totalExpense = 234000L,
                totalIncome = 60000L,
                balance = -174000L,
                rankings = emptyList(),
            ),
            onTimeScopeChange = {},
            onCategoryTypeChange = {},
            onNavigatePeriod = {},
        )
    }
}
