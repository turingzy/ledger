package cn.turingzy.ledger.ui.component

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.turingzy.ledger.util.parseImageUrl

private enum class BackgroundSource { URL, LOCAL, DEFAULT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CardBackgroundDialog(
    onDismiss: () -> Unit,
    onLocalImage: (Uri) -> Unit,
    onRestoreDefault: () -> Unit,
    onImageUrl: (String) -> Unit,
) {
    var url by rememberSaveable { mutableStateOf("") }
    var localUri by rememberSaveable { mutableStateOf<String?>(null) }
    var source by rememberSaveable { mutableStateOf(BackgroundSource.URL) }
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            localUri = uri.toString()
            url = ""
            source = BackgroundSource.LOCAL
        }
    }
    val validUrl = parseImageUrl(url) != null
    val canConfirm = when (source) {
        BackgroundSource.URL -> validUrl
        BackgroundSource.LOCAL -> localUri != null
        BackgroundSource.DEFAULT -> true
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = colors.background) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            ) {
                Text("更换卡片背景", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(20.dp))
                Box(
                    Modifier.fillMaxWidth().height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(colors.surfaceVariant)
                        .border(1.dp, colors.outline, RoundedCornerShape(22.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    BasicTextField(
                        value = url,
                        onValueChange = {
                            url = it
                            source = BackgroundSource.URL
                        },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, color = colors.onSurface),
                        cursorBrush = SolidColor(colors.onSurface),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "图片 URL" },
                        decorationBox = { innerTextField ->
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                if (url.isEmpty()) {
                                    Text(
                                        "https://…",
                                        modifier = Modifier.padding(start = 6.dp),
                                        fontSize = 15.sp,
                                        lineHeight = 22.sp,
                                        color = colors.onSurfaceVariant,
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                }
                if (source == BackgroundSource.URL && url.isNotBlank() && !validUrl) {
                    Text(
                        "请输入有效的 HTTPS 图片地址",
                        modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                        fontSize = 12.sp,
                        color = colors.error,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BackgroundSourceButton(
                        label = "本地图片",
                        selected = source == BackgroundSource.LOCAL,
                        onClick = {
                            focusManager.clearFocus()
                            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.weight(1f),
                    )
                    BackgroundSourceButton(
                        label = "恢复初始",
                        selected = source == BackgroundSource.DEFAULT,
                        onClick = {
                            focusManager.clearFocus()
                            url = ""
                            source = BackgroundSource.DEFAULT
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss, modifier = Modifier.offset(x = (-8).dp).size(40.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "关闭更换背景", modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            when (source) {
                                BackgroundSource.URL -> onImageUrl(url.trim())
                                BackgroundSource.LOCAL -> localUri?.let { onLocalImage(Uri.parse(it)) }
                                BackgroundSource.DEFAULT -> onRestoreDefault()
                            }
                        },
                        enabled = canConfirm,
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.tertiary),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.width(48.dp),
                    ) {
                        Text("确认", Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                    }
                }
            }
        }
    }
}

@Composable
private fun BackgroundSourceButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
        border = BorderStroke(1.dp, colors.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) colors.tertiary.copy(alpha = 0.10f) else colors.background,
            contentColor = if (selected) colors.tertiary else colors.onSurfaceVariant,
        ),
    ) {
        Text(label, maxLines = 1)
        Spacer(Modifier.weight(1f))
        Icon(
            imageVector = if (selected) Icons.Default.Check else Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = if (selected) "已选择" else null,
            modifier = Modifier.size(16.dp),
        )
    }
}
