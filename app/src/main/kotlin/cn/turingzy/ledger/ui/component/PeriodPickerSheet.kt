package cn.turingzy.ledger.ui.component

import cn.turingzy.ledger.model.DateFilterType
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PeriodPickerSheet(
    type: DateFilterType,
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onSelect: (DateFilterType, LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    showViewSelector: Boolean = true,
) {
    val views = listOf(DateFilterType.WEEK, DateFilterType.MONTH, DateFilterType.YEAR)
    var view by rememberSaveable(type) { mutableStateOf(type.takeIf { it in views } ?: DateFilterType.MONTH) }
    var contentView by remember { mutableStateOf(view) }
    val listStates = rememberSaveableStateHolder()
    LaunchedEffect(view) {
        // Keep list measurement out of the selector's slide animation.
        if (contentView != view) delay(220)
        contentView = view
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val height = LocalConfiguration.current.screenHeightDp.dp * 0.72f
    val today = LocalDate.now()
    fun apply(selectedView: DateFilterType, date: LocalDate) {
        onSelect(selectedView, periodStart(selectedView, date))
        scope.launch {
            sheetState.hide()
            onDismiss()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            Modifier.height(height).fillMaxWidth().testTag("ledgerPeriodPicker")
                .navigationBarsPadding().padding(horizontal = 12.dp),
        ) {
            Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                LedgerIconButton(onDismiss, modifier = Modifier.align(Alignment.CenterStart), showPressFeedback = false) {
                    Icon(MaterialSymbols.Close, "关闭日期选择")
                }
                if (showViewSelector) {
                    LedgerSegmentedControl(
                        labels = listOf("周", "月", "年"),
                        selectedIndex = views.indexOf(view),
                        onSelect = { view = views[it] },
                        modifier = Modifier.width(168.dp).testTag("periodViewSelector"), height = 28.dp,
                        fontSize = 13.sp, fontWeight = FontWeight.Normal,
                    )
                }
                val isCurrentPeriod = periodStart(view, initialDate) == periodStart(view, today)
                LedgerIconButton(
                    { apply(view, today) }, modifier = Modifier.align(Alignment.CenterEnd),
                    enabled = !isCurrentPeriod, showPressFeedback = false,
                ) {
                    Icon(
                        MaterialSymbols.Undo, view.returnLabel,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isCurrentPeriod) 0.25f else 1f),
                    )
                }
            }
            listStates.SaveableStateProvider(contentView) {
                PeriodList(
                    contentView, initialDate, today,
                    { date -> if (contentView == view) apply(contentView, date) },
                    Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PeriodList(
    type: DateFilterType,
    initialDate: LocalDate,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listType = if (type == DateFilterType.MONTH) DateFilterType.YEAR else type
    val series = remember(listType, initialDate) { PeriodSeries(listType, initialDate) }
    val precedingItems = if (type == DateFilterType.YEAR) 4 else if (type == DateFilterType.WEEK) 1 else 0
    val state = rememberLazyListState((series.indexOf(initialDate) - precedingItems).coerceAtLeast(0))
    val chosen = periodStart(type, initialDate)
    Column(modifier.fillMaxWidth()) {
        if (type == DateFilterType.WEEK) {
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
                listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日").forEach { day ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(day, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).testTag("ledgerPeriodList"),
            state = state,
            contentPadding = PaddingValues(top = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(if (type == DateFilterType.YEAR) 10.dp else 22.dp),
        ) {
            items(series.size, key = { series.dateAt(it).toEpochDay() }) { index ->
                val date = series.dateAt(index)
                when (type) {
                    DateFilterType.WEEK -> LedgerWeekOption(date, chosen == date, today, onSelect)
                    DateFilterType.MONTH -> LedgerMonthOptions(date.year, chosen, onSelect)
                    DateFilterType.YEAR -> PeriodOption(date, "${date.year}年", chosen == date, onSelect)
                    DateFilterType.RANGE -> Unit
                }
            }
        }
    }
}

@Composable
private fun LedgerWeekOption(date: LocalDate, chosen: Boolean, today: LocalDate, onSelect: (LocalDate) -> Unit) {
    val label = weekLabel(date) + if (date == weekStart(today)) "（本周）" else ""
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .periodSelection(date, chosen, onSelect),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            label, modifier = Modifier.padding(horizontal = 2.dp), fontSize = 13.sp,
            color = if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(if (chosen) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f) else MaterialTheme.colorScheme.background)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(7) { offset ->
                Box(
                    Modifier.weight(1f).height(36.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(date.plusDays(offset.toLong()).dayOfMonth.toString(), fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun LedgerMonthOptions(year: Int, chosen: LocalDate, onSelect: (LocalDate) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "${year}年", modifier = Modifier.padding(start = 4.dp, bottom = 6.dp), fontSize = 15.sp,
            color = if (chosen.year == year) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        (1..12).chunked(5).forEach { months ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                months.forEach { month ->
                    val date = LocalDate.of(year, month, 1)
                    PeriodOption(date, "${month}月", chosen == date, onSelect, Modifier.weight(1f))
                }
                repeat(5 - months.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PeriodOption(
    date: LocalDate,
    label: String,
    chosen: Boolean,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.background).periodSelection(date, chosen, onSelect),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 15.sp, fontWeight = if (chosen) FontWeight.Medium else FontWeight.Normal,
            color = if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun Modifier.periodSelection(date: LocalDate, chosen: Boolean, onSelect: (LocalDate) -> Unit): Modifier =
    testTag("ledgerPeriod-${date.toEpochDay()}")
        .semantics { selected = chosen; contentDescription = date.toString() }
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(date) }
