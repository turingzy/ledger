package cn.turingzy.ledger.ui.feature.analytics.calendar

import cn.turingzy.ledger.ui.feature.analytics.AnalyticsDashboardState
import cn.turingzy.ledger.ui.feature.analytics.AnalyticsDashboardViewModel
import cn.turingzy.ledger.ui.feature.analytics.component.analyticsChartInfoColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.turingzy.ledger.R
import cn.turingzy.ledger.model.transaction.DateFilterType
import cn.turingzy.ledger.model.transaction.TransactionType
import cn.turingzy.ledger.ui.component.biz.LocalLedgerCurrency
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.picker.PeriodPickerActions
import cn.turingzy.ledger.ui.component.picker.PeriodPickerSheet
import cn.turingzy.ledger.ui.component.biz.currencySymbol
import cn.turingzy.ledger.ui.component.picker.returnLabel
import cn.turingzy.ledger.ui.component.base.*
import cn.turingzy.ledger.ui.component.biz.BusinessAmountSize
import cn.turingzy.ledger.ui.component.biz.BusinessAmountText
import cn.turingzy.ledger.ui.component.biz.BusinessTransactionItem
import cn.turingzy.ledger.ui.interaction.UiInteractionKind
import cn.turingzy.ledger.ui.interaction.rememberUiEvent
import cn.turingzy.ledger.ui.interaction.uiSelectable
import cn.turingzy.ledger.ui.interaction.uiClickable
import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.util.toFormattedAmount
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
internal fun ConsumptionCalendarScreen(
    initialMonth: YearMonth,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onRecordTransaction: (LocalDate) -> Unit = {},
    viewModel: AnalyticsDashboardViewModel = hiltViewModel(),
    onTransactionClick: (Long) -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
) {
    val dashboard by viewModel.uiState.collectAsStateWithLifecycle()
    CompositionLocalProvider(LocalLedgerCurrency provides dashboard.currency) {
        Box(modifier.fillMaxSize()) {
            ConsumptionCalendarContent(
                dashboard, initialMonth, onBack,
                onRecordTransaction = onRecordTransaction, onTransactionClick = onTransactionClick,
            )
            snackbarHostState?.let { host ->
                SnackbarHost(host, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConsumptionCalendarContent(
    dashboard: AnalyticsDashboardState,
    initialMonth: YearMonth,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
    onRecordTransaction: (LocalDate) -> Unit = {},
    onTransactionClick: (Long) -> Unit = {},
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val colors = LedgerStyle.colors
    var monthKey by rememberSaveable(initialMonth.toString()) { mutableStateOf(initialMonth.toString()) }
    val month = remember(monthKey) { YearMonth.parse(monthKey) }
    val initialDate = if (month == YearMonth.from(today)) today else month.atDay(1)
    var selectedEpoch by rememberSaveable(monthKey) { mutableLongStateOf(initialDate.toEpochDay()) }
    val selectedDate = LocalDate.ofEpochDay(selectedEpoch)
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val data = remember(dashboard.days, month, today) { consumptionCalendarData(dashboard.days, month, today) }
    val entries = remember(dashboard.transactions, selectedDate) {
        dashboard.transactions.filter { it.date == selectedDate && it.type == TransactionType.EXPENSE }
    }
    val dayFormatter = remember(configuration) { DateTimeFormatter.ofPattern(if (configuration.locales[0].language == "en") "MMM d" else "M月d日", configuration.locales[0]) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val listState = rememberLazyListState()
    BasePageScaffold(
        title = textResources.getString(R.string.ui_spending_calendar),
        modifier = modifier,
        onBack = onBack,
        titleStyle = LedgerStyle.typography.detailPageTitle,
        backDescription = textResources.getString(R.string.ui_back_to_analytics),
        scrolled = listState.canScrollBackward,
    ) { headerPadding ->
        LazyColumn(
            Modifier.fillMaxSize().testTag("consumptionCalendarList"),
            state = listState,
            contentPadding = PaddingValues(
                start = LedgerStyle.spacing.pageHorizontal,
                end = LedgerStyle.spacing.pageHorizontal,
                top = headerPadding.calculateTopPadding() + LedgerStyle.spacing.sectionGap,
                bottom = LedgerStyle.spacing.block,
            ),
        ) {
            if (!dashboard.amountsAvailable) {
                item(key = "missing_rates") {
                    BaseEmptyState(title = textResources.getString(R.string.ui_exchange_rate_needed_refresh_or_enter_a_rate))
                }
            } else {
                item(key = "calendar") {
                    BaseCard(
                        shape = LedgerStyle.shapes.summaryCard,
                        outlined = true,
                        contentPadding = PaddingValues(horizontal = LedgerStyle.spacing.calendarCardHorizontal, vertical = LedgerStyle.spacing.calendarCardVertical),
                        contentSpacing = LedgerStyle.spacing.inlineGap,
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                month.format(DateTimeFormatter.ofPattern(if (configuration.locales[0].language == "en") "MMM yyyy" else "yyyy年M月", configuration.locales[0])),
                                Modifier.clip(LedgerStyle.shapes.control)
                                    .uiClickable(kind = UiInteractionKind.Navigation) { showPicker = true }
                                    .padding(vertical = LedgerStyle.spacing.small),
                                style = LedgerStyle.typography.calendarMonthTitle,
                                color = colors.textPrimary, maxLines = 1,
                            )
                            Spacer(Modifier.weight(1f))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    textResources.getString(R.string.calendar_month_expense, month.monthValue).trim(),
                                    style = LedgerStyle.typography.calendarMonthLabel, color = colors.textSecondary,
                                )
                                BusinessAmountText(data.totalExpense, dashboard.currency, textStyle = LedgerStyle.typography.calendarMonthAmount)
                            }
                        }
                        ConsumptionMonthGrid(data, selectedDate, today, { selectedEpoch = it.toEpochDay() })
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.extraSmall, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(textResources.getString(R.string.ui_less), style = LedgerStyle.typography.chartLabel, color = colors.textSecondary)
                            (0..4).forEach { level ->
                                Box(Modifier.size(LedgerStyle.spacing.medium).background(calendarHeatColor(level), LedgerStyle.shapes.indicator))
                            }
                            Text(textResources.getString(R.string.ui_more), style = LedgerStyle.typography.chartLabel, color = colors.textSecondary)
                        }
                    }
                }
                item(key = "day") {
                    BaseSectionHeader(
                        title = textResources.getString(R.string.calendar_day_count, selectedDate.format(dayFormatter), entries.size),
                        variant = BaseSectionHeaderVariant.Supporting,
                        modifier = Modifier.padding(top = LedgerStyle.spacing.section, bottom = LedgerStyle.spacing.medium),
                        actions = { BusinessAmountText(data.expenses[selectedDate] ?: 0L, dashboard.currency, size = BusinessAmountSize.Small) },
                    )
                }
                if (entries.isEmpty()) item(key = "empty") {
                    when {
                        dashboard.loading -> BaseEmptyState(title = textResources.getString(R.string.ui_loading_transactions))
                        else -> ConsumptionCalendarEmptyState(onRecord = { onRecordTransaction(selectedDate) })
                    }
                }
                itemsIndexed(entries, key = { _, entry -> entry.id }) { index, entry ->
                    val corners = LedgerStyle.shapes.summaryCard
                    val square = CornerSize(LedgerStyle.spacing.none)
                    BaseCard(
                        shape = corners.copy(
                            topStart = if (index == 0) corners.topStart else square,
                            topEnd = if (index == 0) corners.topEnd else square,
                            bottomStart = if (index == entries.lastIndex) corners.bottomStart else square,
                            bottomEnd = if (index == entries.lastIndex) corners.bottomEnd else square,
                        ),
                        contentPadding = PaddingValues(LedgerStyle.spacing.none),
                        contentSpacing = LedgerStyle.spacing.none,
                    ) {
                        if (index > 0) BaseDivider(Modifier.padding(
                            start = LedgerStyle.dimensions.compactIconButton + LedgerStyle.spacing.rowHorizontal + LedgerStyle.spacing.sectionGap,
                            end = LedgerStyle.spacing.rowHorizontal,
                        ))
                        BusinessTransactionItem(
                            amount = entry.amount, currency = entry.currency, type = entry.type,
                            category = entry.category, accountName = entry.accountName,
                            timeText = entry.time?.format(timeFormatter).orEmpty(), note = entry.note,
                            onClick = { onTransactionClick(entry.id) },
                        )
                    }
                }
            }
        }
    }
    if (showPicker) {
        val selectMonth = rememberUiEvent<Pair<DateFilterType, LocalDate>>(
            { (_, date) -> monthKey = YearMonth.from(date).toString(); showPicker = false },
            UiInteractionKind.Selection,
        )
        PeriodPickerSheet(
            DateFilterType.MONTH, month.atDay(1), { showPicker = false },
            { type, date -> selectMonth(type to date) }, showViewSelector = false,
            header = { returnToCurrent ->
                PeriodPickerActions(DateFilterType.MONTH, returnToCurrent)
            },
        )
    }
}

@Composable
private fun ConsumptionMonthGrid(
    data: ConsumptionCalendarData,
    selectedDate: LocalDate,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit,
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val currencySymbol = currencySymbol(LocalLedgerCurrency.current)
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf(textResources.getString(cn.turingzy.ledger.R.string.ui_mon), textResources.getString(cn.turingzy.ledger.R.string.ui_tue), textResources.getString(cn.turingzy.ledger.R.string.ui_wed), textResources.getString(cn.turingzy.ledger.R.string.ui_thu_2), textResources.getString(cn.turingzy.ledger.R.string.ui_fri), textResources.getString(cn.turingzy.ledger.R.string.ui_sat), textResources.getString(cn.turingzy.ledger.R.string.ui_sun_2)).forEachIndexed { index, label ->
                val highlighted = today.dayOfWeek.value == index + 1
                Text(label, Modifier.weight(1f).padding(vertical = 5.dp).semantics { selected = highlighted }, fontSize = LedgerStyle.typography.label.fontSize, lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    color = if (highlighted) colors.tertiary else colors.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
        data.cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                week.forEach { date ->
                    if (date == null) Spacer(Modifier.weight(1f).aspectRatio(1.25f))
                    else {
                        val level = data.levels[date] ?: 0
                        val selected = date == selectedDate
                        val future = date > today
                        val color = if (level >= 3) colors.onPrimary else colors.onSurface
                        Box(Modifier.weight(1f).aspectRatio(1.25f).clip(RoundedCornerShape(9.dp))
                            .background(calendarHeatColor(level).copy(alpha = if (future) 0.35f else 1f))
                            .then(if (selected) Modifier.border(1.5.dp, colors.tertiary, RoundedCornerShape(9.dp)) else Modifier)
                            .uiSelectable(selected = selected, enabled = !future, onClick = { onSelect(date) })
                            .semantics { contentDescription = "$date，支出${(data.expenses[date] ?: 0L).toFormattedAmount(true, symbol = currencySymbol)}" },
                            contentAlignment = Alignment.Center) {
                            Text(date.dayOfMonth.toString(), fontSize = LedgerStyle.typography.bodySmall.fontSize, lineHeight = 16.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = color.copy(alpha = if (future) 0.35f else 1f))
                            if (date == today) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp)
                                .size(3.dp).background(colors.error, CircleShape))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun calendarHeatColor(level: Int): Color {
    val colors = MaterialTheme.colorScheme
    val base = analyticsChartInfoColor()
    if (colors.surface.luminance() < 0.5f) return when (level) {
        0 -> base
        1 -> lerp(base, colors.tertiary, 0.18f)
        2 -> lerp(base, colors.tertiary, 0.40f)
        3 -> lerp(base, colors.tertiary, 0.70f)
        else -> colors.tertiary
    }
    return when (level) {
        0 -> base
        1 -> lerp(colors.surface, colors.tertiary, 0.35f)
        2 -> lerp(colors.surface, colors.tertiary, 0.85f)
        3 -> lerp(colors.tertiary, colors.onSurface, 0.32f)
        else -> lerp(colors.tertiary, colors.onSurface, 0.55f)
    }
}
