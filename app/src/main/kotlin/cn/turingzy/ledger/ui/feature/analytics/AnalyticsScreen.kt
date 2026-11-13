package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import cn.turingzy.ledger.ui.component.LocalBottomBarContentPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.turingzy.ledger.model.DateFilterType
import cn.turingzy.ledger.ui.component.LedgerDatePickerDialog
import cn.turingzy.ledger.ui.component.LedgerSegmentedControl
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.PeriodPickerSheet
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class AnalyticsDateEndpoint { START, END }

@Composable
fun AnalyticsScreen(
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = hiltViewModel(),
    dashboardViewModel: AnalyticsDashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dashboard by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    AnalyticsContent(
        uiState = uiState,
        onTimeScopeChange = viewModel::setTimeScope,
        onNavigatePeriod = viewModel::navigatePeriod,
        onSelectPeriod = viewModel::selectPeriod,
        onSelectRange = viewModel::selectRange,
        onCategoryTypeChange = viewModel::setCategoryType,
        modifier = modifier,
        dashboard = dashboard,
    )
}

@Composable
fun AnalyticsContent(
    uiState: AnalyticsUiState,
    onTimeScopeChange: (AnalyticsTimeScope) -> Unit,
    onNavigatePeriod: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onSelectPeriod: (LocalDate) -> Unit = {},
    onSelectRange: (LocalDate, LocalDate) -> Unit = { _, _ -> },
    dashboard: AnalyticsDashboardState = AnalyticsDashboardState(),
    onCategoryTypeChange: (cn.turingzy.ledger.model.CategoryType) -> Unit = {},
) {
    var showPeriodPicker by rememberSaveable { mutableStateOf(false) }
    var dateEndpoint by rememberSaveable { mutableStateOf<AnalyticsDateEndpoint?>(null) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize().statusBarsPadding(),
    ) { innerPadding ->
        when (uiState) {
            AnalyticsUiState.Loading -> Box(
                Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            is AnalyticsUiState.Success -> Column(
                Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding()).verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp + innerPadding.calculateBottomPadding() + LocalBottomBarContentPadding.current),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.CenterStart) {
                    Text(
                        "分析", fontSize = 28.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface, letterSpacing = (-0.6).sp,
                    )
                    Box(Modifier.align(Alignment.CenterEnd).size(36.dp), contentAlignment = Alignment.Center) {
                        Icon(MaterialSymbols.CalendarMonth, "日历", Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                AnalyticsTimeScopeSelector(uiState.timeScope, onTimeScopeChange)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(
                        if (uiState.timeScope == AnalyticsTimeScope.ALL) 16.dp else 0.dp,
                        Alignment.CenterHorizontally,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (uiState.timeScope == AnalyticsTimeScope.ALL) {
                        AnalyticsRangeDate(
                            uiState.periodStart, "选择分析开始日期", { dateEndpoint = AnalyticsDateEndpoint.START },
                        )
                        Text("～", fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AnalyticsRangeDate(
                            uiState.periodEnd, "选择分析结束日期", { dateEndpoint = AnalyticsDateEndpoint.END },
                        )
                    } else {
                        PeriodNavigationArrow(false, { onNavigatePeriod(-1) })
                        Text(
                            uiState.periodDisplay,
                            Modifier.weight(1f).semantics { contentDescription = "选择分析日期" }
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { showPeriodPicker = true }.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 17.sp, fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                        )
                        PeriodNavigationArrow(true, { onNavigatePeriod(1) })
                    }
                }
                if (dashboard.loading) {
                    androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth())
                } else {
                    AnalyticsDashboardCards(uiState, dashboard, onCategoryTypeChange)
                }
            }
        }
    }
    val periodState = uiState as? AnalyticsUiState.Success ?: return
    val pickerType = when (periodState.timeScope) {
        AnalyticsTimeScope.WEEK -> DateFilterType.WEEK
        AnalyticsTimeScope.MONTH -> DateFilterType.MONTH
        AnalyticsTimeScope.YEAR -> DateFilterType.YEAR
        else -> null
    }
    if (showPeriodPicker && pickerType != null) {
        PeriodPickerSheet(
            type = pickerType, initialDate = periodState.periodStart,
            onDismiss = { showPeriodPicker = false }, onSelect = { _, date -> onSelectPeriod(date) },
            showViewSelector = false,
        )
    }
    val endpoint = dateEndpoint
    if (endpoint != null && periodState.timeScope == AnalyticsTimeScope.ALL) {
        LedgerDatePickerDialog(
            initialDate = if (endpoint == AnalyticsDateEndpoint.START) periodState.periodStart else periodState.periodEnd,
            onDismiss = { dateEndpoint = null },
            onConfirm = { date ->
                if (endpoint == AnalyticsDateEndpoint.START) {
                    onSelectRange(date, maxOf(date, periodState.periodEnd))
                } else {
                    onSelectRange(minOf(periodState.periodStart, date), date)
                }
                dateEndpoint = null
            },
        )
    }
}

@Composable
private fun AnalyticsRangeDate(
    date: LocalDate,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy MM.dd") }
    Text(
        date.format(formatter),
        modifier.semantics { contentDescription = description }.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null, onClick = onClick,
        ).padding(vertical = 4.dp),
        fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
    )
}

@Composable
fun AnalyticsTimeScopeSelector(
    currentScope: AnalyticsTimeScope,
    onScopeSelected: (AnalyticsTimeScope) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scopes = listOf(AnalyticsTimeScope.WEEK, AnalyticsTimeScope.MONTH, AnalyticsTimeScope.YEAR, AnalyticsTimeScope.ALL)
    var selectedScope by remember(currentScope) { mutableStateOf(currentScope) }
    LedgerSegmentedControl(
        labels = listOf("周", "月", "年", "全部"),
        selectedIndex = scopes.indexOf(selectedScope).coerceAtLeast(0),
        onSelect = { selectedScope = scopes[it]; onScopeSelected(scopes[it]) },
        modifier = modifier.fillMaxWidth(), emphasizeSelection = false,
    )
}

@Composable
private fun PeriodNavigationArrow(
    forward: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier.size(28.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current, onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (forward) MaterialSymbols.ChevronRight else MaterialSymbols.ChevronLeft,
            if (forward) "下一周期" else "上一周期",
            Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary,
        )
    }
}
