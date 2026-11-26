package cn.turingzy.ledger.ui.feature.analytics

import cn.turingzy.ledger.ui.feature.analytics.component.AnalyticsChartInteractionHost
import cn.turingzy.ledger.ui.feature.analytics.component.AnalyticsDashboardCards
import cn.turingzy.ledger.ui.feature.analytics.calendar.ConsumptionCalendarSheet
import cn.turingzy.ledger.ui.component.base.ScrollTitleContainerBase
import cn.turingzy.ledger.ui.component.biz.LocalCurrencyBiz
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import cn.turingzy.ledger.ui.component.base.PageFrameBase
import cn.turingzy.ledger.ui.component.base.PrimaryPageHeaderBase
import androidx.compose.material3.Text
import cn.turingzy.ledger.ui.component.card.summary.TopBarIconButton
import cn.turingzy.ledger.ui.component.navigation.LocalBottomBarContentPadding
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
import androidx.hilt.navigation.compose.hiltViewModel
import cn.turingzy.ledger.ui.feature.bill.BillViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.turingzy.ledger.model.transaction.DateFilterType
import cn.turingzy.ledger.ui.component.picker.LedgerDatePickerDialog
import cn.turingzy.ledger.ui.component.input.SegmentedControl
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.picker.PeriodPickerSheet
import cn.turingzy.ledger.ui.interaction.UiInteractionKind
import cn.turingzy.ledger.ui.interaction.rememberUiAction
import cn.turingzy.ledger.ui.interaction.uiClickable
import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.R
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * 自定义日期区间选择时的端点类型。
 */
private enum class AnalyticsDateEndpoint { START, END }

/**
 * 统计分析屏幕。
 *
 * @param modifier 修饰符。
 * @param viewModel 统计分析主 ViewModel。
 * @param dashboardViewModel 统计看板 ViewModel。
 */
@Composable
fun AnalyticsScreen(
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = hiltViewModel(),
    dashboardViewModel: AnalyticsDashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dashboard by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val billViewModel: BillViewModel = hiltViewModel()
    var calendarMonth by rememberSaveable { mutableStateOf<String?>(null) }
    androidx.compose.runtime.CompositionLocalProvider(LocalAnalyticsBillViewModel provides billViewModel) {
        AnalyticsContent(
            uiState = uiState,
            onTimeScopeChange = viewModel::setTimeScope,
            onNavigatePeriod = viewModel::navigatePeriod,
            onSelectPeriod = viewModel::selectPeriod,
            onSelectRange = viewModel::selectRange,
            onCategoryTypeChange = viewModel::setCategoryType,
            modifier = modifier,
            dashboard = dashboard,
            onOpenCalendar = { calendarMonth = YearMonth.from(it).toString() },
        )
    }
    calendarMonth?.let { month ->
        ConsumptionCalendarSheet(
            dashboard = dashboard,
            initialMonth = YearMonth.parse(month),
            onDismiss = { calendarMonth = null },
            billViewModel = billViewModel,
        )
    }
}

/**
 * 统计分析界面的内容布局。
 *
 * @param uiState 当前统计分析状态。
 * @param onTimeScopeChange 时间范围粒度改变回调。
 * @param onNavigatePeriod 前后周期导航步进回调（如 -1 表示上一期，1 表示下一期）。
 * @param modifier 修饰符。
 * @param onSelectPeriod 选中特定周期起始日期的回调。
 * @param onSelectRange 自定义区间选中的回调（起始日期与结束日期）。
 * @param dashboard 看板数据状态。
 * @param onCategoryTypeChange 收支类别类型切换回调（支出/收入）。
 * @param onOpenCalendar 打开消费日历回调。
 */
