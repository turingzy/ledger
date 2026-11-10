package cn.turingzy.ledger.ui.feature.settings

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.CancellationException
import timber.log.Timber

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.ui.component.LedgerSegmentedControl
import cn.turingzy.ledger.ui.component.calendar.CalendarPickerState
import cn.turingzy.ledger.ui.component.calendar.CalendarSelection
import cn.turingzy.ledger.ui.component.calendar.MonthCalendarDatePickerCard
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class ExportFormat(val label: String) { EXCEL("Excel"), CSV("CSV") }
private enum class BillTransferStatus { IDLE, RUNNING, COMPLETED, CANCELLED }

@Composable
internal fun BillTransferSheet(rules: List<ParseRulePreview>, onDismissRequest: () -> Unit) {
    val today = remember { LocalDate.now() }
    val context = LocalContext.current
    var importing by remember { mutableStateOf(true) }
    var fileName by remember { mutableStateOf<String?>(null) }
    var selectedRuleId by remember { mutableStateOf<Long?>(null) }
    var choosingRule by remember { mutableStateOf(false) }
    val selectedRule = rules.firstOrNull { it.id == selectedRuleId }
    var allBills by remember { mutableStateOf(true) }
    var range by remember { mutableStateOf(CalendarSelection(today.withDayOfMonth(1), today)) }
    var format by remember { mutableStateOf(ExportFormat.EXCEL) }
    var choosingRange by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(BillTransferStatus.IDLE) }
    var progress by remember { mutableIntStateOf(0) }
    var operation by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val running = status == BillTransferStatus.RUNNING

    fun resetPreview() {
        status = BillTransferStatus.IDLE
        progress = 0
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            fileName = billFileName(context, uri)
            resetPreview()
        }
    }

    SettingsBottomSheet(
        title = when {
            choosingRange -> "导出范围"
            choosingRule -> "解析规则"
            else -> "账单导入与导出"
        },
        onDismissRequest = onDismissRequest,
        onBack = if (choosingRange || choosingRule) ({ choosingRange = false; choosingRule = false }) else null,
    ) {
        Column(
            Modifier.fillMaxWidth().animateContentSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (choosingRule) {
                Surface(shape = RoundedCornerShape(16.dp), color = colors.surfaceVariant.copy(alpha = 0.45f)) {
                    LazyColumn(Modifier.heightIn(max = 360.dp).padding(horizontal = 16.dp)) {
                        item(key = "automatic") {
                            ImportRuleChoice("自动识别", selectedRule == null) {
                                selectedRuleId = null
                                choosingRule = false
                                resetPreview()
                            }
                        }
                        items(rules, key = { it.id }) { rule ->
                            ImportRuleChoice(rule.name, selectedRule?.id == rule.id) {
                                selectedRuleId = rule.id
                                choosingRule = false
                                resetPreview()
                            }
                        }
                    }
                }
                if (rules.isEmpty()) {
                    Text("可在“账单解析规则”中新增规则。", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            } else if (choosingRange) {
                val calendarState = remember(range) {
                    CalendarPickerState(YearMonth.from(range.end), range).apply { editing = true }
                }
                MonthCalendarDatePickerCard(
                    state = calendarState,
                    onDismiss = { choosingRange = false },
                    onConfirm = { start, end ->
                        range = CalendarSelection(start, end)
                        choosingRange = false
                        resetPreview()
                    },
                )
            } else {
                LedgerSegmentedControl(
                    labels = listOf("导入", "导出"),
                    selectedIndex = if (importing) 0 else 1,
                    onSelect = { importing = it == 0; resetPreview() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !running,
                )
                if (importing) {
                    Surface(shape = RoundedCornerShape(16.dp), color = colors.surfaceVariant.copy(alpha = 0.45f)) {
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            BillImportRow("账单文件", fileName ?: "选择文件", enabled = !running) {
                                filePicker.launch(arrayOf(
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "application/vnd.ms-excel", "text/csv", "text/comma-separated-values", "text/plain",
                                ))
                            }
                            HorizontalDivider(thickness = 0.5.dp, color = colors.outlineVariant)
                            BillImportRow("解析规则", selectedRule?.name ?: "自动识别", enabled = !running) { choosingRule = true }
                        }
                    }
                    Text("支持 Excel、CSV 账单文件。当前仅演示导入流程，不会读取或写入账单。", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                } else {
                    Text("导出范围", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    LedgerSegmentedControl(
                        labels = listOf("全部账单", "指定范围"),
                        selectedIndex = if (allBills) 0 else 1,
                        onSelect = { allBills = it == 0; resetPreview() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !running,
                    )
                    if (!allBills) {
                        Surface(shape = RoundedCornerShape(16.dp), color = colors.surfaceVariant.copy(alpha = 0.45f)) {
                            Column(Modifier.padding(horizontal = 16.dp)) {
                                ExportDateRow("开始日期", range.start, enabled = !running) { choosingRange = true }
                                HorizontalDivider(thickness = 0.5.dp, color = colors.outlineVariant)
                                ExportDateRow("结束日期", range.end, enabled = !running) { choosingRange = true }
                            }
                        }
                    }
                    Text("导出格式", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    LedgerSegmentedControl(
                        labels = ExportFormat.entries.map { it.label },
                        selectedIndex = ExportFormat.entries.indexOf(format),
                        onSelect = { format = ExportFormat.entries[it]; resetPreview() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !running,
                    )
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    enabled = !importing || fileName != null,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (running) colors.error else colors.outlineVariant),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (running) colors.error.copy(alpha = 0.08f) else colors.surfaceVariant.copy(alpha = 0.45f),
                        contentColor = if (running) colors.error else colors.onSurface,
                    ),
                    onClick = {
                        if (running) {
                            operation?.cancel()
                            operation = null
                            status = BillTransferStatus.CANCELLED
                        } else {
                            status = BillTransferStatus.RUNNING
                            progress = 0
                            operation = scope.launch {
                                for (percent in 1..100) {
                                    delay(35)
                                    progress = percent
                                }
                                status = BillTransferStatus.COMPLETED
                                operation = null
                            }
                        }
                    },
                ) {
                    Text(
                        if (running) { if (importing) "取消导入" else "取消导出" }
                        else { if (importing) "导入账单" else "导出账单" },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                if (running || status == BillTransferStatus.COMPLETED) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape),
                            color = colors.tertiary,
                            trackColor = colors.surfaceVariant,
                            strokeCap = StrokeCap.Round,
                            gapSize = 0.dp,
                            drawStopIndicator = {},
                        )
                        Text("$progress%", Modifier.width(40.dp), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                }
                val message = when (status) {
                    BillTransferStatus.IDLE -> null
                    BillTransferStatus.RUNNING -> if (importing) "正在演示账单导入…" else "正在导出${if (allBills) "全部账单" else "所选范围账单"}…"
                    BillTransferStatus.COMPLETED -> if (importing) "导入演示完成，未更改账目数据" else "${format.label} 导出演示完成，未生成实际文件"
                    BillTransferStatus.CANCELLED -> if (importing) "已取消导入" else "已取消导出"
                }
                if (message != null) Text(message, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ExportDateRow(label: String, date: LocalDate, enabled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(date.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")), color = colors.onSurfaceVariant)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "选择导出日期范围", tint = colors.onSurfaceVariant)
    }
}

@Composable
private fun BillImportRow(label: String, value: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, Modifier.width(88.dp), style = MaterialTheme.typography.bodyLarge)
        Text(value, Modifier.weight(1f), textAlign = TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.onSurfaceVariant)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = colors.onSurfaceVariant)
    }
}

@Composable
private fun ImportRuleChoice(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        if (selected) Icon(Icons.Default.Check, "已选择", tint = MaterialTheme.colorScheme.tertiary)
    }
}

private fun billFileName(context: Context, uri: Uri): String = try {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
    } ?: "已选择文件"
} catch (error: CancellationException) {
    throw error
} catch (error: Exception) {
    Timber.w(error, "Unable to read selected bill file name")
    "已选择文件"
}
