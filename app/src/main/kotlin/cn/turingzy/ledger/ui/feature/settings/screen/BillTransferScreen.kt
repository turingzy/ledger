package cn.turingzy.ledger.ui.feature.settings.screen

import cn.turingzy.ledger.ui.feature.settings.component.SettingsItemHeight
import cn.turingzy.ledger.ui.feature.settings.component.SettingsSubpageScaffold
import cn.turingzy.ledger.ui.feature.settings.component.SettingsTransferAction
import cn.turingzy.ledger.ui.feature.settings.component.SettingsTransferFileRow
import cn.turingzy.ledger.ui.feature.settings.component.selectedTransferFileName
import cn.turingzy.ledger.ui.feature.settings.component.settingsPageScroll
import cn.turingzy.ledger.ui.component.base.BaseIconButtonContent
import cn.turingzy.ledger.ui.interaction.uiClickable
import cn.turingzy.ledger.ui.component.base.BaseDivider
import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.ui.component.base.BaseSurface
import cn.turingzy.ledger.ui.component.base.BaseButtonVariant
import cn.turingzy.ledger.ui.component.base.BaseActionButton
import cn.turingzy.ledger.ui.component.base.BaseDialogWindow
import cn.turingzy.ledger.ui.component.base.BaseAlertDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.ui.component.input.LedgerSegmentedControl
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.picker.LedgerDateTimePicker
import cn.turingzy.ledger.ui.theme.LedgerTheme
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

private enum class ExportFormat(val label: String) { EXCEL("Excel"), CSV("CSV") }
private enum class BillTransferPicker { RULE, ACCOUNT, START, END }
private enum class BillTransferStatus { IDLE, RUNNING, COMPLETED, CANCELLED }

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
            confirmButton = {
                BaseActionButton(variant = BaseButtonVariant.Text, onClick = {
                    duplicateConfirmation = null
                    runTransfer(preview = false)
                }) { Text(textResources.getString(cn.turingzy.ledger.R.string.ui_skip_duplicates)) }
            },
            dismissButton = {
                BaseActionButton(variant = BaseButtonVariant.Text, onClick = {
                    duplicateConfirmation = null
                    runTransfer(preview = false, includePossibleDuplicates = true)
                }) { Text(textResources.getString(cn.turingzy.ledger.R.string.ui_import_anyway)) }
            },
        )
    }

    SettingsSubpageScaffold(title = textResources.getString(cn.turingzy.ledger.R.string.ui_import_and_export), onBack = onDismissRequest) {
        Column(
            Modifier.fillMaxWidth().settingsPageScroll(),
            verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.large),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.small)) {
                LedgerSegmentedControl(
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
                    BillImportRow(textResources.getString(cn.turingzy.ledger.R.string.ui_import_rule), selectedRule?.name ?: textResources.getString(cn.turingzy.ledger.R.string.ui_select), enabled = !running) {
                        picker = BillTransferPicker.RULE
                    }
                    if (selectedRule?.accountId == null) {
                        BillTransferDivider()
                        BillImportRow(textResources.getString(cn.turingzy.ledger.R.string.ui_import_account), accounts.firstOrNull { it.account.id == accountId }?.account?.name ?: textResources.getString(cn.turingzy.ledger.R.string.ui_use_accounts_from_the_file), enabled = !running) {
                            picker = BillTransferPicker.ACCOUNT
                        }
                    }
                }
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

    when (picker) {
        BillTransferPicker.ACCOUNT -> BillTransferDialog(
            title = textResources.getString(cn.turingzy.ledger.R.string.ui_import_account), onDismiss = { picker = null }, onConfirm = { picker = null },
        ) {
            ImportRuleChoice(textResources.getString(cn.turingzy.ledger.R.string.ui_use_accounts_from_the_file), accountId == null) { accountId = null }
            accounts.forEach { item ->
                ImportRuleChoice(item.account.name, accountId == item.account.id) { accountId = item.account.id }
            }
        }
        BillTransferPicker.RULE -> {
            var pendingRuleId by remember { mutableStateOf(selectedRule?.id) }
            BillTransferDialog(
                title = textResources.getString(cn.turingzy.ledger.R.string.ui_import_rule),
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
                        textResources.getString(cn.turingzy.ledger.R.string.ui_add_a_rule_in_import_rules),
                        Modifier.padding(vertical = LedgerStyle.spacing.medium),
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
                title = if (editingStart) textResources.getString(cn.turingzy.ledger.R.string.ui_start_date) else textResources.getString(cn.turingzy.ledger.R.string.ui_end_date),
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
                    textResources.getString(cn.turingzy.ledger.R.string.ui_end_time_must_not_precede_start_time),
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
    BaseSurface(shape = LedgerStyle.shapes.card, color = MaterialTheme.colorScheme.surface) {
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
    BaseSurface(modifier, shape = LedgerStyle.shapes.card, color = MaterialTheme.colorScheme.surface) {
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

@Composable
private fun BillTransferDivider() {
    BaseDivider()
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

@Composable
private fun ImportRuleChoice(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(LedgerStyle.shapes.control)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick).padding(vertical = LedgerStyle.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.large),
    ) {
        Icon(
            if (selected) MaterialSymbols.RadioButtonChecked else MaterialSymbols.RadioButtonUnchecked,
            null, Modifier.size(LedgerStyle.icons.inline),
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
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    BaseDialogWindow(onDismissRequest = onDismiss) {
        BoxWithConstraints(Modifier.widthIn(max = LedgerStyle.dimensions.wideDialogMaxWidth).fillMaxWidth()) {
            BaseSurface(shape = LedgerStyle.shapes.dialog, color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxWidth().heightIn(max = maxHeight).padding(LedgerStyle.spacing.extraLarge)) {
                    Text(title, Modifier.padding(bottom = 12.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), content = content)
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        BaseIconButtonContent(onClick = onDismiss) { Icon(MaterialSymbols.Close, textResources.getString(cn.turingzy.ledger.R.string.ui_cancel)) }
                        Spacer(Modifier.weight(1f))
                        BaseIconButtonContent(onClick = onConfirm, enabled = confirmEnabled) {
                            Icon(MaterialSymbols.Check, textResources.getString(cn.turingzy.ledger.R.string.ui_confirm), tint = MaterialTheme.colorScheme.tertiary.copy(alpha = if (confirmEnabled) 1f else LedgerStyle.opacity.disabled))
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
