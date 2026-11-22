package cn.turingzy.ledger.ui.component.input

import cn.turingzy.ledger.ui.component.base.FormFieldFrameBase
import cn.turingzy.ledger.ui.component.base.DropdownIndicatorBase
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.unit.Dp
import cn.turingzy.ledger.ui.component.base.DropdownMenuBase
import cn.turingzy.ledger.ui.theme.LedgerStyle

/**
 * 表单下拉选择框组件。
 *
 * 将标准表单字段外框 [FormFieldFrameBase] 与下拉弹窗 [LedgerDropdown] 结合，展示选中的文本与下拉指示器。
 *
 * @param label 表单项的标题标签。
 * @param value 当前选中的显示文本。
 * @param options 待选选项列表。
 * @param optionLabel 将选项转换为显示文案的映射函数。
 * @param onSelect 选中某选项时的回调。
 * @param modifier 应用于该组件的修饰符。
 * @param alignMenuToStart 菜单弹出时是否与锚点左侧（Start）对齐；为 false 时与右侧（End）对齐。
 * @param menuWidth 弹出菜单的宽度。
 * @param menuTestTag 菜单的测试标签。
 * @param isSelected 判断某选项是否处于选中状态的函数，默认根据文本匹配。
 */
@Composable
internal fun <T> FormDropdown(
    label: String,
    value: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    alignMenuToStart: Boolean = false,
    menuTestTag: String = "accountDropdownOptions",
    isSelected: (T) -> Boolean = { optionLabel(it) == value },
    menuWidth: Dp = LedgerStyle.dimensions.dropdownWidth,
) {
    LedgerDropdown(
        options = options,
        optionLabel = optionLabel,
        isSelected = isSelected,
        onSelect = onSelect,
        modifier = modifier,
        alignMenuToStart = alignMenuToStart,
        menuTestTag = menuTestTag,
        menuWidth = menuWidth,
    ) { anchorModifier, expanded, opensUp, onToggle ->
        FormFieldFrameBase(
            label = label,
            modifier = anchorModifier.semantics { contentDescription = label },
            focused = expanded,
            onClick = onToggle,
        ) {
            Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            DropdownIndicatorBase(expanded, opensUp = opensUp)
        }
    }
}

/**
 * 通用下拉选择器弹窗容器。
 *
 * 负责定位菜单位置、根据屏幕可用空间计算菜单最大高度，并将展开状态及切换函数透传给 [anchor] 锚点组件。
 *
 * @param options 待选选项列表。
 * @param optionLabel 将选项转换为文本的回调。
 * @param isSelected 判断选项是否被选中的回调。
 * @param onSelect 选项选中回调。
 * @param modifier 外部修饰符。
 * @param alignMenuToStart 是否将弹窗菜单左对齐。
 * @param menuWidth 弹出菜单的宽度。
 * @param menuTestTag 菜单测试标签。
 * @param direction 菜单展开方向（向上或向下）。
 * @param anchor 锚点组件插槽，接收锚点修饰符、展开状态、是否向上展开以及切换展开状态的回调。
 */
@Composable
internal fun <T> LedgerDropdown(
    options: List<T>,
    optionLabel: (T) -> String,
    isSelected: (T) -> Boolean,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    alignMenuToStart: Boolean = false,
    menuTestTag: String = "dropdownOptions",
    direction: DropdownDirection = DropdownDirection.UP,
    menuWidth: Dp = LedgerStyle.dimensions.dropdownWidth,
    anchor: @Composable (modifier: Modifier, expanded: Boolean, opensUp: Boolean, onToggle: () -> Unit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val menuPositionProvider = remember(alignMenuToStart, direction) { FormMenuPositionProvider(alignMenuToStart, direction) }
    var menuMaxHeight by remember { mutableStateOf(LedgerStyle.dimensions.dropdownMaxHeight) }
    val fieldCoordinates = remember { FormDropdownAnchor() }
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val view = LocalView.current
    fun updateMenuHeight(coordinates: LayoutCoordinates) {
        val top = coordinates.positionInWindow().y
        val available = when (direction) {
            DropdownDirection.UP -> top
            DropdownDirection.DOWN -> view.rootView.height - top - coordinates.size.height
        }
        menuMaxHeight = with(density) { available.coerceAtLeast(0f).toDp().coerceAtMost(LedgerStyle.dimensions.dropdownMaxHeight) }
    }
    Box(modifier.fillMaxWidth()) {
        anchor(
            Modifier.onGloballyPositioned {
                fieldCoordinates.coordinates = it
                if (expanded) updateMenuHeight(it)
            },
            expanded,
            direction == DropdownDirection.UP,
            {
                focusManager.clearFocus()
                if (!expanded) {
                    fieldCoordinates.coordinates?.takeIf { it.isAttached }?.let {
                        updateMenuHeight(it)
                    }
                }
                expanded = !expanded
            },
        )
        Box(Modifier.matchParentSize()) {
            if (expanded) DropdownMenuBase(
                options = options, optionLabel = optionLabel, isSelected = isSelected,
                onSelect = { onSelect(it); expanded = false },
                onDismissRequest = { expanded = false },
                positionProvider = menuPositionProvider,
                maxHeight = menuMaxHeight,
                menuWidth = menuWidth,
                modifier = Modifier.testTag(menuTestTag),
            )
        }
    }
}

/**
 * 保存表单下拉框锚点在窗口中布局坐标的持有类。
 */
private class FormDropdownAnchor {
    var coordinates: LayoutCoordinates? = null
}

/**
 * 下拉菜单的展开方向。
 */
internal enum class DropdownDirection {
    /** 向上弹出 */
    UP,
    /** 向下弹出 */
    DOWN
}

/**
 * 表单下拉菜单的弹窗位置计算器。
 *
 * @property alignToStart 是否与锚点起始边对齐。
 * @property direction 菜单展开方向。
 */
internal class FormMenuPositionProvider(
    private val alignToStart: Boolean,
    private val direction: DropdownDirection = DropdownDirection.UP,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset(
        x = (if (alignToStart) anchorBounds.left else anchorBounds.right - popupContentSize.width)
            .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
        y = when (direction) {
            DropdownDirection.UP -> (anchorBounds.top - popupContentSize.height).coerceAtLeast(0)
            DropdownDirection.DOWN -> anchorBounds.bottom.coerceIn(0, windowSize.height)
        },
    )
}
