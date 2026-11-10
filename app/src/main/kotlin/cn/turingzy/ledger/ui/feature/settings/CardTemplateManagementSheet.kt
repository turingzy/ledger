package cn.turingzy.ledger.ui.feature.settings

import android.net.Uri
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import cn.turingzy.ledger.ui.component.BankCardAspectRatio
import cn.turingzy.ledger.ui.component.CardPageIndicator
import cn.turingzy.ledger.R
import cn.turingzy.ledger.model.CardTemplate
import kotlinx.coroutines.launch

@Composable
internal fun CardTemplateManagementSheet(
    templates: List<CardTemplate>,
    onDismissRequest: () -> Unit,
) {
    val drafts = remember { mutableStateListOf<CardTemplate>().apply { addAll(templates) } }
    val pagerState = rememberPagerState(pageCount = { drafts.size.coerceAtLeast(1) })
    val scope = rememberCoroutineScope()
    val selectedIndex = pagerState.currentPage.coerceAtMost(drafts.lastIndex)
    var newTemplate by remember { mutableStateOf<CardTemplate?>(null) }
    val selected = newTemplate ?: drafts.getOrNull(selectedIndex)
    val focusManager = LocalFocusManager.current
    var uploadTarget by remember { mutableStateOf<Long?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val index = drafts.indexOfFirst { it.id == uploadTarget }
        if (uri != null) {
            if (newTemplate?.id == uploadTarget) {
                newTemplate = newTemplate?.copy(coverUri = uri.toString())
            } else if (index >= 0) {
                drafts[index] = drafts[index].copy(coverUri = uri.toString())
            }
        }
        uploadTarget = null
    }

    SettingsBottomSheet(
        title = "卡片管理",
        onDismissRequest = onDismissRequest,
        onBack = if (newTemplate != null) {
            {
                focusManager.clearFocus()
                newTemplate = null
            }
        } else null,
        action = {
            val creating = newTemplate != null
            val canSave = newTemplate?.let { it.name.isNotBlank() && it.coverUri.isNotBlank() } == true
            IconButton(
                enabled = !creating || canSave,
                onClick = {
                    val pending = newTemplate
                    if (pending == null) {
                        val id = minOf(drafts.minOfOrNull { it.id } ?: 0L, 0L) - 1L
                        newTemplate = CardTemplate(id = id, name = "", coverUri = "")
                    } else {
                        if (!canSave) return@IconButton
                        focusManager.clearFocus()
                        drafts.add(pending)
                        newTemplate = null
                        scope.launch { pagerState.scrollToPage(drafts.lastIndex) }
                    }
                },
            ) {
                Icon(
                    if (creating) Icons.Default.Check else Icons.Default.Add,
                    if (creating) "保存卡片模板预览" else "新增卡片模板",
                    tint = if (creating) {
                        MaterialTheme.colorScheme.tertiary.copy(alpha = if (canSave) 1f else 0.38f)
                    } else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    ) {
        Column(
            Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (newTemplate != null) {
                TemplateCoverPreview(
                    template = newTemplate,
                    creating = true,
                    onUpload = {
                        uploadTarget = newTemplate?.id
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                )
            } else {
                HorizontalPager(
                    state = pagerState,
                    key = { drafts.getOrNull(it)?.id ?: "empty" },
                    pageSpacing = 12.dp,
                ) { page ->
                    TemplateCoverPreview(
                        template = drafts.getOrNull(page),
                        onUpload = {
                            uploadTarget = drafts.getOrNull(page)?.id
                            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    )
                }
                CardPageIndicator(pageCount = drafts.size, currentPage = selectedIndex)
            }
            TemplateDetailsPreview(
                template = selected,
                creating = newTemplate != null,
                onNameChange = { name ->
                    if (newTemplate != null) newTemplate = newTemplate?.copy(name = name)
                    else selected?.let { drafts[selectedIndex] = it.copy(name = name) }
                },
                onColorChange = { color ->
                    if (newTemplate != null) newTemplate = newTemplate?.copy(textColor = color)
                    else selected?.let { drafts[selectedIndex] = it.copy(textColor = color) }
                },
                onDelete = { if (selected != null) drafts.removeAt(selectedIndex) },
            )
        }
    }
}

@Composable
private fun TemplateCoverPreview(
    template: CardTemplate?,
    creating: Boolean = false,
    onUpload: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.fillMaxWidth().aspectRatio(BankCardAspectRatio).clip(RoundedCornerShape(24.dp))
            .background(colors.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (creating && template?.coverUri.isNullOrBlank()) {
            Box(
                Modifier.fillMaxSize().background(Color.White).drawWithCache {
                    val stroke = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())))
                    onDrawBehind {
                        drawRoundRect(Color.Black, cornerRadius = CornerRadius(24.dp.toPx()), style = stroke)
                    }
                },
                contentAlignment = Alignment.Center,
            ) {
                IconButton(onClick = onUpload, modifier = Modifier.size(64.dp)) {
                    Icon(painterResource(R.drawable.ic_image_arrow_up), "上传卡片封面", Modifier.size(32.dp), tint = Color.Black)
                }
            }
        } else if (template != null) {
            if (template.coverUri.startsWith("content://")) {
                AndroidView(
                    factory = { context -> ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP } },
                    update = { it.setImageURI(Uri.parse(template.coverUri)) },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Image(
                    painterResource(R.drawable.bg_wallet_card),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            IconButton(onClick = onUpload, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                Icon(
                    painterResource(R.drawable.ic_image_arrow_up),
                    "上传卡片封面",
                    tint = Color.White,
                )
            }
        } else {
            Text("暂无卡片，点击右上角添加", color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun TemplateDetailsPreview(
    template: CardTemplate?,
    creating: Boolean,
    onNameChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var showColors by remember(template?.id) { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(16.dp), color = colors.surfaceVariant.copy(alpha = 0.45f)) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("卡片名称", modifier = Modifier.width(96.dp))
                BasicTextField(
                    value = template?.name.orEmpty(),
                    onValueChange = onNameChange,
                    enabled = template != null,
                    singleLine = true,
                    decorationBox = { field ->
                        Box(contentAlignment = Alignment.CenterEnd) {
                            if (template?.name.isNullOrEmpty()) Text("请输入卡片名称", color = colors.onSurfaceVariant)
                            field()
                        }
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface, textAlign = TextAlign.End),
                    cursorBrush = SolidColor(colors.onSurface),
                    modifier = Modifier.weight(1f).semantics { contentDescription = "卡片名称" },
                )
            }
            HorizontalDivider(thickness = 0.5.dp, color = colors.outlineVariant)
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("文字颜色")
                Row(
                    Modifier.clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = template != null) { showColors = true }
                        .padding(vertical = 12.dp, horizontal = 4.dp)
                        .semantics { contentDescription = "选择文字颜色" },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(18.dp).clip(CircleShape)
                            .background(template?.textColor?.asTemplateColor(colors.onSurface) ?: colors.outlineVariant)
                            .border(0.5.dp, colors.outlineVariant, CircleShape),
                    )
                    Text(template?.textColor.orEmpty(), color = colors.onSurfaceVariant)
                }
            }
            if (!creating) {
                HorizontalDivider(thickness = 0.5.dp, color = colors.outlineVariant)
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("是否删除")
                    IconButton(onClick = onDelete, enabled = template != null) {
                        Icon(Icons.Default.DeleteOutline, "删除卡片模板", tint = colors.error)
                    }
                }
            }
        }
    }
    if (showColors && template != null) {
        TemplateColorPickerSheet(
            initialColor = template.textColor,
            onDismissRequest = { showColors = false },
            onConfirm = {
                onColorChange(it)
                showColors = false
            },
        )
    }

}

private fun String.asTemplateColor(fallback: Color): Color {
    val hex = removePrefix("#")
    if (hex.length != 6 && hex.length != 8) return fallback
    val value = hex.toLongOrNull(16) ?: return fallback
    return Color((if (hex.length == 6) value or 0xFF000000L else value).toInt())
}
