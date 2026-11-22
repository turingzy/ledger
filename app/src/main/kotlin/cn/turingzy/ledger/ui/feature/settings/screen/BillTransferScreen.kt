package cn.turingzy.ledger.ui.feature.settings.screen

import cn.turingzy.ledger.ui.feature.settings.component.SettingsItemHeight
import cn.turingzy.ledger.ui.feature.settings.component.SettingsSubpageScaffold
import cn.turingzy.ledger.ui.feature.settings.component.SettingsTransferAction
import cn.turingzy.ledger.ui.feature.settings.component.SettingsTransferFileRow
import cn.turingzy.ledger.ui.feature.settings.component.selectedTransferFileName
import cn.turingzy.ledger.ui.feature.settings.component.settingsPageScroll
import cn.turingzy.ledger.ui.component.base.ButtonBase
import cn.turingzy.ledger.ui.component.base.ButtonVariantBase
import cn.turingzy.ledger.ui.component.input.PreferenceChoice
import cn.turingzy.ledger.ui.component.picker.LedgerDateTimeDialog
import cn.turingzy.ledger.ui.interaction.uiClickable
import cn.turingzy.ledger.ui.component.base.DividerBase
import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.ui.component.base.SurfaceBase
import cn.turingzy.ledger.ui.component.base.BaseAlertDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.ui.component.input.SegmentedControl
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.theme.LedgerTheme
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Job
import android.net.Uri
import cn.turingzy.ledger.model.account.AccountWithTemplate
import cn.turingzy.ledger.model.transfer.BillImportResult
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import java.time.ZoneId
import kotlinx.coroutines.launch

/**
 * 账单导出的文件格式枚举。
 *
 * @property label 格式的显示名称（如 "Excel", "CSV"）
 */
private enum class ExportFormat(val label: String) { EXCEL("Excel"), CSV("CSV") }

/**
 * 账单导出时间范围选择的目标枚举。
 */
private enum class BillTransferPicker {
    /** 编辑开始时间。 */
    START,
    /** 编辑结束时间。 */
    END,
}

/**
 * 账单导入导出操作的运行状态。
 */
private enum class BillTransferStatus {
    /** 空闲或就绪状态。 */
    IDLE,
    /** 正在执行导入或导出任务。 */
    RUNNING,
    /** 任务执行完毕。 */
    COMPLETED,
    /** 任务已被取消。 */
    CANCELLED,
}

/**
 * 账单导入与导出屏幕组件。
 *
 * 提供账单导入（选择文件、绑定解析规则与关联账户、重复账单检测与跳过）
 * 和账单导出（全部交易或指定时间区间、Excel 或 CSV 格式）的核心交互功能。
 *
 * @param rules 账单解析规则列表
 * @param onDismissRequest 退出或返回上一级页面的回调
 * @param accounts 现有账户列表，用于导入时指定目标归属账户
 * @param onPreviewImport 预导入校验挂起函数，返回可能重复记录及校验结果
 * @param onImport 执行账单文件正式导入的挂起函数
 * @param onExport 执行账单导出的挂起函数，返回成功导出的条数
 */
