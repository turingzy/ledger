package cn.turingzy.ledger.ui.feature.settings

import cn.turingzy.ledger.ui.components.base.BaseDivider
import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.ui.components.base.BaseSurface
import cn.turingzy.ledger.ui.components.base.BaseButtonVariant
import cn.turingzy.ledger.ui.components.base.BaseActionButton
import cn.turingzy.ledger.ui.components.base.BaseAlertDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.LedgerSegmentedControl
import cn.turingzy.ledger.ui.theme.LedgerTheme
import kotlinx.coroutines.Job
import android.net.Uri
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import kotlinx.coroutines.launch

private enum class TransferStatus { IDLE, RUNNING, COMPLETED, CANCELLED }

private data class TransferState(
    val progress: Int = 0,
    val status: TransferStatus = TransferStatus.IDLE,
)

@Composable
internal fun BackupAndRestoreScreen(
    onDismissRequest: () -> Unit,
    onBackup: suspend (Uri, Boolean, Boolean) -> Unit,
    onRestore: suspend (Uri) -> Unit,
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    var restoring by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var includeBills by remember { mutableStateOf(true) }
    var includeSettings by remember { mutableStateOf(true) }
    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var confirmRestore by remember { mutableStateOf(false) }
    var fileName by remember { mutableStateOf<String?>(null) }
    var state by remember { mutableStateOf(TransferState()) }
    var operationJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            fileUri = uri
            fileName = selectedTransferFileName(context, uri)
            state = TransferState()
        }
    }

    fun runTransfer(destination: Uri? = null) {
        state = TransferState(status = TransferStatus.RUNNING)
        resultMessage = null
        operationJob = scope.launch {
            try {
                if (restoring) onRestore(requireNotNull(fileUri))
                else onBackup(requireNotNull(destination), includeBills, includeSettings)
                state = TransferState(100, TransferStatus.COMPLETED)
                resultMessage = if (restoring) textResources.getString(cn.turingzy.ledger.R.string.ui_restore_completed) else textResources.getString(cn.turingzy.ledger.R.string.ui_backup_saved)
            } catch (cancelled: CancellationException) {
                state = state.copy(status = TransferStatus.CANCELLED)
                throw cancelled
            } catch (error: Exception) {
                Timber.e(error, "Backup or restore failed")
                state = TransferState()
                resultMessage = error.message ?: textResources.getString(cn.turingzy.ledger.R.string.ui_operation_failed_please_try_again)
            } finally { operationJob = null }
        }
    }
    val destinationPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runTransfer(uri)
    }
    fun performAction() {
        if (state.status == TransferStatus.RUNNING) {
            operationJob?.cancel()
            return
        }
        if (restoring) confirmRestore = true
        else destinationPicker.launch("Ledger-backup-${java.time.LocalDate.now()}.json")
    }
    if (confirmRestore) BaseAlertDialog(
        onDismissRequest = { confirmRestore = false },
        title = { Text(textResources.getString(cn.turingzy.ledger.R.string.ui_restore_backup_2)) },
        text = { Text(textResources.getString(cn.turingzy.ledger.R.string.ui_bill_backups_replace_local_accounts_transactions_and_categories)) },
        confirmButton = { BaseActionButton(variant = BaseButtonVariant.Text, onClick = { confirmRestore = false; runTransfer() }) { Text(textResources.getString(cn.turingzy.ledger.R.string.ui_restore)) } },
        dismissButton = { BaseActionButton(variant = BaseButtonVariant.Text, onClick = { confirmRestore = false }) { Text(textResources.getString(cn.turingzy.ledger.R.string.ui_cancel)) } },
    )

    SettingsSubpageScaffold(title = textResources.getString(cn.turingzy.ledger.R.string.ui_backup_and_restore), onBack = onDismissRequest) {
        if (resultMessage != null) Text(resultMessage.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        DataTransferContent(
            restoring = restoring,
            includeBills = includeBills,
            includeSettings = includeSettings,
            fileName = fileName,
            state = state,
            onModeChange = { restoring = it; state = TransferState() },
            onBillsChange = { includeBills = it; state = TransferState() },
            onSettingsChange = { includeSettings = it; state = TransferState() },
            onChooseFile = { filePicker.launch(arrayOf("*/*")) },
            onAction = ::performAction,
        )
    }
}

