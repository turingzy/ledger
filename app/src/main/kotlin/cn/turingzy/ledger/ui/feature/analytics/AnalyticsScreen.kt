package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.ui.component.calendar.CalendarPickerState
import cn.turingzy.ledger.ui.component.calendar.CalendarRangeSelector
import cn.turingzy.ledger.ui.component.calendar.CalendarSelection
import cn.turingzy.ledger.ui.component.calendar.CalendarView
import cn.turingzy.ledger.ui.component.calendar.MonthCalendarDatePickerCard
import java.time.YearMonth
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
        onPeriodSelected = viewModel::selectRange,
        onNavigatePeriod = viewModel::navigatePeriod,
        modifier = modifier,
    )
}

@Composable
fun AnalyticsContent(
    uiState: AnalyticsUiState,
    onTimeScopeChange: (AnalyticsTimeScope) -> Unit,
    onCategoryTypeChange: (CategoryType) -> Unit,
    onPeriodSelected: (LocalDate, LocalDate) -> Unit,
    onNavigatePeriod: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPeriodPicker by rememberSaveable { mutableStateOf(false) }
    var calendarSession by remember { mutableIntStateOf(0) }

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
                val confirmedSelection = remember(uiState.periodStart, uiState.periodEnd) {
                    CalendarSelection(uiState.periodStart, uiState.periodEnd)
                }
                val calendarState = remember(confirmedSelection, uiState.timeScope, calendarSession) {
                    CalendarPickerState(
                        initialMonth = YearMonth.from(confirmedSelection.start),
                        initialSelection = confirmedSelection,
                        resetSelection = if (uiState.timeScope == AnalyticsTimeScope.THREE_MONTHS) {
                            { today ->
                                val month = YearMonth.from(today)
                                CalendarSelection(month.minusMonths(2).atDay(1), month.atEndOfMonth())
                            }
                        } else null,
                    ).apply {
                        view = when (uiState.timeScope) {
                            AnalyticsTimeScope.WEEK -> CalendarView.DAY
                            AnalyticsTimeScope.MONTH, AnalyticsTimeScope.THREE_MONTHS -> CalendarView.MONTH
                            AnalyticsTimeScope.YEAR -> CalendarView.YEAR
                        }
                    }
                }
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    key("header_and_time_scope") {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "分析",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    letterSpacing = (-0.6).sp,
                                    modifier = Modifier.align(Alignment.CenterStart),
                                )
                            }

                            AnalyticsTimeScopeSelector(
                                currentScope = uiState.timeScope,
                                onScopeSelected = {
                                    showPeriodPicker = false
                                    onTimeScopeChange(it)
                                },
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                PeriodNavigationArrow(forward = false, onClick = {
                                    showPeriodPicker = false
                                    onNavigatePeriod(-1)
                                })
                                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    CalendarRangeSelector(
                                        selection = if (showPeriodPicker) calendarState.selection else confirmedSelection,
                                        expanded = showPeriodPicker,
                                        onClick = {
                                            if (!showPeriodPicker) calendarSession += 1
                                            showPeriodPicker = !showPeriodPicker
                                        },
                                    )
                                }
                                PeriodNavigationArrow(forward = true, onClick = {
                                    showPeriodPicker = false
                                    onNavigatePeriod(1)
                                })
                            }

                            AnimatedVisibility(
                                visible = showPeriodPicker,
                                enter = fadeIn(tween(250)) + expandVertically(tween(300)),
                                exit = fadeOut(tween(200)) + shrinkVertically(tween(250)),
                            ) {
                                key(calendarSession) {
                                    MonthCalendarDatePickerCard(
                                        state = calendarState,
                                        onDismiss = { showPeriodPicker = false },
                                        onConfirm = { start, end ->
                                            onPeriodSelected(start, end)
                                            showPeriodPicker = false
                                        },
                                    )
                                }
                            }
                        }
                    }

                    key("trend_chart_card") {
                        TrendLineChartCard(
                            points = uiState.trendPoints,
                            periodStart = uiState.periodStart,
                            periodEnd = uiState.periodEnd,
                            scope = uiState.timeScope,
                        )
                    }

                    key("category_breakdown_card") {
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
        labels = listOf("周", "月", "3个月", "年"),
        selectedIndex = AnalyticsTimeScope.entries.indexOf(currentScope),
        onSelect = { onScopeSelected(AnalyticsTimeScope.entries[it]) },
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun PeriodNavigationArrow(
    forward: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (forward) Icons.AutoMirrored.Filled.ArrowForward else Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = if (forward) "下一周期" else "上一周期",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
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
                periodDisplay = "2026 11.02-11.08",
                totalExpense = 234000L,
                totalIncome = 60000L,
                balance = -174000L,
                rankings = emptyList(),
                periodStart = LocalDate.of(2026, 11, 2),
            ),
            onTimeScopeChange = {},
            onCategoryTypeChange = {},
            onPeriodSelected = { _, _ -> },
            onNavigatePeriod = {},
        )
    }
}
