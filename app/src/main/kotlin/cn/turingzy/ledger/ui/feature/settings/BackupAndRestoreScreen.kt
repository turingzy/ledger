package cn.turingzy.ledger.ui.feature.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class TransferPreviewStatus { IDLE, RUNNING, COMPLETED, CANCELLED }

private data class TransferPreviewState(
    val progress: Int = 0,
    val status: TransferPreviewStatus = TransferPreviewStatus.IDLE,
)

@Composable
internal fun BackupAndRestoreScreen(onDismissRequest: () -> Unit) {
    var restoring by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var includeBills by remember { mutableStateOf(true) }
    var includeSettings by remember { mutableStateOf(true) }
    var fileName by remember { mutableStateOf<String?>(null) }
    var state by remember { mutableStateOf(TransferPreviewState()) }
    var operationJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            fileName = selectedTransferFileName(context, uri)
            state = TransferPreviewState()
        }
    }

    fun performAction() {
        if (state.status == TransferPreviewStatus.RUNNING) {
            operationJob?.cancel()
            operationJob = null
            state = state.copy(status = TransferPreviewStatus.CANCELLED)
            return
        }
        if (!restoring && !includeBills && !includeSettings) return
        if (restoring && fileName == null) return
        state = TransferPreviewState(status = TransferPreviewStatus.RUNNING)
        operationJob = scope.launch {
            for (percent in 1..100) {
                delay(45)
                state = state.copy(progress = percent)
            }
            state = state.copy(status = TransferPreviewStatus.COMPLETED)
            operationJob = null
        }
    }

    SettingsPage(title = "备份与恢复", onDismissRequest = onDismissRequest) {
        DataTransferContent(
            restoring = restoring,
            includeBills = includeBills,
            includeSettings = includeSettings,
            fileName = fileName,
            state = state,
            onModeChange = { restoring = it; state = TransferPreviewState() },
            onBillsChange = { includeBills = it; state = TransferPreviewState() },
            onSettingsChange = { includeSettings = it; state = TransferPreviewState() },
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
    state: TransferPreviewState,
    onModeChange: (Boolean) -> Unit,
    onBillsChange: (Boolean) -> Unit,
    onSettingsChange: (Boolean) -> Unit,
    onChooseFile: () -> Unit,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val running = state.status == TransferPreviewStatus.RUNNING
    Column(
        modifier.fillMaxWidth().settingsPageScroll(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LedgerSegmentedControl(
            labels = listOf("备份", "恢复"),
            selectedIndex = if (restoring) 1 else 0,
            onSelect = { onModeChange(it == 1) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !running,
        )
        if (restoring) {
            SettingsTransferFileRow(fileName = fileName, enabled = !running, onClick = onChooseFile)
        } else {
            Text("备份内容", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            Surface(shape = RoundedCornerShape(16.dp), color = colors.surface) {
                Column(Modifier.fillMaxWidth()) {
                    BackupContentOption("账单数据", includeBills, !running, onBillsChange)
                    HorizontalDivider(Modifier.padding(horizontal = 12.dp), thickness = 0.5.dp, color = colors.outlineVariant)
                    BackupContentOption("应用设置", includeSettings, !running, onSettingsChange)
                }
            }
        }
        TransferActionContent(
            title = if (restoring) "开始恢复" else "开始备份",
            cancelTitle = if (restoring) "取消恢复" else "取消备份",
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
            Modifier.size(24.dp),
            tint = (if (selected) colors.tertiary else colors.onSurfaceVariant).copy(alpha = if (enabled) 1f else 0.38f),
        )
        Text(title, Modifier.padding(start = 12.dp).weight(1f), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun TransferActionContent(
    title: String,
    cancelTitle: String,
    state: TransferPreviewState,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val running = state.status == TransferPreviewStatus.RUNNING
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingsTransferAction(
            title = if (running) cancelTitle else title,
            running = running,
            progress = state.progress.takeIf { running || state.status == TransferPreviewStatus.COMPLETED },
            enabled = enabled,
            onClick = onClick,
        )
        if (state.status == TransferPreviewStatus.CANCELLED) {
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
            state = TransferPreviewState(),
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
            state = TransferPreviewState(48, TransferPreviewStatus.RUNNING),
            onModeChange = {},
            onBillsChange = {},
            onSettingsChange = {},
            onChooseFile = {},
            onAction = {},
        )
    }
}
