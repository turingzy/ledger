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
