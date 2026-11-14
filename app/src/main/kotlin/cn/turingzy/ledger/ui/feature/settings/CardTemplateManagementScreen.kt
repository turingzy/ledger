package cn.turingzy.ledger.ui.feature.settings

import cn.turingzy.ledger.ui.component.AccountCardBackground
import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.model.AccountType
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.navigation.SettingsSubpageTransition
import cn.turingzy.ledger.ui.theme.SurfaceLight
import cn.turingzy.ledger.ui.theme.IosLabel
import cn.turingzy.ledger.model.CardTemplate
import cn.turingzy.ledger.ui.component.BankCardAspectRatio
import cn.turingzy.ledger.ui.component.CardPageIndicator
import cn.turingzy.ledger.ui.component.SummaryCardIconButton
import cn.turingzy.ledger.ui.component.LedgerIconButton
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.rememberCircularCardPagerState

@Composable
internal fun CardTemplateManagementScreen(
    templates: List<CardTemplate>,
    onDismissRequest: () -> Unit,
    onCreate: suspend (CardTemplate) -> Long?,
    onUpdate: (CardTemplate) -> Unit,
    onDelete: (Long) -> Unit,
    onCopyImage: suspend (Uri) -> String?,
) {
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    val drafts = remember { mutableStateListOf<CardTemplate>().apply { addAll(templates) } }
    var selectedId by remember { mutableStateOf(drafts.firstOrNull()?.id) }
    val pagerState = rememberCircularCardPagerState(drafts.map { it.id }, selectedId)
    val selectedIndex = if (drafts.isEmpty()) -1 else Math.floorMod(pagerState.currentPage, drafts.size)
    val selected = drafts.getOrNull(selectedIndex)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            if (drafts.isNotEmpty()) selectedId = drafts[Math.floorMod(page, drafts.size)].id
        }
    }
    var newFace by remember { mutableStateOf(CardTemplate(id = -1L, name = "", coverUri = "")) }
    var creating by remember { mutableStateOf(false) }
    var uploadTarget by remember { mutableStateOf<Long?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val target = uploadTarget
        if (uri != null) scope.launch {
            val image = onCopyImage(uri) ?: return@launch
            val index = drafts.indexOfFirst { it.id == target }
            if ((creating || drafts.isEmpty()) && newFace.id == target) newFace = newFace.copy(coverUri = image)
            else if (index >= 0) {
                val updated = drafts[index].copy(coverUri = image)
                drafts[index] = updated
                onUpdate(updated)
            }
        }
        uploadTarget = null
    }

    fun replaceImage(template: CardTemplate) {
        uploadTarget = template.id
        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    SettingsSubpageTransition(
        targetState = creating || drafts.isEmpty(),
        depth = { if (it) 1 else 0 },
    ) { adding ->
        if (adding) {
            val pending = newFace
            val canSave = !saving && pending.name.isNotBlank() && pending.coverUri.isNotBlank()
            SettingsPage(
                title = "新增卡面",
                onDismissRequest = { if (drafts.isEmpty()) onDismissRequest() else creating = false },
                avoidKeyboard = true,
                action = {
                    LedgerIconButton(enabled = canSave, onClick = {
                        saving = true
                        scope.launch {
                            try {
                                val id = onCreate(pending.copy(name = pending.name.trim())) ?: return@launch
                                drafts.add(pending.copy(id = id, name = pending.name.trim()))
                                selectedId = id
                                creating = false
                            } finally { saving = false }
                        }
                    }) {
                        Icon(MaterialSymbols.Check, "确认新增卡面", tint = MaterialTheme.colorScheme.tertiary.copy(alpha = if (canSave) 1f else 0.38f))
                    }
                },
            ) {
                Column(
                    Modifier.fillMaxWidth().settingsPageScroll(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TemplateCoverPreview(pending, onChooseImage = { replaceImage(pending) })
                    CardFaceProperties(
                        template = pending,
                        onChange = { newFace = it },
                    )
                }
            }
        } else {
            SettingsPage(
                title = "卡面管理",
                onDismissRequest = onDismissRequest,
                avoidKeyboard = true,
                action = {
                    LedgerIconButton(onClick = {
                        newFace = CardTemplate(id = minOf(drafts.minOfOrNull { it.id } ?: 0L, 0L) - 1L, name = "", coverUri = "")
                        creating = true
                    }) { Icon(MaterialSymbols.Add, "新增卡面") }
                },
            ) {
                Column(
                    Modifier.fillMaxWidth().settingsPageScroll(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (selected != null) {
                        HorizontalPager(state = pagerState, key = { it }, pageSpacing = 12.dp) { pageIndex ->
                            drafts.getOrNull(if (drafts.isEmpty()) -1 else Math.floorMod(pageIndex, drafts.size))?.let {
                                val face = it
                                TemplateCoverPreview(
                                    template = face,
                                    onChooseImage = { replaceImage(face) },
                                    onDelete = {
                                        val index = drafts.indexOfFirst { draft -> draft.id == face.id }
                                        if (index >= 0) {
                                            onDelete(face.id)
                                            drafts.removeAt(index)
                                            if (drafts.isEmpty()) newFace = CardTemplate(id = -1L, name = "", coverUri = "")
                                            selectedId = drafts.getOrNull(index.coerceAtMost(drafts.lastIndex))?.id
                                        }
                                    },
                                )
                            }
                        }
                        CardPageIndicator(pageCount = drafts.size, currentPage = selectedIndex)
                        CardFaceProperties(
                            template = selected,
                            onChange = { updated ->
                                val index = drafts.indexOfFirst { it.id == updated.id }
                                if (index >= 0) {
                                    drafts[index] = updated
                                    if (updated.name.isNotBlank()) onUpdate(updated)
                                }
                            },

                        )
                    }

                }
            }
        }
    }
}

@Composable
private fun CardFaceProperties(
    template: CardTemplate,
    onChange: (CardTemplate) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = RoundedCornerShape(16.dp), color = colors.surface) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = SettingsItemHeight).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("卡面名称", Modifier.width(96.dp), style = MaterialTheme.typography.bodyLarge)
                BasicTextField(
                    value = template.name,
                    onValueChange = { onChange(template.copy(name = it)) },
                    modifier = Modifier.weight(1f).semantics { contentDescription = "卡面名称" },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurfaceVariant, textAlign = TextAlign.End),
                    cursorBrush = SolidColor(colors.onSurface),
                    decorationBox = { field ->
                        Box(contentAlignment = Alignment.CenterEnd) {
                            if (template.name.isEmpty()) Text("请输入卡面名称", color = colors.onSurfaceVariant)
                            field()
                        }
                    },
                )
            }
        }
        Surface(shape = RoundedCornerShape(16.dp), color = colors.surface) {
            CardTextColorPicker(template.textColor, onChange = { onChange(template.copy(textColor = it)) })
        }
    }
}

