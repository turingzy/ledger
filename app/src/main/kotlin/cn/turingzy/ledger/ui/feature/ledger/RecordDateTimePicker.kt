package cn.turingzy.ledger.ui.feature.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.ui.component.LedgerSegmentedControl
import java.time.LocalDate
import java.time.YearMonth

@Composable
internal fun RecordDateTimePicker(
    date: LocalDate,
    hour: String,
    minute: String,
    onDateChange: (LocalDate) -> Unit,
    onHourChange: (String) -> Unit,
    onMinuteChange: (String) -> Unit,
) {
    var tab by remember { mutableStateOf(0) }
    val focusManager = LocalFocusManager.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LedgerSegmentedControl(
            labels = listOf("日期", "时间"), selectedIndex = tab,
            onSelect = { focusManager.clearFocus(); tab = it }, modifier = Modifier.fillMaxWidth(),
        )
        if (tab == 0) {
            RecordDateCalendar(date, onDateChange)
        } else {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RecordTimeField("小时", hour, onHourChange)
                Text("：", Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.headlineMedium)
                RecordTimeField("分钟", minute, onMinuteChange)
            }
            Text("24 小时制", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RecordDateCalendar(selectedDate: LocalDate, onSelect: (LocalDate) -> Unit) {
    var month by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    val colors = MaterialTheme.colorScheme
    val first = month.atDay(1)
    val gridStart = first.minusDays(first.dayOfWeek.value - 1L)
    val days = List(42) { gridStart.plusDays(it.toLong()) }
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { month = month.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "上个月")
            }
            Text("${month.year}年${month.monthValue}月", Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
            IconButton(onClick = { month = month.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "下个月")
            }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                Text(it, Modifier.weight(1f).padding(vertical = 6.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
        LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.height(216.dp), userScrollEnabled = false) {
            items(days, key = { it.toEpochDay() }) { date ->
                val chosen = date == selectedDate
                Box(
                    Modifier.height(36.dp).semantics { selected = chosen; contentDescription = date.toString() }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button,
                        ) { onSelect(date) },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).background(if (chosen) colors.onSurface else colors.surface.copy(alpha = 0f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            date.dayOfMonth.toString(), style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                chosen -> colors.surface
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

@Composable
private fun RecordTimeField(label: String, value: String, onChange: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicTextField(
            value = value,
            onValueChange = { input -> if (input.length <= 2 && input.all { it in '0'..'9' }) onChange(input) },
            textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center, color = colors.onSurface),
            cursorBrush = SolidColor(colors.onSurface), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            modifier = Modifier.width(80.dp).clip(RoundedCornerShape(16.dp))
                .background(colors.surfaceVariant.copy(alpha = 0.45f)).padding(vertical = 16.dp)
                .semantics { contentDescription = label },
        )
        Text(label, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }
}
