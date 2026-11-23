package cn.turingzy.ledger.ui.component.base

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import cn.turingzy.ledger.R
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.theme.LedgerStyle

/**
 * 表单字段外框容器。
 *
 * 提供浮动标签、状态边框（正常、聚焦、只读、错误）及底部错误文案展示。
 *
 * @param label 浮动显示在上边框的标签文本。
 * @param modifier 应用于整个表单字段容器的修饰符。
 * @param focused 是否处于聚焦状态。
 * @param readOnly 是否为只读状态。
 * @param error 错误提示文本，为 null 时不显示。
 * @param onClick 可选的点击事件回调，传入时使整个外框可点击。
 * @param content 字段内部行内容插槽。
 */
@Composable
fun FormFieldFrameBase(
    label: String,
    modifier: Modifier = Modifier,
    focused: Boolean = false,
    readOnly: Boolean = false,
    error: String? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val labelBackground = LocalSurfaceBackground.current.takeOrElse { colors.surface }
    val borderColor = when {
        readOnly -> colors.onSurface.copy(alpha = LedgerStyle.opacity.readOnlyBorder)
        error != null -> colors.error
        focused -> colors.primary
        else -> colors.outlineVariant
    }
    Column(modifier.fillMaxWidth().semantics(mergeDescendants = onClick != null) {}) {
        Box(Modifier.fillMaxWidth().padding(top = LedgerStyle.dimensions.fieldLabelOffset)) {
            Row(
                Modifier.fillMaxWidth().height(LedgerStyle.dimensions.rowMinHeight)
                    .border(LedgerStyle.dimensions.formBorderThickness, borderColor, LedgerStyle.shapes.field)
                    .then(if (onClick == null) Modifier else Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick,
                    ))
                    .padding(horizontal = LedgerStyle.spacing.large),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.medium),
                content = content,
            )
            Text(
                label,
                Modifier.align(Alignment.TopStart).offset(x = LedgerStyle.spacing.large, y = -LedgerStyle.dimensions.fieldLabelOffset)
                    .background(labelBackground).padding(horizontal = LedgerStyle.spacing.extraSmall),
                style = MaterialTheme.typography.bodyMedium,
                color = if (readOnly) colors.onSurface.copy(alpha = LedgerStyle.opacity.disabled) else colors.onSurfaceVariant,
            )
        }
        if (error != null) {
            Text(error, Modifier.padding(start = LedgerStyle.spacing.large, top = LedgerStyle.spacing.extraSmall), style = MaterialTheme.typography.labelSmall, color = colors.error)
        }
    }
}

/**
 * 基础下拉菜单组件。
 *
 * 在独立弹窗 ([Popup]) 中以列表形式展示选项，支持自定义定位、选中状态勾选标识以及自动滚动至初始选中项。
 *
 * @param T 选项数据类型。
 * @param options 待展示的选项列表。
 * @param optionLabel 获取选项显示文本的转换函数。
 * @param isSelected 判断某选项是否处于选中状态的判断函数。
 * @param onSelect 点击选中某个选项时的回调。
 * @param onDismissRequest 请求关闭下拉菜单时的回调。
 * @param positionProvider 决定弹窗相对锚点显示位置的 [PopupPositionProvider]。
 * @param modifier 应用于内部滚动列表的修饰符。
 * @param maxHeight 下拉菜单的最大允许高度。
 * @param menuWidth 弹出菜单的宽度。
 * @param optionTextColor 选项文字颜色。
 */
@Composable
fun <T> DropdownMenuBase(
    options: List<T>,
    optionLabel: (T) -> String,
    isSelected: (T) -> Boolean,
    onSelect: (T) -> Unit,
    onDismissRequest: () -> Unit,
    positionProvider: PopupPositionProvider,
    modifier: Modifier = Modifier,
    maxHeight: Dp = LedgerStyle.dimensions.dropdownMaxHeight,
    optionTextColor: Color = LedgerStyle.colors.textPrimary,
    menuWidth: Dp = LedgerStyle.dimensions.dropdownWidth,
) {
    val dimensions = LedgerStyle.dimensions
    val colors = LedgerStyle.colors
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        SurfaceBase(
            Modifier.width(menuWidth).height(minOf(maxHeight,
                dimensions.dropdownItemHeight * options.size + LedgerStyle.spacing.extraSmall * 2)),
            shape = LedgerStyle.shapes.card, color = colors.overlay, contentColor = colors.textPrimary,
            tonalElevation = LedgerStyle.spacing.none, shadowElevation = dimensions.dropdownShadow,
            border = BorderStroke(dimensions.dropdownBorder, colors.outline),
        ) {
            LazyColumn(
                modifier = modifier,
                state = rememberLazyListState(initialFirstVisibleItemIndex = options.indexOfFirst(isSelected).coerceAtLeast(0)),
                contentPadding = PaddingValues(vertical = LedgerStyle.spacing.extraSmall),
            ) {
                itemsIndexed(options, key = { index, _ -> index }) { _, option ->
                    DropdownMenuItem(
                        modifier = Modifier.height(dimensions.dropdownItemHeight),
                        contentPadding = PaddingValues(horizontal = LedgerStyle.spacing.large),
                        text = {
                            Text(optionLabel(option), style = MaterialTheme.typography.bodyMedium,
                                color = optionTextColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        trailingIcon = if (isSelected(option)) ({
                            Icon(MaterialSymbols.Check, null, Modifier.size(LedgerStyle.icons.inline), tint = colors.info)
                        }) else null,
                        onClick = { onSelect(option) },
                    )
                }
            }
        }
    }
}

/**
 * 下拉展开状态指示图标。
 *
 * 折叠时显示向右箭头，展开时显示下拉箭头（并根据是否向上弹出进行旋转指示），并附带无障碍语义状态描述。
 *
 * @param expanded 是否处于展开状态。
 * @param modifier 应用于指示图标的修饰符。
 * @param opensUp 菜单展开时是否朝上方弹出（朝上弹出时箭头翻转 180 度）。
 * @param contentColor 图标颜色。
 */
@Composable
fun DropdownIndicatorBase(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    opensUp: Boolean = true,
    contentColor: Color = LedgerStyle.colors.textSecondary,
) {
    val state = stringResource(if (expanded) R.string.ui_expanded else R.string.ui_collapsed)
    Icon(
        if (expanded) MaterialSymbols.ExpandMore else MaterialSymbols.ChevronRight,
        contentDescription = null,
        modifier = modifier.size(LedgerStyle.icons.inline).rotate(if (expanded && opensUp) 180f else 0f)
            .semantics { stateDescription = state },
        tint = contentColor,
    )
}
