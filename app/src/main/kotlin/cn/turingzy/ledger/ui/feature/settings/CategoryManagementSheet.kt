package cn.turingzy.ledger.ui.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.ui.component.categoryGlyph
import cn.turingzy.ledger.ui.component.LedgerSegmentedControl
import cn.turingzy.ledger.model.Category
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.model.CategoryWithIcon
import cn.turingzy.ledger.model.Icon as CategoryIcon
import kotlinx.coroutines.launch

@Composable
internal fun CategoryManagementSheet(
    categories: List<CategoryWithIcon>,
    availableIcons: List<CategoryIcon>,
    onDismissRequest: () -> Unit,
    onSaveCategory: suspend (Category) -> Boolean,
    onDeleteCategory: suspend (Category) -> Boolean,
) {
    var selectedType by remember { mutableStateOf(CategoryType.EXPENSE) }
    var draft by remember { mutableStateOf<Category?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val editing = draft
    val valid = editing != null && editing.name.isNotBlank() && availableIcons.any { it.id == editing.iconId }

    fun returnToGrid() {
        focusManager.clearFocus()
        draft = null
        error = null
    }

    SettingsBottomSheet(
        title = when {
            editing == null -> "分类管理"
            editing.id == 0L -> "新增分类"
            else -> "分类详情"
        },
        onDismissRequest = onDismissRequest,
        onBack = if (editing != null) ({ if (!busy) returnToGrid() }) else null,
        action = {
            if (busy) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = MaterialTheme.colorScheme.tertiary, strokeWidth = 2.dp)
                }
            } else {
                IconButton(
                    enabled = editing == null || valid,
                    onClick = {
                        if (editing == null) {
                            error = null
                            draft = Category(
                                name = "",
                                type = selectedType,
                                iconId = availableIcons.firstOrNull()?.id ?: 0L,
                                sortOrder = (categories.filter { it.category.type == selectedType }.maxOfOrNull { it.category.sortOrder } ?: 0) + 1,
                            )
                        } else {
                            focusManager.clearFocus()
                            scope.launch {
                                busy = true
                                try {
                                    if (onSaveCategory(editing)) {
                                        selectedType = editing.type
                                        returnToGrid()
                                    } else error = "保存失败，请重试"
                                } finally {
                                    busy = false
                                }
                            }
                        }
                    },
                ) {
                    Icon(
                        if (editing == null) Icons.Default.Add else Icons.Default.Check,
                        if (editing == null) "新增分类" else "保存分类",
                        tint = if (editing == null) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.tertiary.copy(alpha = if (valid) 1f else 0.38f),
                    )
                }
            }
        },
    ) {
        if (editing == null) {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                CategoryTypeSwitch(selectedType, onSelect = { selectedType = it })
                val visible = categories.filter { it.category.type == selectedType }.sortedBy { it.category.sortOrder }
                if (visible.isEmpty()) {
                    Text(
                        "暂无分类，点击右上角添加",
                        Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(visible, key = { it.category.id }) { item ->
                            CategoryIconTile(item.icon, item.category.name, onClick = { draft = item.category })
                        }
                    }
                }
            }
        } else {
            CategoryEditor(
                category = editing,
                availableIcons = availableIcons,
                enabled = !busy,
                error = error,
                onChange = { draft = it; error = null },
                onDelete = { confirmDelete = true },
            )
        }
    }

    if (confirmDelete && editing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除分类") },
            text = { Text("确定删除“${editing.name}”分类吗？") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        busy = true
                        try {
                            if (onDeleteCategory(editing)) returnToGrid()
                            else error = "删除失败，请重试"
                        } finally {
                            busy = false
                        }
                    }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun CategoryTypeSwitch(selected: CategoryType, onSelect: (CategoryType) -> Unit, enabled: Boolean = true) {
    val types = CategoryType.entries
    LedgerSegmentedControl(
        labels = types.map { it.label() },
        selectedIndex = types.indexOf(selected),
        onSelect = { onSelect(types[it]) },
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
    )
}

@Composable
private fun CategoryIconTile(
    icon: CategoryIcon,
    title: String?,
    onClick: () -> Unit,
    selected: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.clip(RoundedCornerShape(12.dp)).clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            enabled = enabled,
            onClick = onClick,
        ).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(colors.surfaceVariant.copy(alpha = 0.45f))
                .then(if (selected) Modifier.border(1.dp, colors.onSurface, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon.categoryGlyph(), if (title == null) icon.displayName else null, Modifier.size(24.dp), tint = colors.onSurface)
        }
        if (title != null) {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = colors.onSurface)
        }
    }
}

@Composable
private fun CategoryEditor(
    category: Category,
    availableIcons: List<CategoryIcon>,
    enabled: Boolean,
    error: String?,
    onChange: (Category) -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val icon = availableIcons.firstOrNull { it.id == category.iconId }
    Column(
        Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(72.dp).clip(CircleShape).background(colors.surfaceVariant.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon?.categoryGlyph() ?: Icons.Outlined.MoreHoriz, null, Modifier.size(32.dp), tint = colors.onSurface) }
        }
        if (category.id == 0L) CategoryTypeSwitch(category.type, { onChange(category.copy(type = it)) }, enabled)
        Surface(shape = RoundedCornerShape(16.dp), color = colors.surfaceVariant.copy(alpha = 0.45f)) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("分类名称", Modifier.width(96.dp))
                    BasicTextField(
                        value = category.name,
                        onValueChange = { onChange(category.copy(name = it)) },
                        enabled = enabled,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface, textAlign = TextAlign.End),
                        cursorBrush = SolidColor(colors.onSurface),
                        modifier = Modifier.weight(1f).semantics { contentDescription = "分类名称" },
                        decorationBox = { field ->
                            Box(contentAlignment = Alignment.CenterEnd) {
                                if (category.name.isEmpty()) Text("请输入名称", color = colors.onSurfaceVariant)
                                field()
                            }
                        },
                    )
                }
                if (category.id != 0L) {
                    HorizontalDivider(thickness = 0.5.dp, color = colors.outlineVariant)
                    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("分类类型")
                        Text(category.type.label(), color = colors.onSurfaceVariant)
                    }
                }
                if (category.id != 0L && !category.isSystem) {
                    HorizontalDivider(thickness = 0.5.dp, color = colors.outlineVariant)
                    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("删除分类")
                        IconButton(onClick = onDelete, enabled = enabled) {
                            Icon(Icons.Outlined.DeleteOutline, "删除分类", tint = colors.error)
                        }
                    }
                }
            }
        }
        if (error != null) Text(error, style = MaterialTheme.typography.bodySmall, color = colors.error)
        Text("选择图标", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        if (availableIcons.isEmpty()) {
            Text("暂无可用图标", color = colors.onSurfaceVariant)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(availableIcons, key = { it.id }) { option ->
                    CategoryIconTile(
                        option, title = null,
                        onClick = { onChange(category.copy(iconId = option.id)) },
                        selected = option.id == category.iconId,
                        enabled = enabled,
                    )
                }
            }
        }
    }
}

private fun CategoryType.label(): String = when (this) {
    CategoryType.EXPENSE -> "支出"
    CategoryType.INCOME -> "收入"
    CategoryType.TRANSFER -> "转账"
}