@Composable
private fun TemplateCoverPreview(
    template: CardTemplate,
    onChooseImage: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val placeholder = onChooseImage != null && template.coverUri.isBlank()
    val borderColor = IosLabel.copy(alpha = 0.4f)
    Box(
        Modifier.fillMaxWidth().aspectRatio(BankCardAspectRatio).clip(RoundedCornerShape(24.dp))
            .background(if (placeholder) SurfaceLight else MaterialTheme.colorScheme.surfaceVariant)
            .then(if (placeholder) Modifier.clickable(onClick = onChooseImage) else Modifier)
            .then(if (placeholder) Modifier.drawWithCache {
                val stroke = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())))
                onDrawBehind { drawRoundRect(borderColor, cornerRadius = CornerRadius(24.dp.toPx()), style = stroke) }
            } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (placeholder) Icon(MaterialSymbols.AddPhotoAlternate, "选择卡面图片", Modifier.size(40.dp), tint = IosLabel)
        if (!placeholder) AccountCardBackground(
            account = Account(name = "", type = AccountType.DEBIT, templateId = template.id),
            template = template,
            modifier = Modifier.fillMaxSize(),
        )
        if (template.name.isNotBlank()) Text(
            template.name,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
            color = template.textColor.asCardTextColor() ?: SurfaceLight,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!placeholder) {
            if (onDelete != null) SummaryCardIconButton(
                MaterialSymbols.Delete, "删除卡面", onDelete,
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                contentColor = MaterialTheme.colorScheme.error,
            )
            if (onChooseImage != null) SummaryCardIconButton(
                MaterialSymbols.AddPhotoAlternate, "更换卡面", onChooseImage,
                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
            )
        }
    }
}
