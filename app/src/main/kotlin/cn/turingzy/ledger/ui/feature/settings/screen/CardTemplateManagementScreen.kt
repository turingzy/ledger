package cn.turingzy.ledger.ui.feature.settings.screen

import cn.turingzy.ledger.ui.feature.settings.component.CardTextColorPicker
import cn.turingzy.ledger.ui.feature.settings.component.SettingsItemHeight
import cn.turingzy.ledger.ui.feature.settings.component.SettingsSubpageScaffold
import cn.turingzy.ledger.ui.feature.settings.component.SettingsDeleteAction
import cn.turingzy.ledger.ui.feature.settings.component.settingsPageScroll
import cn.turingzy.ledger.R
import cn.turingzy.ledger.ui.component.base.IconButtonContentBase
import cn.turingzy.ledger.ui.component.base.IconButtonBase
import cn.turingzy.ledger.ui.component.base.ButtonBase
import cn.turingzy.ledger.ui.component.base.ButtonVariantBase
import androidx.compose.ui.res.stringResource
import cn.turingzy.ledger.ui.interaction.uiClickable
import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.ui.component.base.SurfaceBase
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cn.turingzy.ledger.navigation.SettingsEditorCreate
import cn.turingzy.ledger.navigation.SettingsEditorList
import cn.turingzy.ledger.navigation.SettingsEditorNavHost
import cn.turingzy.ledger.navigation.openSettingsCreatePage
import cn.turingzy.ledger.navigation.settingsCreatePage
import cn.turingzy.ledger.model.account.CardTemplate
import cn.turingzy.ledger.model.account.selectableCardTemplates
import cn.turingzy.ledger.ui.component.card.bank.BankCardAspectRatio
import cn.turingzy.ledger.ui.component.card.bank.CardPageIndicator
import cn.turingzy.ledger.ui.component.card.bank.AutomaticCardTextColor
import cn.turingzy.ledger.ui.component.card.bank.TitaniumCardSurface
import cn.turingzy.ledger.ui.component.card.bank.TitaniumDrag
import cn.turingzy.ledger.ui.component.card.bank.titaniumImageUri
import cn.turingzy.ledger.ui.component.card.bank.titaniumInkOverride
import cn.turingzy.ledger.ui.component.card.bank.titaniumPreset
import cn.turingzy.ledger.ui.component.card.bank.titaniumTitle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.card.bank.rememberCircularCardPagerState

/**
 * 卡片背景模板管理页面屏幕组件。
 *
 * 提供卡片外观轮播预览、名称与文字颜色实时修改、封面图片替换、删除模板，
 * 以及新增自定义卡片模板的完整管理流程。
 *
 * @param templates 当前已有的卡片模板列表
 * @param onDismissRequest 返回或退出回调
 * @param onCreate 新建卡片模板的挂起函数，返回生成的新模板 ID
 * @param onUpdate 更新卡片模板的回调
 * @param onDelete 删除卡片模板的回调
 * @param onCopyImage 将外部选择的图片拷贝至内部存储的挂起函数
 */
