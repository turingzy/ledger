package cn.turingzy.ledger.ui.feature.ledger

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import cn.turingzy.ledger.ui.component.MaterialSymbols
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

@Composable
internal fun RecordDateTimePicker(
    date: LocalDate,
    hour: String,
    minute: String,
    onDateChange: (LocalDate) -> Unit,
    onHourChange: (String) -> Unit,
    onMinuteChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RecordDateCalendar(date, onDateChange)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            RecordTimeWheel("小时", hour, 24, onHourChange)
            Text(":", Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.titleLarge)
            RecordTimeWheel("分钟", minute, 60, onMinuteChange)
        }
    }
}

private enum class RecordCalendarPanel { DAYS, MONTHS, YEARS }

@Composable
private fun RecordDateCalendar(selectedDate: LocalDate, onSelect: (LocalDate) -> Unit) {
    var month by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    var panel by remember { mutableStateOf(RecordCalendarPanel.DAYS) }
    val colors = MaterialTheme.colorScheme
    val days = remember(month) {
        val first = month.atDay(1)
        val gridStart = first.minusDays(first.dayOfWeek.value - 1L)
        List(42) { gridStart.plusDays(it.toLong()) }
    }
    val years = remember(month.year) {
        ((month.year - 100).coerceAtLeast(1)..(month.year + 20).coerceAtMost(9999)).toList()
    }
    val months = remember { (1..12).toList() }
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RecordCalendarHeading(
                text = "${month.year}年", description = "选择年份", expanded = panel == RecordCalendarPanel.YEARS,
                onClick = { panel = if (panel == RecordCalendarPanel.YEARS) RecordCalendarPanel.DAYS else RecordCalendarPanel.YEARS },
            )
            RecordCalendarHeading(
                text = "${month.monthValue}月", description = "选择月份", expanded = panel == RecordCalendarPanel.MONTHS,
                onClick = { panel = if (panel == RecordCalendarPanel.MONTHS) RecordCalendarPanel.DAYS else RecordCalendarPanel.MONTHS },
            )
        }
        Box(Modifier.height(204.dp)) {
            when (panel) {
                RecordCalendarPanel.YEARS -> RecordPeriodChoices(
                    values = years,
                    selected = month.year, tag = "recordYearGrid", description = { "年份$it" },
                    onSelect = { year ->
                        month = month.withYear(year)
                        onSelect(recordDateInMonth(selectedDate, month))
                        panel = RecordCalendarPanel.MONTHS
                    },
                )
                RecordCalendarPanel.MONTHS -> RecordPeriodChoices(
                    values = months, selected = month.monthValue,
                    tag = "recordMonthGrid", description = { "月份${it.toString().padStart(2, '0')}" },
                    onSelect = { selectedMonth ->
                        month = month.withMonth(selectedMonth)
                        onSelect(recordDateInMonth(selectedDate, month))
                        panel = RecordCalendarPanel.DAYS
                    },
                )
                RecordCalendarPanel.DAYS -> Column {
                    Row(Modifier.fillMaxWidth()) {
                        listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                            Text(it, Modifier.weight(1f).padding(vertical = 4.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                    LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.height(180.dp), userScrollEnabled = false) {
                        items(days, key = { it.toEpochDay() }) { date ->
                            val chosen = date == selectedDate
                            Box(
                                Modifier.height(30.dp).semantics { selected = chosen; contentDescription = date.toString() }
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = LocalIndication.current, role = Role.Button) {
                                        month = YearMonth.from(date)
                                        onSelect(date)
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    Modifier.size(28.dp).clip(CircleShape).background(if (chosen) colors.primary else colors.surface.copy(alpha = 0f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        date.dayOfMonth.toString(), style = MaterialTheme.typography.bodyMedium,
                                        color = when {
                                            chosen -> colors.onPrimary
                                            date.month == month.month -> colors.onSurface
                                            else -> colors.onSurfaceVariant.copy(alpha = 0.4f)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordCalendarHeading(text: String, description: String, expanded: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.semantics {
            contentDescription = description
            stateDescription = if (expanded) "展开" else "收起"
        }.clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null,
            role = Role.Button, onClick = onClick,
        ).padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
        Icon(MaterialSymbols.ArrowDropDown, null, Modifier.size(24.dp).rotate(if (expanded) 180f else 0f))
    }
}

internal fun recordDateInMonth(date: LocalDate, month: YearMonth): LocalDate = month.atDay(minOf(date.dayOfMonth, month.lengthOfMonth()))

@Composable
private fun RecordPeriodChoices(values: List<Int>, selected: Int, tag: String, description: (Int) -> String, onSelect: (Int) -> Unit) {
    val state = rememberLazyGridState(initialFirstVisibleItemIndex = (values.indexOf(selected) - 8).coerceAtLeast(0) / 4 * 4)
    LazyVerticalGrid(
        GridCells.Fixed(4), Modifier.fillMaxWidth().height(204.dp).testTag(tag), state = state,
        horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(values, key = { it }) { value ->
            Box(
                Modifier.height(44.dp)
                    .semantics { contentDescription = description(value); this.selected = value == selected }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() }, indication = null,
                    ) { onSelect(value) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    value.toString(), style = MaterialTheme.typography.bodyMedium,
                    color = if (value == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun RecordTimeWheel(label: String, value: String, count: Int, onChange: (String) -> Unit) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = value.toIntOrNull()?.coerceIn(0, count - 1) ?: 0)
    val currentOnChange by rememberUpdatedState(onChange)
    val selectedIndex by remember(state) {
        derivedStateOf {
            val layout = state.layoutInfo
            val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
            layout.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - center) }?.index
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { selectedIndex }.distinctUntilChanged().collect { index ->
            if (index != null) currentOnChange(index.toString().padStart(2, '0'))
        }
    }
    Box(Modifier.width(80.dp).height(120.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
        LazyColumn(
            state = state, flingBehavior = rememberSnapFlingBehavior(state),
            contentPadding = PaddingValues(vertical = 40.dp),
            modifier = Modifier.fillMaxWidth().height(120.dp).semantics {
                contentDescription = label
                stateDescription = value
            },
        ) {
            items((0 until count).toList(), key = { it }) { number ->
                Box(Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) {
                    Text(
                        number.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.titleLarge,
                        color = if (number == selectedIndex) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    )
                }
            }
        }
    }
}
