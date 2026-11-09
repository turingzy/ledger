package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.turingzy.ledger.model.CategoryType
import java.time.LocalDate

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
        onPeriodSelected = viewModel::selectPeriod,
        modifier = modifier,
    )
}

@Composable
fun AnalyticsContent(
    uiState: AnalyticsUiState,
    onTimeScopeChange: (AnalyticsTimeScope) -> Unit,
    onCategoryTypeChange: (CategoryType) -> Unit,
    onNavigatePeriod: (Int) -> Unit,
    onPeriodSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPeriodPicker by rememberSaveable { mutableStateOf(false) }

    if (showPeriodPicker && uiState is AnalyticsUiState.Success) {
        AnalyticsPeriodPicker(
            scope = uiState.timeScope,
            selectedDate = uiState.periodStart,
            onDismissRequest = { showPeriodPicker = false },
            onConfirm = { date ->
                onPeriodSelected(date)
                showPeriodPicker = false
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) { innerPadding ->
        when (uiState) {
            AnalyticsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            is AnalyticsUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item(key = "header_and_time_scope") {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
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
                                    color = MaterialTheme.colorScheme.onSurface,
                                    letterSpacing = (-0.6).sp,
                                    modifier = Modifier.padding(bottom = 2.dp),
                                )

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(bounded = true, radius = 18.dp),
                                        ) { /* 导出报表 */ },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "导出分享",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }

                            AnalyticsTimeScopeSelector(
                                currentScope = uiState.timeScope,
                                onScopeSelected = onTimeScopeChange,
                            )
                        }
                    }

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
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                    .clickable { onNavigatePeriod(-1) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "上一周期",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }

                            Text(
                                text = uiState.periodDisplay,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showPeriodPicker = true }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            )

                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                    .clickable { onNavigatePeriod(1) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "下一周期",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }

                    // 卡片 1: 收支走势图卡片
                    item(key = "trend_chart_card") {
                        TrendLineChartCard(points = uiState.trendPoints, scope = uiState.timeScope)
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

@Composable
fun AnalyticsTimeScopeSelector(
    currentScope: AnalyticsTimeScope,
    onScopeSelected: (AnalyticsTimeScope) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnalyticsSegmentedControl(
        labels = listOf("周", "月", "年"),
        selectedIndex = AnalyticsTimeScope.entries.indexOf(currentScope),
        onSelect = { onScopeSelected(AnalyticsTimeScope.entries[it]) },
        modifier = modifier.fillMaxWidth(),
    )
}

@Preview(showBackground = true)
@Composable
fun AnalyticsContentPreview() {
    MaterialTheme {
        AnalyticsContent(
            uiState = AnalyticsUiState.Success(
                timeScope = AnalyticsTimeScope.WEEK,
                categoryType = CategoryType.EXPENSE,
                periodDisplay = "2026.11.2 - 11.8",
                totalExpense = 234000L,
                totalIncome = 60000L,
                balance = -174000L,
                rankings = emptyList(),
                periodStart = LocalDate.of(2026, 11, 2),
            ),
            onTimeScopeChange = {},
            onCategoryTypeChange = {},
            onNavigatePeriod = {},
            onPeriodSelected = {},
        )
    }
}