@Composable
private fun DataTransferContent(
    restoring: Boolean,
    includeBills: Boolean,
    includeSettings: Boolean,
    fileName: String?,
    state: TransferState,
    onModeChange: (Boolean) -> Unit,
    onBillsChange: (Boolean) -> Unit,
    onSettingsChange: (Boolean) -> Unit,
    onChooseFile: () -> Unit,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val colors = MaterialTheme.colorScheme
    val running = state.status == TransferStatus.RUNNING
    Column(
        modifier.fillMaxWidth().settingsPageScroll(),
        verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.large),
    ) {
        LedgerSegmentedControl(
            labels = listOf(textResources.getString(cn.turingzy.ledger.R.string.ui_backup), textResources.getString(cn.turingzy.ledger.R.string.ui_restore)),
            selectedIndex = if (restoring) 1 else 0,
            onSelect = { onModeChange(it == 1) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !running,
        )
        if (restoring) {
            SettingsTransferFileRow(fileName = fileName, enabled = !running, onClick = onChooseFile)
        } else {
            Text(textResources.getString(cn.turingzy.ledger.R.string.ui_backup_contents), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            BaseSurface(shape = LedgerStyle.shapes.card, color = colors.surface) {
                Column(Modifier.fillMaxWidth()) {
                    BackupContentOption(textResources.getString(cn.turingzy.ledger.R.string.ui_transactions_and_accounts), includeBills, !running, onBillsChange)
                    BaseDivider(Modifier.padding(horizontal = LedgerStyle.spacing.large))
                    BackupContentOption(textResources.getString(cn.turingzy.ledger.R.string.ui_app_settings), includeSettings, !running, onSettingsChange)
                }
            }
        }
        TransferActionContent(
            title = if (restoring) textResources.getString(cn.turingzy.ledger.R.string.ui_restore_backup) else textResources.getString(cn.turingzy.ledger.R.string.ui_create_backup),
            cancelTitle = if (restoring) textResources.getString(cn.turingzy.ledger.R.string.ui_cancel_restore) else textResources.getString(cn.turingzy.ledger.R.string.ui_cancel_backup),
            state = state,
            enabled = running || if (restoring) fileName != null else includeBills || includeSettings,
            onClick = onAction,
        )
    }
}

@Composable
private fun BackupContentOption(title: String, selected: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().heightIn(min = SettingsItemHeight)
            .toggleable(value = selected, enabled = enabled, role = Role.Checkbox, onValueChange = onChange)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (selected) MaterialSymbols.CheckBox else MaterialSymbols.CheckBoxOutlineBlank,
            null,
            Modifier.size(LedgerStyle.icons.default),
            tint = (if (selected) colors.tertiary else colors.onSurfaceVariant).copy(alpha = if (enabled) 1f else LedgerStyle.opacity.disabled),
        )
        Text(title, Modifier.padding(start = 12.dp).weight(1f), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun TransferActionContent(
    title: String,
    cancelTitle: String,
    state: TransferState,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val running = state.status == TransferStatus.RUNNING
    Column(verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.medium)) {
        SettingsTransferAction(
            title = if (running) cancelTitle else title,
            running = running,
            progress = state.progress.takeIf { state.status == TransferStatus.COMPLETED },
            enabled = enabled,
            onClick = onClick,
        )
        if (state.status == TransferStatus.CANCELLED) {
            Text("已${cancelTitle}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BackupPreview() {
    LedgerTheme {
        DataTransferContent(
            restoring = false,
            includeBills = true,
            includeSettings = false,
            fileName = null,
            state = TransferState(),
            onModeChange = {},
            onBillsChange = {},
            onSettingsChange = {},
            onChooseFile = {},
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RestorePreview() {
    LedgerTheme {
        DataTransferContent(
            restoring = true,
            includeBills = false,
            includeSettings = false,
            fileName = "Ledger-backup.json",
            state = TransferState(48, TransferStatus.RUNNING),
            onModeChange = {},
            onBillsChange = {},
            onSettingsChange = {},
            onChooseFile = {},
            onAction = {},
        )
    }
}
