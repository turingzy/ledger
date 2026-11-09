package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@Composable
internal fun AnalyticsPeriodPicker(
    scope: AnalyticsTimeScope,
    selectedDate: LocalDate,
    onDismissRequest: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    var selected by remember(scope, selectedDate) { mutableStateOf(selectedDate) }
    var visibleMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    var yearPage by remember { mutableIntStateOf((selectedDate.year - 5).coerceIn(1, 9988)) }
    val colors = MaterialTheme.colorScheme
    val heading = when (scope) {
        AnalyticsTimeScope.WEEK -> "选择周"
        AnalyticsTimeScope.MONTH -> "选择月份"
        AnalyticsTimeScope.YEAR -> "选择年份"
    }
    val monday = selected.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val summary = when (scope) {
        AnalyticsTimeScope.WEEK -> "${monday.format(DateTimeFormatter.ofPattern("yyyy.M.d"))} — ${
            monday.plusDays(
                6
            ).format(DateTimeFormatter.ofPattern("yyyy.M.d"))
        }"

        AnalyticsTimeScope.MONTH -> selected.format(DateTimeFormatter.ofPattern("yyyy年M月"))
        AnalyticsTimeScope.YEAR -> "${selected.year}年"
    }
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(shape = RoundedCornerShape(28.dp), color = colors.surface) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(13.dp))
                            .background(colors.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            null,
                            tint = colors.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(heading, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (scope == AnalyticsTimeScope.WEEK) "点击日期，选中所在整周" else "选择要查看的统计周期",
                            fontSize = 11.sp,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val canPrevious =
                        if (scope == AnalyticsTimeScope.YEAR) yearPage > 1 else visibleMonth.year > 1
                    val canNext =
                        if (scope == AnalyticsTimeScope.YEAR) yearPage < 9988 else visibleMonth.year < 9998
                    IconButton(enabled = canPrevious, onClick = {
                        when (scope) {
                            AnalyticsTimeScope.WEEK -> visibleMonth = visibleMonth.minusMonths(1)
                            AnalyticsTimeScope.MONTH -> visibleMonth = visibleMonth.minusYears(1)
                            AnalyticsTimeScope.YEAR -> yearPage = (yearPage - 12).coerceAtLeast(1)
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "上一页",
                            tint = colors.primary
                        )
                    }
                    Text(
                        when (scope) {
                            AnalyticsTimeScope.WEEK -> "${visibleMonth.year}年${visibleMonth.monthValue}月"
                            AnalyticsTimeScope.MONTH -> "${visibleMonth.year}年"
                            AnalyticsTimeScope.YEAR -> "$yearPage — ${yearPage + 11}年"
                        },
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    )
                    IconButton(enabled = canNext, onClick = {
                        when (scope) {
                            AnalyticsTimeScope.WEEK -> visibleMonth = visibleMonth.plusMonths(1)
                            AnalyticsTimeScope.MONTH -> visibleMonth = visibleMonth.plusYears(1)
                            AnalyticsTimeScope.YEAR -> yearPage = (yearPage + 12).coerceAtMost(9988)
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            "下一页",
                            tint = colors.primary
                        )
                    }
                }
                if (scope == AnalyticsTimeScope.WEEK) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                                Box(
                                    Modifier.weight(1f).height(28.dp),
                                    contentAlignment = Alignment.Center
                                ) { Text(it, fontSize = 11.sp, color = colors.onSurfaceVariant) }
                            }
                        }
                        val first = visibleMonth.atDay(1)
                            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        (0 until 42).chunked(7).forEach { indices ->
                            Row(
                                Modifier.fillMaxWidth().selectableGroup(),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                indices.forEach { index ->
                                    val date = first.plusDays(index.toLong())
                                    val active =
                                        !date.isBefore(monday) && !date.isAfter(monday.plusDays(6))
                                    PeriodCell(
                                        label = date.dayOfMonth.toString(),
                                        selected = active,
                                        onClick = { selected = date },
                                        modifier = Modifier.weight(1f),
                                        muted = YearMonth.from(date) != visibleMonth,
                                        description = date.format(DateTimeFormatter.ofPattern("yyyy年M月d日")),
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        (0 until 12).chunked(3).forEach { indices ->
                            Row(
                                Modifier.fillMaxWidth().selectableGroup(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                indices.forEach { index ->
                                    val date =
                                        if (scope == AnalyticsTimeScope.MONTH) visibleMonth.atDay(1)
                                            .withMonth(index + 1)
                                        else LocalDate.of(yearPage + index, 1, 1)
                                    val active =
                                        selected.year == date.year && (scope == AnalyticsTimeScope.YEAR || selected.month == date.month)
                                    PeriodCell(
                                        if (scope == AnalyticsTimeScope.MONTH) "${index + 1}月" else "${date.year}年",
                                        active,
                                        { selected = date },
                                        Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(colors.primary.copy(alpha = 0.07f)).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("已选范围", fontSize = 10.sp, color = colors.onSurfaceVariant)
                    Text(
                        summary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primary
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        selected = LocalDate.now()
                        visibleMonth = YearMonth.from(selected)
                        yearPage = (selected.year - 5).coerceIn(1, 9988)
                    }) {
                        Text(
                            when (scope) {
                                AnalyticsTimeScope.WEEK -> "本周"; AnalyticsTimeScope.MONTH -> "本月"; AnalyticsTimeScope.YEAR -> "今年"
                            }
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismissRequest) {
                        Text(
                            "取消",
                            color = colors.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { onConfirm(selected) },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("确定") }
                }
            }
        }
    }
}

@Composable
private fun PeriodCell(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    muted: Boolean = false,
    description: String = label,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier.height(40.dp).clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) colors.primary.copy(alpha = 0.13f) else colors.surfaceVariant.copy(
                    alpha = 0.25f
                )
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = when {
                selected -> colors.primary; muted -> colors.onSurfaceVariant.copy(alpha = 0.45f); else -> colors.onSurface
            }
        )
    }
}
