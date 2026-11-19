package cn.turingzy.ledger.ui.component.input

import cn.turingzy.ledger.ui.component.base.FormFieldFrameBase
import cn.turingzy.ledger.ui.component.base.DropdownIndicatorBase
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import cn.turingzy.ledger.ui.component.base.DropdownMenuBase
import cn.turingzy.ledger.ui.theme.LedgerStyle

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
) {
    LedgerDropdown(
        options = options,
        optionLabel = optionLabel,
        isSelected = isSelected,
        onSelect = onSelect,
        modifier = modifier,
        alignMenuToStart = alignMenuToStart,
        menuTestTag = menuTestTag,
    ) { anchorModifier, expanded, onToggle ->
        FormFieldFrameBase(
            label = label,
            modifier = anchorModifier.semantics { contentDescription = label },
            focused = expanded,
            onClick = onToggle,
        ) {
            Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            DropdownIndicatorBase(expanded)
        }
    }
}

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
    anchor: @Composable (Modifier, Boolean, () -> Unit) -> Unit,
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
                modifier = Modifier.testTag(menuTestTag),
            )
        }
    }
}

private class FormDropdownAnchor {
    var coordinates: LayoutCoordinates? = null
}

internal enum class DropdownDirection { UP, DOWN }

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