@Composable
fun AnalyticsContent(
    uiState: AnalyticsUiState,
    onTimeScopeChange: (AnalyticsTimeScope) -> Unit,
    onNavigatePeriod: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onSelectPeriod: (LocalDate) -> Unit = {},
    onSelectRange: (LocalDate, LocalDate) -> Unit = { _, _ -> },
    dashboard: AnalyticsDashboardState = AnalyticsDashboardState(),
    onCategoryTypeChange: (cn.turingzy.ledger.model.category.CategoryType) -> Unit = {},
    onOpenCalendar: (LocalDate) -> Unit = {},
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    var showPeriodPicker by rememberSaveable { mutableStateOf(false) }
    var dateEndpoint by rememberSaveable { mutableStateOf<AnalyticsDateEndpoint?>(null) }
    val bottomBarPadding = LocalBottomBarContentPadding.current
    androidx.compose.runtime.CompositionLocalProvider(LocalCurrencyBiz provides ((uiState as? AnalyticsUiState.Success)?.currency ?: "CNY")) {
        AnalyticsChartInteractionHost {
            PageFrameBase(modifier) {
                when (uiState) {
                    AnalyticsUiState.Loading -> Unit
                    is AnalyticsUiState.Success -> {
                        val scrollState = rememberScrollState()
                        ScrollTitleContainerBase(
                            title = textResources.getString(cn.turingzy.ledger.R.string.ui_analytics),
                            scrollOffset = { scrollState.value },
                            onScrollToTop = { scrollState.animateScrollTo(0) },
                            actions = {
                                TopBarIconButton(
                                    imageVector = MaterialSymbols.CalendarMonth,
                                    contentDescription = textResources.getString(R.string.ui_spending_calendar),
                                    onClick = rememberUiAction(
                                        { onOpenCalendar(minOf(uiState.periodEnd, LocalDate.now())) },
                                        UiInteractionKind.Navigation,
                                    ),
                                )
                            },
                        ) { scrollModifier ->
                            Column(
                                scrollModifier.fillMaxSize().verticalScroll(scrollState)
                                    .padding(
                                        start = LedgerStyle.spacing.pageHorizontal,
                                        end = LedgerStyle.spacing.pageHorizontal,
                                        top = LedgerStyle.spacing.extraSmall,
                                        bottom = bottomBarPadding + LedgerStyle.spacing.block,
                                    ),
                                verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.labelGap),
                            ) {
                                PrimaryPageHeaderBase(
                                    title = textResources.getString(cn.turingzy.ledger.R.string.ui_analytics),
                                )
                                AnalyticsTimeScopeSelector(uiState.timeScope, onTimeScopeChange)
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = LedgerStyle.spacing.extraSmall),
                                    horizontalArrangement = Arrangement.spacedBy(
                                        if (uiState.timeScope == AnalyticsTimeScope.ALL) 16.dp else 0.dp,
                                        Alignment.CenterHorizontally,
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (uiState.timeScope == AnalyticsTimeScope.ALL) {
                                        AnalyticsRangeDate(
                                            uiState.periodStart, textResources.getString(cn.turingzy.ledger.R.string.ui_select_analytics_start_date), { dateEndpoint = AnalyticsDateEndpoint.START },
                                        )
                                        Text("～", fontSize = LedgerStyle.typography.sectionTitle.fontSize, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        AnalyticsRangeDate(
                                            uiState.periodEnd, textResources.getString(cn.turingzy.ledger.R.string.ui_select_analytics_end_date), { dateEndpoint = AnalyticsDateEndpoint.END },
                                        )
                                    } else {
                                        PeriodNavigationArrow(false, { onNavigatePeriod(-1) })
                                        Text(
                                            uiState.periodDisplay,
                                            Modifier.weight(1f).semantics { contentDescription = textResources.getString(cn.turingzy.ledger.R.string.ui_select_analytics_period) }
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null,
                                                    onClick = rememberUiAction({ showPeriodPicker = true }, UiInteractionKind.Navigation)
                                                ).padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = LedgerStyle.typography.sectionTitle.fontSize, fontWeight = FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onSurface, maxLines = 1,
                                            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                                        )
                                        PeriodNavigationArrow(true, { onNavigatePeriod(1) })
                                    }
                                }
                                if (!dashboard.loading) {
                                    AnalyticsDashboardCards(uiState, dashboard, onCategoryTypeChange)
                                }
                            }
                        }
                    }
                }
            }
        }
        val periodState = uiState as? AnalyticsUiState.Success ?: return@CompositionLocalProvider
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
}

/**
 * 自定义区间模式下的日期点击文本。
 *
 * @param date 展示的日期。
 * @param description 无障碍语义描述。
 * @param onClick 点击事件回调。
 * @param modifier 修饰符。
 */
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
            indication = null, onClick = rememberUiAction(onClick, UiInteractionKind.Navigation),
        ).padding(vertical = LedgerStyle.spacing.extraSmall),
        fontSize = LedgerStyle.typography.sectionTitle.fontSize, color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
    )
}

/**
 * 统计时间粒度分段选择器（周、月、年、全部）。
 *
 * @param currentScope 当前选中的时间粒度。
 * @param onScopeSelected 选择时间粒度时的回调。
 * @param modifier 修饰符。
 */
@Composable
fun AnalyticsTimeScopeSelector(
    currentScope: AnalyticsTimeScope,
    onScopeSelected: (AnalyticsTimeScope) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val scopes = listOf(AnalyticsTimeScope.WEEK, AnalyticsTimeScope.MONTH, AnalyticsTimeScope.YEAR, AnalyticsTimeScope.ALL)
    var selectedScope by remember(currentScope) { mutableStateOf(currentScope) }
    SegmentedControl(
        labels = listOf(textResources.getString(cn.turingzy.ledger.R.string.ui_week), textResources.getString(cn.turingzy.ledger.R.string.ui_month), textResources.getString(cn.turingzy.ledger.R.string.ui_year), textResources.getString(cn.turingzy.ledger.R.string.ui_all)),
        selectedIndex = scopes.indexOf(selectedScope).coerceAtLeast(0),
        onSelect = { selectedScope = scopes[it]; onScopeSelected(scopes[it]) },
        modifier = modifier.fillMaxWidth(), emphasizeSelection = false,
    )
}

/**
 * 周期前后切换的圆形导航箭头按钮。
 *
 * @param forward 是否为前进（下一周期），true 表示下一周期，false 表示上一周期。
 * @param onClick 点击事件回调。
 * @param modifier 修饰符。
 */
@Composable
private fun PeriodNavigationArrow(
    forward: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    Box(
        modifier.size(28.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .uiClickable(kind = UiInteractionKind.Navigation, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (forward) MaterialSymbols.ChevronRight else MaterialSymbols.ChevronLeft,
            if (forward) textResources.getString(cn.turingzy.ledger.R.string.ui_next_period) else textResources.getString(cn.turingzy.ledger.R.string.ui_previous_period),
            Modifier.size(LedgerStyle.icons.default), tint = MaterialTheme.colorScheme.primary,
        )
    }
}
