package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.foundation.LocalIndication
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.LedgerSegmentedControl
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.turingzy.ledger.model.DateFilterType
import cn.turingzy.ledger.ui.component.PeriodPickerSheet
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import cn.turingzy.ledger.model.AnalyticsCard
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
        onNavigatePeriod = viewModel::navigatePeriod,
        onSelectPeriod = viewModel::selectPeriod,
        modifier = modifier,
        onCardEnabledChange = viewModel::setCardEnabled,
        onCardOrderChange = viewModel::setCardOrder,
        onDismissCardSettingsError = viewModel::dismissCardSettingsError,
    )
}

@Composable
fun AnalyticsContent(
    uiState: AnalyticsUiState,
    onTimeScopeChange: (AnalyticsTimeScope) -> Unit,
    onCategoryTypeChange: (CategoryType) -> Unit,
    onNavigatePeriod: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onCardEnabledChange: (AnalyticsCard, Boolean) -> Unit = { _, _ -> },
    onCardOrderChange: (List<AnalyticsCard>) -> Unit = {},
    onDismissCardSettingsError: () -> Unit = {},
    onSelectPeriod: (LocalDate) -> Unit = {},
) {
    var showPeriodPicker by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val settingsError = (uiState as? AnalyticsUiState.Success)?.cardSettingsError
    LaunchedEffect(settingsError) {
        if (settingsError != null) {
            snackbarHostState.showSnackbar(settingsError)
            onDismissCardSettingsError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                                AnalyticsCardMenu(
                                    settings = uiState.cardSettings,
                                    onEnabledChange = onCardEnabledChange,
                                    onOrderChange = onCardOrderChange,
                                    modifier = Modifier.align(Alignment.CenterEnd),
                                )
                            }

                            AnalyticsTimeScopeSelector(
                                currentScope = uiState.timeScope,
                                onScopeSelected = onTimeScopeChange,
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (uiState.timeScope != AnalyticsTimeScope.ALL) PeriodNavigationArrow(forward = false, onClick = {
                                    onNavigatePeriod(-1)
                                })
                                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = uiState.periodDisplay,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            .semantics { contentDescription = "选择分析日期" }
                                            .clickable(
                                                enabled = uiState.timeScope != AnalyticsTimeScope.ALL,
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                            ) { showPeriodPicker = true },
                                    )
                                }
                                if (uiState.timeScope != AnalyticsTimeScope.ALL) PeriodNavigationArrow(forward = true, onClick = {
                                    onNavigatePeriod(1)
                                })
                            }
                        }
                    }

                    uiState.cardSettings.enabledCards.forEach { card ->
                        key(card) {
                            when (card) {
                                AnalyticsCard.COMPARISON -> uiState.comparison?.let { comparison ->
                                    AnalyticsComparisonCard(uiState, comparison)
                                }
                                AnalyticsCard.TREND -> TrendLineChartCard(
                                    points = uiState.trendPoints,
                                    periodStart = uiState.periodStart,
                                    periodEnd = uiState.periodEnd,
                                    scope = uiState.timeScope,
                                )
                                AnalyticsCard.CATEGORIES -> CategoryBreakdownCard(
                                    uiState = uiState,
                                    onCategoryTypeChange = onCategoryTypeChange,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    val periodState = uiState as? AnalyticsUiState.Success
    val pickerType = when (periodState?.timeScope) {
        AnalyticsTimeScope.WEEK -> DateFilterType.WEEK
        AnalyticsTimeScope.MONTH -> DateFilterType.MONTH
        AnalyticsTimeScope.YEAR -> DateFilterType.YEAR
        else -> null
    }
    if (showPeriodPicker && periodState != null && pickerType != null) {
        PeriodPickerSheet(
            type = pickerType,
            initialDate = periodState.periodStart,
            onDismiss = { showPeriodPicker = false },
            onSelect = { _, date -> onSelectPeriod(date) },
            showViewSelector = false,
        )
    }
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
        modifier = modifier.fillMaxWidth(),
        emphasizeSelection = false,
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
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (forward) MaterialSymbols.ChevronRight else MaterialSymbols.ChevronLeft,
            contentDescription = if (forward) "下一周期" else "上一周期",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
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
            onNavigatePeriod = {},
        )
    }
}
