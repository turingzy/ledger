package cn.turingzy.ledger.ui.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import cn.turingzy.ledger.ui.component.LedgerIconButton
import cn.turingzy.ledger.ui.component.LedgerSegmentedControl
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.LedgerDateTimePicker
import cn.turingzy.ledger.ui.theme.LedgerTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Job
import android.net.Uri
import cn.turingzy.ledger.model.AccountWithTemplate
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import java.time.ZoneId
import kotlinx.coroutines.launch

private enum class ExportFormat(val label: String) { EXCEL("Excel"), CSV("CSV") }
private enum class BillTransferPicker { RULE, ACCOUNT, START, END }
private enum class BillTransferStatus { IDLE, RUNNING, COMPLETED, CANCELLED }

@Composable
internal fun BillTransferScreen(
    rules: List<ParseRulePreview>, onDismissRequest: () -> Unit,
    accounts: List<AccountWithTemplate> = emptyList(),
    onImport: suspend (Uri, ParseRulePreview, Long?) -> Int = { _, _, _ -> 0 },
    onExport: suspend (Uri, Boolean, Long, Long) -> Int = { _, _, _, _ -> 0 },
) {
    val today = remember { LocalDate.now() }
    val context = LocalContext.current
    var importing by remember { mutableStateOf(true) }
    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var accountId by remember { mutableStateOf<Long?>(null) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var fileName by remember { mutableStateOf<String?>(null) }
    var selectedRuleId by remember { mutableStateOf<Long?>(null) }
    var picker by remember { mutableStateOf<BillTransferPicker?>(null) }
    val selectedRule = rules.firstOrNull { it.id == selectedRuleId }
    var allBills by remember { mutableStateOf(true) }
    var rangeStart by remember { mutableStateOf(today.withDayOfMonth(1).atStartOfDay()) }
    var rangeEnd by remember { mutableStateOf(today.atTime(23, 59, 59)) }
    var format by remember { mutableStateOf(ExportFormat.EXCEL) }
    var status by remember { mutableStateOf(BillTransferStatus.IDLE) }
    var exportCoolingDown by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var operation by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val running = status == BillTransferStatus.RUNNING

    fun resetResult() {
        status = BillTransferStatus.IDLE
        progress = 0
        resultMessage = null
    }

    LaunchedEffect(exportCoolingDown) {
        if (exportCoolingDown) {
            delay(10_000)
            exportCoolingDown = false
            if (status == BillTransferStatus.COMPLETED && !importing) resetResult()
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            fileUri = uri
            fileName = selectedTransferFileName(context, uri)
            resetResult()
        }
    }

    fun runTransfer(destination: Uri? = null) {
        if (!importing && exportCoolingDown) return
        status = BillTransferStatus.RUNNING
        resultMessage = null
        operation = scope.launch {
            try {
                val count = if (importing) onImport(requireNotNull(fileUri), requireNotNull(selectedRule), accountId)
                else onExport(requireNotNull(destination), format == ExportFormat.EXCEL,
                    if (allBills) Long.MIN_VALUE else rangeStart.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    if (allBills) Long.MAX_VALUE else rangeEnd.withSecond(59).withNano(999_000_000).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                status = BillTransferStatus.COMPLETED
                if (!importing) exportCoolingDown = true
                progress = 100
                resultMessage = "已${if (importing) "导入" else "导出"} $count 笔账单"
            } catch (cancelled: CancellationException) {
                status = BillTransferStatus.CANCELLED
                throw cancelled
            } catch (error: Exception) {
                Timber.e(error, "Bill transfer failed")
                status = BillTransferStatus.IDLE
                resultMessage = error.message ?: "操作失败，请重试"
            } finally { operation = null }
        }
    }
    val csvDestination = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) runTransfer(uri)
    }
    val excelDestination = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
        if (uri != null) runTransfer(uri)
    }

    SettingsPage(title = "账单导入与导出", onDismissRequest = onDismissRequest) {
        Column(
            Modifier.fillMaxWidth().settingsPageScroll(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LedgerSegmentedControl(
                    labels = listOf("导入", "导出"),
                    selectedIndex = if (importing) 0 else 1,
                    onSelect = { importing = it == 0; resetResult() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !running,
                )
                if (importing) Text("支持 Excel(XLSX)、CSV 账单", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            if (importing) {
                SettingsTransferFileRow(fileName = fileName, enabled = !running) {
                    filePicker.launch(arrayOf(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "text/csv", "text/comma-separated-values", "text/plain",
                    ))
                }
                BillTransferCard {
                    BillImportRow("解析规则", selectedRule?.name ?: "请选择", enabled = !running) {
                        picker = BillTransferPicker.RULE
                    }
                    if (selectedRule?.accountId == null) {
                        BillTransferDivider()
                        BillImportRow("导入账户", accounts.firstOrNull { it.account.id == accountId }?.account?.name ?: "使用文件中的账户", enabled = !running) {
                            picker = BillTransferPicker.ACCOUNT
                        }
                    }
                }
            } else {
                Text("导出范围", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BillTransferOption("全部账单", allBills, !running, Modifier.weight(1f)) { allBills = true; resetResult() }
                    BillTransferOption("指定范围", !allBills, !running, Modifier.weight(1f)) { allBills = false; resetResult() }
                }
                if (!allBills) {
                    BillTransferCard {
                        BillImportRow("开始日期", rangeStart.format(billDateFormat), enabled = !running) { picker = BillTransferPicker.START }
                        BillTransferDivider()
                        BillImportRow("结束日期", rangeEnd.format(billDateFormat), enabled = !running) { picker = BillTransferPicker.END }
                    }
                }
                Text("导出格式", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExportFormat.entries.forEach { option ->
                        BillTransferOption(option.label, format == option, !running, Modifier.weight(1f)) {
                            format = option
                            resetResult()
                        }
                    }
                }
            }
            SettingsTransferAction(
                title = if (running) { if (importing) "取消导入" else "取消导出" }
                    else { if (importing) "导入账单" else "导出账单" },
                enabled = (importing || !exportCoolingDown) && (running || !importing || (fileName != null && selectedRule != null)),
                running = running,
                progress = progress.takeIf { status == BillTransferStatus.COMPLETED },
                onClick = {
                    if (running) {
                        operation?.cancel()
                        operation = null
                        status = BillTransferStatus.CANCELLED
                    } else {
                        if (importing) runTransfer()
                        else if (format == ExportFormat.EXCEL) excelDestination.launch("Ledger-$today.xlsx")
                        else csvDestination.launch("Ledger-$today.csv")
                    }
                },
            )
            val message = when (status) {
                BillTransferStatus.IDLE -> null
                BillTransferStatus.RUNNING -> null
                BillTransferStatus.COMPLETED -> null
                BillTransferStatus.CANCELLED -> if (importing) "已取消导入" else "已取消导出"
            }
            if (resultMessage != null) Text(resultMessage.orEmpty(), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            if (message != null) Text(message, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }

    when (picker) {
        BillTransferPicker.ACCOUNT -> BillTransferDialog(
            title = "导入账户", onDismiss = { picker = null }, onConfirm = { picker = null },
        ) {
            ImportRuleChoice("使用文件中的账户", accountId == null) { accountId = null }
            accounts.forEach { item ->
                ImportRuleChoice(item.account.name, accountId == item.account.id) { accountId = item.account.id }
            }
        }
        BillTransferPicker.RULE -> {
            var pendingRuleId by remember { mutableStateOf(selectedRule?.id) }
            BillTransferDialog(
                title = "解析规则",
                onDismiss = { picker = null },
                onConfirm = { selectedRuleId = pendingRuleId; picker = null; resetResult() },
                confirmEnabled = pendingRuleId != null,
            ) {
                Column(Modifier.selectableGroup()) {
                    rules.forEachIndexed { index, rule ->
                        if (index > 0) BillTransferDivider()
                        ImportRuleChoice(rule.name, pendingRuleId == rule.id) { pendingRuleId = rule.id }
                    }
                }
                if (rules.isEmpty()) {
                    Text(
                        "可在“账单解析规则”中新增规则。",
                        Modifier.padding(vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }
        BillTransferPicker.START, BillTransferPicker.END -> {
            val editingStart = picker == BillTransferPicker.START
            val initial = if (editingStart) rangeStart else rangeEnd
            var date by remember(picker) { mutableStateOf(initial.toLocalDate()) }
            var hour by remember(picker) { mutableStateOf(initial.hour.toString().padStart(2, '0')) }
            var minute by remember(picker) { mutableStateOf(initial.minute.toString().padStart(2, '0')) }
            val pending = date.atTime(hour.toInt(), minute.toInt())
            val validRange = if (editingStart) pending <= rangeEnd else pending >= rangeStart
            BillTransferDialog(
                title = if (editingStart) "开始日期" else "结束日期",
                onDismiss = { picker = null },
                onConfirm = {
                    if (editingStart) rangeStart = pending else rangeEnd = pending
                    picker = null
                    resetResult()
                },
                confirmEnabled = validRange,
            ) {
                LedgerDateTimePicker(date, hour, minute, { date = it }, { hour = it }, { minute = it })
                if (!validRange) Text(
                    "结束时间不能早于开始时间",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.error,
                )
            }
        }
        null -> Unit
    }
}

private val billDateFormat = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")

@Composable
private fun BillTransferCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun BillTransferOption(
    title: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = SettingsItemHeight)
                .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            if (selected) Icon(MaterialSymbols.Check, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
private fun BillTransferDivider() {
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun BillImportRow(
    label: String,
    value: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().heightIn(min = SettingsItemHeight)
            .clip(RoundedCornerShape(8.dp)).clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            value, Modifier.weight(1f), textAlign = TextAlign.End, maxLines = 1,
            overflow = TextOverflow.Ellipsis, color = colors.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Icon(MaterialSymbols.ChevronRight, null, Modifier.size(20.dp), tint = colors.onSurfaceVariant)
    }
}

@Composable
private fun ImportRuleChoice(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            if (selected) MaterialSymbols.RadioButtonChecked else MaterialSymbols.RadioButtonUnchecked,
            null, Modifier.size(20.dp),
            tint = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BillTransferDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    BasicAlertDialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(Modifier.widthIn(max = 560.dp).fillMaxWidth().imePadding().padding(24.dp)) {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxWidth().heightIn(max = maxHeight).padding(16.dp)) {
                    Text(title, Modifier.padding(bottom = 12.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), content = content)
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        LedgerIconButton(onClick = onDismiss) { Icon(MaterialSymbols.Close, "取消") }
                        Spacer(Modifier.weight(1f))
                        LedgerIconButton(onClick = onConfirm, enabled = confirmEnabled) {
                            Icon(MaterialSymbols.Check, "确认", tint = MaterialTheme.colorScheme.tertiary.copy(alpha = if (confirmEnabled) 1f else 0.38f))
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BillTransferScreenPreview() {
    LedgerTheme { BillTransferScreen(rules = emptyList(), onDismissRequest = {}) }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BillTransferScreenDarkPreview() {
    LedgerTheme { BillTransferScreen(rules = emptyList(), onDismissRequest = {}) }
}