@Composable
internal fun CardTemplateManagementScreen(
    templates: List<CardTemplate>,
    onDismissRequest: () -> Unit,
    onCreate: suspend (CardTemplate) -> Long?,
    onUpdate: (CardTemplate) -> Unit,
    onDelete: (Long) -> Unit,
    onCopyImage: suspend (Uri) -> String?,
) {
    val textResources = androidx.compose.ui.platform.LocalResources.current
    val scope = rememberCoroutineScope()
    val controller = rememberNavController()
    val create by rememberUpdatedState(onCreate)
    val update by rememberUpdatedState(onUpdate)
    val delete by rememberUpdatedState(onDelete)
    val dismiss by rememberUpdatedState(onDismissRequest)
    val drafts = remember { mutableStateListOf<CardTemplate>().apply { addAll(templates.selectableCardTemplates()) } }
    var selectedId by remember { mutableStateOf(drafts.firstOrNull()?.id) }
    val pagerState = rememberCircularCardPagerState(drafts.map { it.id }, selectedId)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            if (drafts.isNotEmpty()) selectedId = drafts[Math.floorMod(page, drafts.size)].id
        }
    }
    var newFace by remember { mutableStateOf(CardTemplate(id = -1L, name = "", coverUri = "")) }
    var uploadTarget by remember { mutableStateOf<Long?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val target = uploadTarget
        val entry = controller.currentBackStackEntry
        if (uri != null) scope.launch {
            val image = onCopyImage(uri) ?: return@launch
            val index = drafts.indexOfFirst { it.id == target }
            if (entry?.destination?.route == SettingsEditorCreate &&
                controller.currentBackStackEntry == entry && newFace.id == target
            ) {
                newFace = newFace.copy(coverUri = image)
            } else if (index >= 0) {
                val current = drafts[index]
                val updated = current.copy(
                    coverUri = image,
                    textColor = if (current.titaniumInkOverride() == null) AutomaticCardTextColor else current.textColor,
                )
                drafts[index] = updated
                update(updated)
            }
        }
        uploadTarget = null
    }

    fun replaceImage(template: CardTemplate) {
        uploadTarget = template.id
        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    SettingsEditorNavHost(controller) {
        settingsCreatePage { entry ->
            var saving by remember { mutableStateOf(false) }
            NewCardTemplatePage(
                pending = newFace,
                onChange = { newFace = it },
                onChooseImage = { replaceImage(newFace) },
                onClose = { controller.popBackStack() },
                onSave = {
                    val pending = newFace.copy(name = newFace.name.trim())
                    saving = true
                    scope.launch {
                        try {
                            val id = create(pending) ?: return@launch
                            drafts.add(pending.copy(id = id))
                            selectedId = id
                            if (controller.currentBackStackEntry == entry) controller.popBackStack()
                        } finally {
                            saving = false
                        }
                    }
                },
                saving = saving,
            )
        }
        composable(SettingsEditorList) {
            val selectedIndex = if (drafts.isEmpty()) -1 else Math.floorMod(pagerState.currentPage, drafts.size)
            val selected = drafts.getOrNull(selectedIndex)
            SettingsSubpageScaffold(
                title = textResources.getString(cn.turingzy.ledger.R.string.ui_card_designs),
                onBack = dismiss,
                actions = {
                    IconButtonBase(
                        MaterialSymbols.Add,
                        textResources.getString(R.string.ui_new_card_design),
                        onClick = {
                            newFace = CardTemplate(id = minOf(drafts.minOfOrNull { it.id } ?: 0L, 0L) - 1L, name = "", coverUri = "")
                            controller.openSettingsCreatePage()
                        },
                    )
                },
            ) {
                Column(
                    Modifier.fillMaxWidth().settingsPageScroll(),
                    verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.large),
                ) {
                    if (selected != null) {
                        HorizontalPager(state = pagerState, key = { it }, pageSpacing = 12.dp) { pageIndex ->
                            drafts.getOrNull(if (drafts.isEmpty()) -1 else Math.floorMod(pageIndex, drafts.size))?.let { face ->
                                TemplateCoverPreview(face, motionEnabled = pageIndex == pagerState.settledPage)
                            }
                        }
                        CardPageIndicator(pageCount = drafts.size, currentPage = selectedIndex)
                        CardFaceProperties(
                            template = selected,
                            onChange = { updated ->
                                val index = drafts.indexOfFirst { it.id == updated.id }
                                if (index >= 0) {
                                    drafts[index] = updated
                                    if (updated.name.isNotBlank()) update(updated)
                                }
                            },
                        )
                        CardFaceActions(
                            selected.id,
                            enabled = !pagerState.isScrollInProgress,
                            onChooseImage = { replaceImage(selected) },
                            onDelete = {
                                val index = drafts.indexOfFirst { it.id == selected.id }
                                if (index >= 0) {
                                    delete(selected.id)
                                    drafts.removeAt(index)
                                    if (drafts.isEmpty()) {
                                        newFace = CardTemplate(id = -1L, name = "", coverUri = "")
                                        controller.openSettingsCreatePage()
                                    }
                                    selectedId = drafts.getOrNull(index.coerceAtMost(drafts.lastIndex))?.id
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * 新增卡片模板页面组件。
 *
 * 独立于轮播管理页全屏打开，提供选择封面图片、输入模板名称及设置卡片文字颜色的完整表单。
 *
 * @param pending 正在创建的卡片模板草稿数据
 * @param onChange 模板数据变更时的回调
 * @param onChooseImage 点击选择封面图片的回调
 * @param onClose 点击关闭或返回的回调
 * @param onSave 点击确认保存的回调
 * @param saving 是否正在执行保存操作
 */
@Composable
internal fun NewCardTemplatePage(
    pending: CardTemplate,
    onChange: (CardTemplate) -> Unit,
    onChooseImage: () -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit,
    saving: Boolean,
) {
    val canSave = !saving && pending.name.isNotBlank() && pending.coverUri.isNotBlank()
    SettingsSubpageScaffold(
        title = stringResource(R.string.ui_new_card_design),
        onBack = onClose,
        closeInsteadOfBack = true,
        actions = {
            IconButtonContentBase(enabled = canSave, onClick = onSave) {
                Icon(
                    MaterialSymbols.Check,
                    stringResource(R.string.ui_add_card_design),
                    tint = MaterialTheme.colorScheme.tertiary.copy(alpha = if (canSave) 1f else LedgerStyle.opacity.disabled),
                )
            }
        },
    ) {
        Column(
            Modifier.fillMaxWidth().settingsPageScroll(),
            verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.large),
        ) {
            TemplateCoverPreview(pending, onChooseImage = onChooseImage)
            CardFaceProperties(pending, onChange = onChange)
            if (pending.coverUri.isNotBlank()) CardFaceActions(pending.id, onChooseImage = onChooseImage)
        }
    }
}

/**
 * 卡片模板的操作操作栏组件。
 *
 * 提供更换卡片封面按钮，以及右侧带二次确认的删除模板操作。
 *
 * @param templateId 模板唯一标识 ID
 * @param onChooseImage 点击更换封面图片回调
 * @param enabled 是否允许操作交互
 * @param onDelete 点击删除模板回调，为 null 时不显示删除按钮
 */
@Composable
private fun CardFaceActions(
    templateId: Long,
    onChooseImage: () -> Unit,
    enabled: Boolean = true,
    onDelete: (() -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        ButtonBase(stringResource(R.string.ui_change_card_design), onChooseImage,
            enabled = enabled, variant = ButtonVariantBase.Text)
        if (onDelete != null) {
            SettingsDeleteAction(templateId, stringResource(R.string.ui_delete_card_design), onDelete, enabled = enabled)
        }
    }
}

/**
 * 卡片模板属性编辑表单组件。
 *
 * 包含模板名称的输入框以及文字颜色的拾色器。
 *
 * @param template 当前编辑的卡片模板
 * @param onChange 模板属性变更回调
 */
@Composable
private fun CardFaceProperties(
    template: CardTemplate,
    onChange: (CardTemplate) -> Unit,
) {
    val textResources = androidx.compose.ui.platform.LocalResources.current
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.large)) {
        SurfaceBase(shape = LedgerStyle.shapes.card, color = colors.surface) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = SettingsItemHeight).padding(horizontal = LedgerStyle.spacing.large),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(textResources.getString(cn.turingzy.ledger.R.string.ui_design_name), Modifier.width(LedgerStyle.dimensions.formLabelWidth), style = MaterialTheme.typography.bodyLarge)
                BasicTextField(
                    value = template.titaniumTitle(),
                    onValueChange = { onChange(template.copy(name = it)) },
                    modifier = Modifier.weight(1f).semantics { contentDescription = textResources.getString(cn.turingzy.ledger.R.string.ui_design_name) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurfaceVariant, textAlign = TextAlign.End),
                    cursorBrush = SolidColor(colors.onSurface),
                    decorationBox = { field ->
                        Box(contentAlignment = Alignment.CenterEnd) {
                            if (template.name.isEmpty()) Text(textResources.getString(cn.turingzy.ledger.R.string.ui_enter_a_design_name), color = colors.onSurfaceVariant)
                            field()
                        }
                    },
                )
            }
        }
        SurfaceBase(shape = LedgerStyle.shapes.card, color = colors.surface) {
            CardTextColorPicker(
                value = template.textColor,
                automatic = template.titaniumInkOverride() == null,
                onChange = { hex ->
                    val explicitColor = if (hex.length == 7) "#FF" + hex.drop(1) else hex
                    onChange(template.copy(textColor = explicitColor))
                },
            )
        }
    }
}

/**
 * 卡片模板封面预览组件。
 *
 * 当未配置封面图片且支持选图时，展示虚线边框的上传占位区域；
 * 否则渲染带有真实 3D 倾斜和高光动效的钛金属质感卡片预览。
 *
 * @param template 待预览的卡片模板
 * @param onChooseImage 点击选择封面图片的回调（为 null 表示只读预览）
 * @param motionEnabled 是否开启触摸拖拽和倾斜动画
 */
@Composable
private fun TemplateCoverPreview(
    template: CardTemplate,
    onChooseImage: (() -> Unit)? = null,
    motionEnabled: Boolean = true,
) {
    val textResources = androidx.compose.ui.platform.LocalResources.current
    if (onChooseImage != null && template.coverUri.isBlank()) {
        val borderColor = LedgerStyle.colors.textPrimary.copy(alpha = 0.4f)
        Box(
            Modifier.fillMaxWidth().aspectRatio(BankCardAspectRatio).clip(LedgerStyle.shapes.dialog)
                .background(LedgerStyle.colors.surface)
                .uiClickable(onClick = onChooseImage)
                .drawWithCache {
                    val stroke = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())))
                    onDrawBehind { drawRoundRect(borderColor, cornerRadius = CornerRadius(24.dp.toPx()), style = stroke) }
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier.padding(LedgerStyle.spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.extraSmall),
            ) {
                Icon(MaterialSymbols.AddPhotoAlternate, contentDescription = null,
                    modifier = Modifier.size(LedgerStyle.icons.imagePlaceholder), tint = LedgerStyle.colors.textPrimary)
                Text(textResources.getString(cn.turingzy.ledger.R.string.ui_upload_card_image),
                    style = LedgerStyle.typography.bodyMedium, color = LedgerStyle.colors.textPrimary)
                Text(textResources.getString(cn.turingzy.ledger.R.string.ui_card_image_recommendation),
                    style = LedgerStyle.typography.caption, color = LedgerStyle.colors.textSecondary,
                    textAlign = TextAlign.Center)
            }
        }
        return
    }

    TitaniumCardSurface(
        preset = template.titaniumPreset(),
        imageUri = template.titaniumImageUri(),
        modifier = Modifier.fillMaxWidth(),
        textColor = template.titaniumInkOverride(),
        motionEnabled = motionEnabled,
        drag = TitaniumDrag.AfterLongPress,
    ) { ink ->
        if (template.name.isNotBlank()) Text(
            template.titaniumTitle(),
            modifier = Modifier.align(Alignment.TopStart).padding(start = 20.dp, top = 17.dp, end = 20.dp),
            color = ink,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Normal),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
