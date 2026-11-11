package cn.turingzy.ledger.ui.component

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import cn.turingzy.ledger.ui.component.LedgerOutlinedButton as OutlinedButton
import androidx.compose.material3.Icon
import cn.turingzy.ledger.ui.component.LedgerIconButton as IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CardBackgroundDialog(
    defaultFilename: String,
    preparedFilename: String?,
    busy: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onLocalImage: (Uri) -> Unit,
    onConfirm: (restoreDefault: Boolean) -> Unit,
) {
    var restoreDefault by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            restoreDefault = false
            onLocalImage(uri)
        }
    }
    val canConfirm = !busy && (restoreDefault || preparedFilename != null)

    BasicAlertDialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Surface(shape = RoundedCornerShape(28.dp), color = colors.background) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp)) {
                Text("更换卡片背景", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(20.dp))
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    BackgroundSourceButton(
                        label = "本地图片",
                        filename = preparedFilename.takeUnless { restoreDefault },
                        enabled = !busy,
                        onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    BackgroundSourceButton(
                        label = "恢复初始",
                        filename = defaultFilename.takeIf { restoreDefault },
                        enabled = !busy,
                        onClick = { restoreDefault = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (busy) {
                    Text("正在处理图片…", Modifier.padding(top = 8.dp), fontSize = 12.sp, color = colors.onSurfaceVariant)
                }
                if (error != null) {
                    Text(error, Modifier.padding(top = 8.dp), fontSize = 12.sp, color = colors.error)
                }
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(showPressFeedback = false, onClick = onDismiss, enabled = !busy, modifier = Modifier.offset(x = (-8).dp).size(40.dp)) {
                        Icon(MaterialSymbols.Close, contentDescription = "关闭更换背景", modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(showPressFeedback = false, onClick = { onConfirm(restoreDefault) }, enabled = canConfirm, modifier = Modifier.size(40.dp)) {
                        Icon(
                            MaterialSymbols.Check,
                            contentDescription = "应用背景",
                            tint = if (canConfirm) colors.tertiary else colors.onSurface.copy(alpha = 0.38f),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BackgroundSourceButton(
    label: String,
    filename: String?,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.semantics { contentDescription = label },
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
        border = BorderStroke(1.dp, colors.onSurface.copy(alpha = 0.24f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.background,
            contentColor = colors.onSurfaceVariant,
        ),
    ) {
        Text(filename ?: label, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
