package cn.turingzy.ledger.ui.feature.settings.component

import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.ui.component.base.SurfaceBase
import cn.turingzy.ledger.ui.component.base.ButtonVariantBase
import cn.turingzy.ledger.ui.component.base.ActionButtonBase
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import timber.log.Timber

/**
 * 账单传输（导入或导出）的操作控制行组件。
 *
 * 包含状态进度指示器（支持不确定循环进度与确定百分比进度展示）和操作触发按钮。
 *
 * @param title 操作按钮文本（例如“开始导入”、“导出账单”）
 * @param running 是否当前正在执行传输任务
 * @param progress 当前传输进度百分比（0..100），为 null 时若 [running] 为 true 则展示循环进度条
 * @param onClick 按钮点击回调
 * @param modifier 修饰符
 * @param enabled 按钮是否允许交互
 */
@Composable
internal fun SettingsTransferAction(
    title: String,
    running: Boolean,
    progress: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.medium),
    ) {
        if (running && progress == null) {
            LinearProgressIndicator(modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape), color = colors.tertiary)
        }
        if (progress != null) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape),
                color = colors.tertiary,
                trackColor = colors.surfaceVariant,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            Text("$progress%", textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        if (!running && progress == null) Spacer(Modifier.weight(1f))
        ActionButtonBase(variant = ButtonVariantBase.Text, actionKey = running,
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.heightIn(min = SettingsItemHeight),
            contentPadding = PaddingValues(vertical = 8.dp),
            showPressFeedback = false,
            colors = ButtonDefaults.textButtonColors(contentColor = if (running) colors.error else colors.primary),
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Start)
        }
    }
}

/**
 * 账单传输文件选择项展示行。
 *
 * 显示“交易文件”标签及当前选中的文件名，点击可调用外部文件选择器。
 *
 * @param fileName 当前已选中的文件名，为 null 时显示“选择文件”提示
 * @param enabled 是否允许点击选择
 * @param modifier 修饰符
 * @param onClick 点击事件回调
 */
@Composable
internal fun SettingsTransferFileRow(
    fileName: String?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val colors = MaterialTheme.colorScheme
    SurfaceBase(modifier, shape = LedgerStyle.shapes.card, color = colors.surface) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = SettingsItemHeight)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.large),
        ) {
            Text(textResources.getString(cn.turingzy.ledger.R.string.ui_transaction_file), style = MaterialTheme.typography.bodyLarge)
            Text(
                fileName ?: textResources.getString(cn.turingzy.ledger.R.string.ui_choose_file), Modifier.weight(1f), textAlign = TextAlign.End,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant,
            )
        }
    }
}

/**
 * 根据给定的内容 URI 查询并解析出文件在系统中的显示名称。
 *
 * @param context Android 上下文环境
 * @param uri 选定文件的 [Uri]
 * @return 解析到的文件名称，若无法读取则返回默认兜底名称
 */
internal fun selectedTransferFileName(context: Context, uri: Uri): String = try {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
    } ?: "已选择文件"
} catch (error: CancellationException) {
    throw error
} catch (error: Exception) {
    Timber.w(error, "Unable to read selected transfer file name")
    "已选择文件"
}
