package cn.turingzy.ledger.ui.feature.analytics

import cn.turingzy.ledger.ui.component.LocalLedgerCurrency
import cn.turingzy.ledger.ui.component.currencySymbol
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.turingzy.ledger.model.DateFilterType
import cn.turingzy.ledger.model.TransactionType
import cn.turingzy.ledger.ui.component.LedgerIconButton
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.PeriodPickerSheet
import cn.turingzy.ledger.ui.component.TransactionRow
import cn.turingzy.ledger.ui.component.categoryGlyph
import cn.turingzy.ledger.util.toFormattedAmount
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
internal fun ConsumptionCalendarScreen(
    initialMonth: YearMonth,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnalyticsDashboardViewModel = hiltViewModel(),
) {
    val dashboard by viewModel.uiState.collectAsStateWithLifecycle()
    CompositionLocalProvider(LocalLedgerCurrency provides dashboard.currency) {
        ConsumptionCalendarContent(dashboard, initialMonth, onBack, modifier)
    }
}

@Composable
internal fun ConsumptionCalendarContent(
    dashboard: AnalyticsDashboardState,
    initialMonth: YearMonth,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val currencySymbol = currencySymbol(LocalLedgerCurrency.current)
    val colors = MaterialTheme.colorScheme
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
    BackHandler(onBack = onBack)
    Surface(modifier.fillMaxSize(), color = colors.background) {
        Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    LedgerIconButton(onClick = onBack) {
                        Icon(MaterialSymbols.ArrowBack, textResources.getString(cn.turingzy.ledger.R.string.ui_back_to_analytics), tint = colors.onSurface)
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(textResources.getString(cn.turingzy.ledger.R.string.ui_spending_calendar), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                if (dashboard.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (!dashboard.amountsAvailable) {
                    Text(textResources.getString(cn.turingzy.ledger.R.string.ui_exchange_rate_needed_refresh_or_enter_a_rate),
                        Modifier.padding(20.dp), color = colors.onSurfaceVariant)
                    return@Column
                }
                LazyColumn(
                    Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                ) {
                    item(key = "calendar") {
                        Surface(shape = RoundedCornerShape(20.dp), color = colors.surface,
                            border = BorderStroke(0.5.dp, colors.outlineVariant)) {
                            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(month.format(DateTimeFormatter.ofPattern(if (configuration.locales[0].language == "en") "MMM yyyy" else "yyyy年M月", configuration.locales[0])), Modifier.clip(RoundedCornerShape(10.dp))
                                        .clickable { showPicker = true }.padding(vertical = 6.dp),
                                        fontSize = 17.sp, textAlign = TextAlign.Start)
                                    Text(buildAnnotatedString {
                                        append(textResources.getString(cn.turingzy.ledger.R.string.calendar_month_expense, month.monthValue))
                                        withStyle(SpanStyle(color = colors.onSurface)) { append(data.totalExpense.toFormattedAmount(true, symbol = currencySymbol)) }
                                    }, Modifier.weight(1f).padding(start = 8.dp), fontSize = 13.sp, lineHeight = 18.sp,
                                        color = colors.onSurfaceVariant, textAlign = TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                ConsumptionMonthGrid(data, selectedDate, today, { selectedEpoch = it.toEpochDay() })
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text(textResources.getString(cn.turingzy.ledger.R.string.ui_less), fontSize = 10.sp, color = colors.onSurfaceVariant)
                                    (0..4).forEach { level ->
                                        Box(Modifier.size(10.dp).background(calendarHeatColor(level), RoundedCornerShape(3.dp)))
                                    }
                                    Text(textResources.getString(cn.turingzy.ledger.R.string.ui_more), fontSize = 10.sp, color = colors.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    item(key = "day") {
                        Row(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(textResources.getString(cn.turingzy.ledger.R.string.calendar_day_count, selectedDate.format(dayFormatter), entries.size), Modifier.weight(1f), fontSize = 14.sp, color = colors.onSurfaceVariant)
                            Text((data.expenses[selectedDate] ?: 0L).toFormattedAmount(true, symbol = currencySymbol), fontSize = 14.sp, color = colors.onSurface)
                        }
                    }
                    if (entries.isEmpty()) item(key = "empty") {
                        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = colors.surface) {
                            Text(if (selectedDate > today) textResources.getString(cn.turingzy.ledger.R.string.ui_future_date) else if (dashboard.loading) textResources.getString(cn.turingzy.ledger.R.string.ui_loading_transactions) else textResources.getString(cn.turingzy.ledger.R.string.ui_no_expenses_on_this_day),
                                Modifier.padding(20.dp), fontSize = 13.sp, color = colors.onSurfaceVariant)
                        }
                    }
                    itemsIndexed(entries, key = { _, entry -> entry.id }) { index, entry ->
                        val top = if (index == 0) 20.dp else 0.dp
                        val bottom = if (index == entries.lastIndex) 20.dp else 0.dp
                        val category = entry.category?.category?.name ?: entry.note
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom))
                            .background(colors.surface)) {
                            if (index > 0) HorizontalDivider(Modifier.padding(start = 62.dp, end = 14.dp), thickness = 0.5.dp, color = colors.outlineVariant)
                            TransactionRow(category,
                                listOfNotNull(entry.accountName, entry.note.takeIf { it.isNotBlank() && it != category }).joinToString(" · "),
                                entry.time?.format(timeFormatter).orEmpty(), entry.amount, entry.type,
                                entry.category?.icon?.categoryGlyph() ?: MaterialSymbols.Payments, currency = entry.currency)
                        }
                    }
                }
            }
        }
    }
    if (showPicker) PeriodPickerSheet(DateFilterType.MONTH, month.atDay(1), { showPicker = false },
        { _, date -> monthKey = YearMonth.from(date).toString(); showPicker = false }, showViewSelector = false)
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
                Text(label, Modifier.weight(1f).semantics { selected = highlighted }, fontSize = 12.sp, lineHeight = 16.sp,
                    fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Normal,
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
                            .selectable(selected = selected, enabled = !future, onClick = { onSelect(date) })
                            .semantics { contentDescription = "$date，支出${(data.expenses[date] ?: 0L).toFormattedAmount(true, symbol = currencySymbol)}" },
                            contentAlignment = Alignment.Center) {
                            Text(date.dayOfMonth.toString(), fontSize = 13.sp, lineHeight = 16.sp,
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
