package cn.turingzy.ledger.ui.component.picker

import cn.turingzy.ledger.ui.theme.LedgerStyle
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.indication
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
import cn.turingzy.ledger.ui.component.base.DropdownIndicatorBase
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

/**
 * 日期与时间复合选择器组件。
 *
 * 整合上方的日期日历视图 [LedgerDatePicker] 与下方的时/分滚轮选择器 [RecordTimeWheel]。
 *
 * @param date 当前选中的日期。
 * @param hour 当前选中的小时字符串（两位数字）。
 * @param minute 当前选中的分钟字符串（两位数字）。
 * @param onDateChange 日期变更回调。
 * @param onHourChange 小时变更回调。
 * @param onMinuteChange 分钟变更回调。
 */
@Composable
internal fun LedgerDateTimePicker(
    date: LocalDate,
    hour: String,
    minute: String,
    onDateChange: (LocalDate) -> Unit,
    onHourChange: (String) -> Unit,
    onMinuteChange: (String) -> Unit,
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    Column(verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.medium)) {
        LedgerDatePicker(date, onDateChange)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            RecordTimeWheel(textResources.getString(cn.turingzy.ledger.R.string.ui_hour), hour, 24, onHourChange)
            Text(":", Modifier.padding(horizontal = LedgerStyle.spacing.large), style = MaterialTheme.typography.titleLarge)
            RecordTimeWheel(textResources.getString(cn.turingzy.ledger.R.string.ui_minute), minute, 60, onMinuteChange)
        }
    }
}

/**
 * 记账日历选择面板视图类型。
 */
private enum class RecordCalendarPanel {
    /** 日期网格视图 */
    DAYS,
    /** 月份选择视图 */
    MONTHS,
    /** 年份选择视图 */
    YEARS
}

/**
 * 单日日历选择器。
 *
 * 提供日期网格展示，并支持点击年月标题快速切换到年份列表或月份列表选择面板。
 *
 * @param selectedDate 当前选中的日期。
 * @param onSelect 选中某日时的回调。
 */
@Composable
internal fun LedgerDatePicker(selectedDate: LocalDate, onSelect: (LocalDate) -> Unit) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    var month by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    var panel by remember { mutableStateOf(RecordCalendarPanel.DAYS) }
    val colors = MaterialTheme.colorScheme
    val days = remember(month) {
        val first = month.atDay(1)
        val gridStart = first.minusDays(first.dayOfWeek.sundayFirstColumn().toLong())
        List(42) { gridStart.plusDays(it.toLong()) }
    }
    val years = remember(month.year) {
        ((month.year - 100).coerceAtLeast(1)..(month.year + 20).coerceAtMost(9999)).toList()
    }
    val months = remember { (1..12).toList() }
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RecordCalendarHeading(
                text = "${month.year}年", description = textResources.getString(cn.turingzy.ledger.R.string.ui_select_year), expanded = panel == RecordCalendarPanel.YEARS,
                onClick = { panel = if (panel == RecordCalendarPanel.YEARS) RecordCalendarPanel.DAYS else RecordCalendarPanel.YEARS },
            )
            RecordCalendarHeading(
                text = "${month.monthValue}月", description = textResources.getString(cn.turingzy.ledger.R.string.ui_select_month), expanded = panel == RecordCalendarPanel.MONTHS,
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
                        MonthGridWeekdays.map(textResources::getString).forEach {
                            Text(it, Modifier.weight(1f).padding(vertical = LedgerStyle.spacing.extraSmall), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                    LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.height(180.dp), userScrollEnabled = false) {
                        items(days, key = { it.toEpochDay() }) { date ->
                            val chosen = date == selectedDate
                            // Keep the whole cell tappable, but draw feedback on the date circle.
                            val pressSource = remember { MutableInteractionSource() }
                            Box(
                                Modifier.height(30.dp).semantics { selected = chosen; contentDescription = date.toString() }
                                    .clickable(interactionSource = pressSource, indication = null, role = Role.Button) {
                                        month = YearMonth.from(date)
                                        onSelect(date)
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    Modifier.size(28.dp).clip(CircleShape)
                                        .background(if (chosen) colors.primary else colors.surface.copy(alpha = 0f))
                                        .indication(pressSource, LocalIndication.current),
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

/**
 * 日历顶部的年/月可点击标题项。
 *
 * @param text 标题文案（如 "2024年" 或 "5月"）。
 * @param description 无障碍语义描述。
 * @param expanded 当前标题对应的展开选择面板是否处于打开状态。
 * @param onClick 点击事件回调。
 */
@Composable
private fun RecordCalendarHeading(text: String, description: String, expanded: Boolean, onClick: () -> Unit) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    Row(
        Modifier.semantics {
            contentDescription = description
            stateDescription = if (expanded) textResources.getString(cn.turingzy.ledger.R.string.ui_expanded) else textResources.getString(cn.turingzy.ledger.R.string.ui_collapsed)
        }.clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null,
            role = Role.Button, onClick = onClick,
        ).padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.tiny),
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
        DropdownIndicatorBase(expanded, opensUp = false)
    }
}

/**
 * 将 [date] 的日（dayOfMonth）限制在目标 [month] 的有效天数内并生成新的 [LocalDate]。
 *
 * 避免如 1 月 31 日切换到 2 月时产生非法日期的越界异常。
 *
 * @param date 原始日期。
 * @param month 目标年月。
 * @return 目标年月中对应日（不超过当月末日）的日期对象。
 */
internal fun recordDateInMonth(date: LocalDate, month: YearMonth): LocalDate = month.atDay(minOf(date.dayOfMonth, month.lengthOfMonth()))

/**
 * 年份或月份网格选择面板。
 *
 * @param values 待选项数值列表（如 1..12 或年份列表）。
 * @param selected 当前选中的数值。
 * @param tag 布局测试标签。
 * @param description 获取无障碍描述文案的函数。
 * @param onSelect 选中数值时的回调。
 */
@Composable
private fun RecordPeriodChoices(values: List<Int>, selected: Int, tag: String, description: (Int) -> String, onSelect: (Int) -> Unit) {
    val state = rememberLazyGridState(initialFirstVisibleItemIndex = (values.indexOf(selected) - 8).coerceAtLeast(0) / 4 * 4)
    LazyVerticalGrid(
        GridCells.Fixed(4), Modifier.fillMaxWidth().height(204.dp).testTag(tag), state = state,
        horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.extraSmall), verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.extraSmall),
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

/**
 * 时间数字滚轮组件（用于小时或分钟）。
 *
 * 带有居中指示条和吸附滚动效果（Snap fling behavior）。
 *
 * @param label 无障碍标签（如 "小时"、"分钟"）。
 * @param value 当前选中的数值字符串。
 * @param count 选项总数（例如小时为 24，分钟为 60）。
 * @param onChange 滚动选中数值变更时的回调。
 */
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
        Box(Modifier.fillMaxWidth().height(40.dp).clip(LedgerStyle.shapes.field).background(LedgerStyle.colors.overlayFieldBackground))
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