@Composable
internal fun BillTransferScreen(
    rules: List<ParseRulePreview>, onDismissRequest: () -> Unit,
    accounts: List<AccountWithTemplate> = emptyList(),
    onPreviewImport: suspend (Uri, ParseRulePreview, Long?) -> BillImportResult = { _, _, _ -> BillImportResult() },
    onImport: suspend (Uri, ParseRulePreview, Long?, Boolean) -> BillImportResult = { _, _, _, _ -> BillImportResult() },
    onExport: suspend (Uri, Boolean, Long, Long) -> Int = { _, _, _, _ -> 0 },
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
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
    var duplicateConfirmation by remember { mutableStateOf<BillImportResult?>(null) }
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

    fun runTransfer(destination: Uri? = null, preview: Boolean = true, includePossibleDuplicates: Boolean = false) {
        if (!importing && exportCoolingDown) return
        status = BillTransferStatus.RUNNING
        resultMessage = null
        operation = scope.launch {
            try {
                if (importing && preview) {
                    val check = onPreviewImport(requireNotNull(fileUri), requireNotNull(selectedRule), accountId)
                    if (check.possibleDuplicates > 0) {
                        duplicateConfirmation = check
                        status = BillTransferStatus.IDLE
                        return@launch
                    }
                }
                val imported = if (importing) onImport(requireNotNull(fileUri), requireNotNull(selectedRule), accountId, includePossibleDuplicates) else null
                val count = if (imported != null) imported.imported else onExport(requireNotNull(destination), format == ExportFormat.EXCEL,
                    if (allBills) Long.MIN_VALUE else rangeStart.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    if (allBills) Long.MAX_VALUE else rangeEnd.withSecond(59).withNano(999_000_000).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                status = BillTransferStatus.COMPLETED
                if (!importing) exportCoolingDown = true
                progress = 100
                resultMessage = textResources.getString(if (importing) cn.turingzy.ledger.R.string.imported_count else cn.turingzy.ledger.R.string.exported_count, count) +
                    imported?.let { result -> (result.duplicates + result.possibleDuplicates).takeIf { it > 0 }?.let { textResources.getString(cn.turingzy.ledger.R.string.skipped_count, it) } }.orEmpty()
            } catch (cancelled: CancellationException) {
                status = BillTransferStatus.CANCELLED
                throw cancelled
            } catch (error: Exception) {
                Timber.e(error, "Bill transfer failed")
                status = BillTransferStatus.IDLE
                resultMessage = error.message ?: textResources.getString(cn.turingzy.ledger.R.string.ui_operation_failed_please_try_again)
            } finally { operation = null }
        }
    }
    val csvDestination = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) runTransfer(uri)
    }
    val excelDestination = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
        if (uri != null) runTransfer(uri)
    }

    duplicateConfirmation?.let { preview ->
        BaseAlertDialog(
            onDismissRequest = { duplicateConfirmation = null },
            title = { Text(textResources.getString(cn.turingzy.ledger.R.string.ui_possible_duplicate_transactions)) },
            text = { Text(textResources.getString(cn.turingzy.ledger.R.string.duplicate_prompt, preview.possibleDuplicates)) },
            confirmLabel = textResources.getString(cn.turingzy.ledger.R.string.ui_skip_duplicates),
            onConfirm = { duplicateConfirmation = null; runTransfer(preview = false) },
            secondaryAction = {
                ButtonBase(
                    textResources.getString(cn.turingzy.ledger.R.string.ui_import_anyway),
                    { duplicateConfirmation = null; runTransfer(preview = false, includePossibleDuplicates = true) },
                    variant = ButtonVariantBase.Text,
                )
            },
        )
    }

    SettingsSubpageScaffold(title = textResources.getString(cn.turingzy.ledger.R.string.ui_import_and_export), onBack = onDismissRequest) {
        Column(
            Modifier.fillMaxWidth().settingsPageScroll(),
            verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.large),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.small)) {
                SegmentedControl(
                    labels = listOf(textResources.getString(cn.turingzy.ledger.R.string.ui_import), textResources.getString(cn.turingzy.ledger.R.string.ui_export)),
                    selectedIndex = if (importing) 0 else 1,
                    onSelect = { importing = it == 0; resetResult() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !running,
                )
                if (importing) Text(textResources.getString(cn.turingzy.ledger.R.string.ui_supports_excel_xlsx_and_csv), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            if (importing) {
                SettingsTransferFileRow(fileName = fileName, enabled = !running) {
                    filePicker.launch(arrayOf(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "text/csv", "text/comma-separated-values", "text/plain",
                    ))
                }
                BillTransferCard {
                    PreferenceChoice(
                        title = textResources.getString(cn.turingzy.ledger.R.string.ui_import_rule),
                        selected = selectedRule?.id?.toString() ?: textResources.getString(cn.turingzy.ledger.R.string.ui_select),
                        options = rules.map { it.id.toString() to it.name },
                        enabled = !running && rules.isNotEmpty(),
                        onSelect = { selectedRuleId = it.toLong(); resetResult() },
                    )
                    if (selectedRule?.accountId == null) {
                        BillTransferDivider()
                        PreferenceChoice(
                            title = textResources.getString(cn.turingzy.ledger.R.string.ui_import_account),
                            selected = accountId?.toString().orEmpty(),
                            options = listOf("" to textResources.getString(cn.turingzy.ledger.R.string.ui_use_accounts_from_the_file)) +
                                accounts.map { it.account.id.toString() to it.account.name },
                            enabled = !running,
                            onSelect = { accountId = it.toLongOrNull(); resetResult() },
                        )
                    }
                }
                if (rules.isEmpty()) Text(textResources.getString(cn.turingzy.ledger.R.string.ui_add_a_rule_in_import_rules),
                    style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            } else {
                Text(textResources.getString(cn.turingzy.ledger.R.string.ui_export_range), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.large)) {
                    BillTransferOption(textResources.getString(cn.turingzy.ledger.R.string.ui_all_transactions), allBills, !running, Modifier.weight(1f)) { allBills = true; resetResult() }
                    BillTransferOption(textResources.getString(cn.turingzy.ledger.R.string.ui_date_range), !allBills, !running, Modifier.weight(1f)) { allBills = false; resetResult() }
                }
                if (!allBills) {
                    BillTransferCard {
                        BillImportRow(textResources.getString(cn.turingzy.ledger.R.string.ui_start_date), rangeStart.format(billDateFormat), enabled = !running) { picker = BillTransferPicker.START }
                        BillTransferDivider()
                        BillImportRow(textResources.getString(cn.turingzy.ledger.R.string.ui_end_date), rangeEnd.format(billDateFormat), enabled = !running) { picker = BillTransferPicker.END }
                    }
                }
                Text(textResources.getString(cn.turingzy.ledger.R.string.ui_export_format), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.large)) {
                    ExportFormat.entries.forEach { option ->
                        BillTransferOption(option.label, format == option, !running, Modifier.weight(1f)) {
                            format = option
                            resetResult()
                        }
                    }
                }
            }
            SettingsTransferAction(
                title = if (running) { if (importing) textResources.getString(cn.turingzy.ledger.R.string.ui_cancel_import) else textResources.getString(cn.turingzy.ledger.R.string.ui_cancel_export) }
                    else { if (importing) textResources.getString(cn.turingzy.ledger.R.string.ui_import_transactions) else textResources.getString(cn.turingzy.ledger.R.string.ui_export_transactions) },
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
                BillTransferStatus.CANCELLED -> if (importing) textResources.getString(cn.turingzy.ledger.R.string.ui_import_cancelled) else textResources.getString(cn.turingzy.ledger.R.string.ui_export_cancelled)
            }
            if (resultMessage != null) Text(resultMessage.orEmpty(), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            if (message != null) Text(message, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }

    picker?.let { target ->
        val editingStart = target == BillTransferPicker.START
        val initial = if (editingStart) rangeStart else rangeEnd
        val zone = ZoneId.systemDefault()
        LedgerDateTimeDialog(
            initialTime = initial.atZone(zone).toInstant().toEpochMilli(),
            onDismiss = { picker = null },
            onConfirm = { time ->
                val pending = Instant.ofEpochMilli(time).atZone(zone).toLocalDateTime()
                if (editingStart) rangeStart = pending else rangeEnd = pending
                picker = null
                resetResult()
            },
            isTimeAllowed = { time ->
                val pending = Instant.ofEpochMilli(time).atZone(zone).toLocalDateTime()
                if (editingStart) pending <= rangeEnd else pending >= rangeStart
            },
            invalidTimeMessage = textResources.getString(cn.turingzy.ledger.R.string.ui_end_time_must_not_precede_start_time),
        )
    }
}

/** 账单时间范围展示的日期格式化器。 */
private val billDateFormat = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")

/**
 * 账单传输界面的分组卡片容器。
 *
 * @param content 卡片内承载的列表内容
 */
@Composable
private fun BillTransferCard(content: @Composable ColumnScope.() -> Unit) {
    SurfaceBase(shape = LedgerStyle.shapes.card, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth(), content = content)
    }
}

/**
 * 账单传输界面中的单选卡片项组件。
 *
 * @param title 选项标题文本
 * @param selected 是否被选中
 * @param enabled 是否允许交互
 * @param modifier 修饰符
 * @param onClick 点击事件回调
 */
@Composable
private fun BillTransferOption(
    title: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    SurfaceBase(modifier, shape = LedgerStyle.shapes.card, color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = SettingsItemHeight)
                .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            if (selected) Icon(MaterialSymbols.Check, null, Modifier.size(LedgerStyle.icons.inline), tint = MaterialTheme.colorScheme.tertiary)
        }
    }
}

/**
 * 账单传输卡片内部的分隔线组件。
 */
@Composable
private fun BillTransferDivider() {
    DividerBase()
}

/**
 * 账单导入导出中的点击导航行（如日期时间选择行）。
 *
 * @param label 左侧说明文本
 * @param value 右侧当前选值展示
 * @param enabled 是否允许点击
 * @param onClick 点击事件回调
 */
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
            .clip(LedgerStyle.shapes.control).uiClickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.medium),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            value, Modifier.weight(1f), textAlign = TextAlign.End, maxLines = 1,
            overflow = TextOverflow.Ellipsis, color = colors.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Icon(MaterialSymbols.ChevronRight, null, Modifier.size(LedgerStyle.icons.inline), tint = colors.onSurfaceVariant)
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
