package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.turingzy.ledger.model.CategoryType
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsContent(
    uiState: AnalyticsUiState,
    onTimeScopeChange: (AnalyticsTimeScope) -> Unit,
    onCategoryTypeChange: (CategoryType) -> Unit,
    onNavigatePeriod: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("统计与报表") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
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
                    CircularProgressIndicator()
                }
            }

            is AnalyticsUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // 时间维度切换 (周 / 月 / 年)
                    item(key = "time_scope_segmented") {
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            AnalyticsTimeScope.entries.forEachIndexed { index, scope ->
                                val label = when (scope) {
                                    AnalyticsTimeScope.WEEK -> "周"
                                    AnalyticsTimeScope.MONTH -> "月"
                                    AnalyticsTimeScope.YEAR -> "年"
                                }
                                SegmentedButton(
                                    selected = uiState.timeScope == scope,
                                    onClick = { onTimeScopeChange(scope) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = AnalyticsTimeScope.entries.size,
                                    ),
                                ) {
                                    Text(label)
                                }
                            }
                        }
                    }

                    // 周期切换条 (< 2026年11月 >)
                    item(key = "period_navigator") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = { onNavigatePeriod(-1) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "上一周期",
                                )
                            }

                            Text(
                                text = uiState.periodDisplay,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )

                            IconButton(onClick = { onNavigatePeriod(1) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "下一周期",
                                )
                            }
                        }
                    }

                    // 周期收支概览卡片
                    item(key = "period_summary_card") {
                        PeriodSummaryCard(
                            totalExpense = uiState.totalExpense,
                            totalIncome = uiState.totalIncome,
                            balance = uiState.balance,
                        )
                    }

                    // 分类类型切换 (支出分析 vs 收入分析)
                    item(key = "category_type_switch") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = uiState.categoryType == CategoryType.EXPENSE,
                                onClick = { onCategoryTypeChange(CategoryType.EXPENSE) },
                                label = { Text("支出分析") },
                            )
                            FilterChip(
                                selected = uiState.categoryType == CategoryType.INCOME,
                                onClick = { onCategoryTypeChange(CategoryType.INCOME) },
                                label = { Text("收入分析") },
                            )
                        }
                    }

                    // 分类排行榜标题
                    item(key = "ranking_header") {
                        val headerLabel = if (uiState.categoryType == CategoryType.EXPENSE) "支出分类排行" else "收入分类排行"
                        Text(
                            text = headerLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }

                    if (uiState.rankings.isEmpty()) {
                        item(key = "empty_rankings") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "当前周期暂无相关交易记录",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else {
                        items(
                            items = uiState.rankings,
                            key = { it.categoryId },
                        ) { ranking ->
                            CategoryRankingCard(
                                ranking = ranking,
                                isExpense = uiState.categoryType == CategoryType.EXPENSE,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PeriodSummaryCard(
    totalExpense: Long,
    totalIncome: Long,
    balance: Long,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "总支出",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = totalExpense.toFormattedAmount(showSymbol = true),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "总收入",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = totalIncome.toFormattedAmount(showSymbol = true),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "净结余",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = balance.toFormattedAmount(showSymbol = true),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
fun CategoryRankingCard(
    ranking: CategoryExpenseRanking,
    isExpense: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = ranking.categoryName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = ranking.totalAmount.toFormattedAmount(showSymbol = true),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "(${(ranking.percentage * 100).toInt()}%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { ranking.percentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
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
                timeScope = AnalyticsTimeScope.MONTH,
                categoryType = CategoryType.EXPENSE,
                periodDisplay = "2026年11月",
                totalExpense = 450000L,
                totalIncome = 2000000L,
                balance = 1550000L,
                rankings = listOf(
                    CategoryExpenseRanking(1L, "餐饮食品", 300000L, 0.67f),
                    CategoryExpenseRanking(2L, "交通出行", 150000L, 0.33f),
                ),
            ),
            onTimeScopeChange = {},
            onCategoryTypeChange = {},
            onNavigatePeriod = {},
        )
    }
}
