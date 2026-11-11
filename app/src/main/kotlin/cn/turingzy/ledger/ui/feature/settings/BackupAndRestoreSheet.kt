package cn.turingzy.ledger.ui.feature.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ButtonDefaults
import cn.turingzy.ledger.ui.component.LedgerOutlinedButton as OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class TransferPreviewStatus { IDLE, RUNNING, COMPLETED, CANCELLED }
private enum class TransferOperation { BACKUP, RESTORE }

private data class TransferPreviewState(
    val progress: Int = 0,
    val status: TransferPreviewStatus = TransferPreviewStatus.IDLE,
)

@Composable
internal fun BackupAndRestoreSheet(onDismissRequest: () -> Unit) {
    var activeOperation by remember { mutableStateOf<TransferOperation?>(null) }

    fun tryStart(operation: TransferOperation): Boolean {
        if (activeOperation != null) return false
        activeOperation = operation
        return true
    }

    SettingsBottomSheet(title = "备份与恢复", onDismissRequest = onDismissRequest) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            TransferPreviewSection(
                title = "备份数据",
                completionMessage = "备份演示完成，未生成实际文件",
                enabled = activeOperation == null || activeOperation == TransferOperation.BACKUP,
                onStartRequested = { tryStart(TransferOperation.BACKUP) },
                onFinished = { activeOperation = null },
            )
            TransferPreviewSection(
                title = "恢复数据",
                completionMessage = "恢复演示完成，未更改账目数据",
                enabled = activeOperation == null || activeOperation == TransferOperation.RESTORE,
                onStartRequested = { tryStart(TransferOperation.RESTORE) },
                onFinished = { activeOperation = null },
            )
        }
    }
}

@Composable
private fun TransferPreviewSection(
    title: String,
    completionMessage: String,
    enabled: Boolean,
    onStartRequested: () -> Boolean,
    onFinished: () -> Unit,
) {
    var state by remember { mutableStateOf(TransferPreviewState()) }
    var operation by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    TransferPreviewContent(
        title = title,
        completionMessage = completionMessage,
        state = state,
        enabled = enabled,
        onStart = start@{
            if (!onStartRequested()) return@start
            operation?.cancel()
            state = TransferPreviewState(status = TransferPreviewStatus.RUNNING)
            operation = scope.launch {
                for (percent in 1..100) {
                    delay(45)
                    state = TransferPreviewState(progress = percent, status = TransferPreviewStatus.RUNNING)
                }
                state = state.copy(status = TransferPreviewStatus.COMPLETED)
                operation = null
                onFinished()
            }
        },
        onCancel = {
            operation?.cancel()
            operation = null
            state = state.copy(status = TransferPreviewStatus.CANCELLED)
            onFinished()
        },
    )
}

@Composable
private fun TransferPreviewContent(
    title: String,
    completionMessage: String,
    state: TransferPreviewState,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val running = state.status == TransferPreviewStatus.RUNNING
    Column(
        Modifier.fillMaxWidth().animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = if (running) onCancel else onStart,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (running) colors.error else colors.outlineVariant),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (running) colors.error.copy(alpha = 0.08f) else colors.surfaceVariant.copy(alpha = 0.45f),
                contentColor = if (running) colors.error else colors.onSurface,
            ),
        ) {
            Text(if (running) "取消${title.removeSuffix("数据")}" else title, style = MaterialTheme.typography.bodyLarge)
        }
        if (running || state.status == TransferPreviewStatus.COMPLETED) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LinearProgressIndicator(
                    progress = { state.progress / 100f },
                    modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape),
                    color = colors.tertiary,
                    trackColor = colors.surfaceVariant,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
                Text(
                    "${state.progress}%",
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        val message = when (state.status) {
            TransferPreviewStatus.IDLE -> ""
            TransferPreviewStatus.RUNNING -> "${title}中…"
            TransferPreviewStatus.COMPLETED -> completionMessage
            TransferPreviewStatus.CANCELLED -> "已取消${title.removeSuffix("数据")}"
        }
        if (message.isNotEmpty()) {
            Text(message, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TransferPreviewContentPreview() {
    MaterialTheme {
        TransferPreviewContent(
            title = "备份数据",
            completionMessage = "备份演示完成，未生成实际文件",
            state = TransferPreviewState(48, TransferPreviewStatus.RUNNING),
            onStart = {},
            onCancel = {},
        )
    }
}
