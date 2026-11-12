package cn.turingzy.ledger.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import cn.turingzy.ledger.ui.component.MaterialSymbols

@Composable
internal fun LedgerFormFieldFrame(
    label: String,
    modifier: Modifier = Modifier,
    focused: Boolean = false,
    readOnly: Boolean = false,
    error: String? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val borderColor = when {
        readOnly -> colors.onSurface.copy(alpha = 0.12f)
        error != null -> colors.error
        focused -> colors.primary
        else -> colors.outlineVariant
    }
    Column(modifier.fillMaxWidth().semantics(mergeDescendants = onClick != null) {}) {
        Box(Modifier.fillMaxWidth().padding(top = 10.dp)) {
            Row(
                Modifier.fillMaxWidth().height(48.dp)
                    .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                    .then(if (onClick == null) Modifier else Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick,
                    ))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                content = content,
            )
            Text(
                label,
                Modifier.align(Alignment.TopStart).offset(x = 12.dp, y = (-10).dp)
                    .background(colors.surface).padding(horizontal = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (readOnly) colors.onSurface.copy(alpha = 0.38f) else colors.onSurfaceVariant,
            )
        }
        if (error != null) {
            Text(error, Modifier.padding(start = 12.dp, top = 4.dp), style = MaterialTheme.typography.labelSmall, color = colors.error)
        }
    }
}

@Composable
internal fun <T> LedgerFormDropdown(
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
        LedgerFormFieldFrame(
            label = label,
            modifier = anchorModifier.semantics { contentDescription = label },
            focused = expanded,
            onClick = onToggle,
        ) {
            Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(
                if (expanded) MaterialSymbols.ExpandMore else MaterialSymbols.ChevronRight,
                null, Modifier.size(24.dp).rotate(if (expanded) 180f else 0f), tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
    anchor: @Composable (Modifier, Boolean, () -> Unit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val menuPositionProvider = remember(alignMenuToStart) { FormMenuPositionProvider(alignMenuToStart) }
    var menuMaxHeight by remember { mutableStateOf(200.dp) }
    val fieldCoordinates = remember { FormDropdownAnchor() }
    val density = LocalDensity.current
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    Box(modifier.fillMaxWidth()) {
        anchor(
            Modifier.onGloballyPositioned {
                fieldCoordinates.coordinates = it
                if (expanded) menuMaxHeight = with(density) { it.positionInWindow().y.toDp().coerceIn(0.dp, 200.dp) }
            },
            expanded,
            {
                focusManager.clearFocus()
                if (!expanded) {
                    fieldCoordinates.coordinates?.takeIf { it.isAttached }?.let {
                        menuMaxHeight = with(density) { it.positionInWindow().y.toDp().coerceIn(0.dp, 200.dp) }
                    }
                }
                expanded = !expanded
            },
        )
        Box(Modifier.matchParentSize()) {
            if (expanded) Popup(
                popupPositionProvider = menuPositionProvider,
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                Surface(
                    modifier = Modifier.width(160.dp).height(minOf(menuMaxHeight, (options.size * 36 + 8).dp)),
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surface,
                    tonalElevation = 0.dp,
                    shadowElevation = 6.dp,
                    border = BorderStroke(1.dp, colors.outlineVariant),
                ) {
                    LazyColumn(
                        modifier = Modifier.testTag(menuTestTag),
                        state = rememberLazyListState(
                            initialFirstVisibleItemIndex = options.indexOfFirst(isSelected).coerceAtLeast(0),
                        ),
                        contentPadding = PaddingValues(vertical = 4.dp),
                    ) {
                        itemsIndexed(options, key = { index, _ -> index }) { _, option ->
                            DropdownMenuItem(
                                modifier = Modifier.height(36.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                text = {
                                    Text(
                                        optionLabel(option), style = MaterialTheme.typography.bodyMedium,
                                        color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                trailingIcon = if (isSelected(option)) ({
                                    Icon(MaterialSymbols.Check, null, Modifier.size(18.dp), tint = colors.tertiary)
                                }) else null,
                                onClick = { onSelect(option); expanded = false },
                            )
                        }
                    }
                }
            }
        }
    }
}

private class FormDropdownAnchor {
    var coordinates: LayoutCoordinates? = null
}

private class FormMenuPositionProvider(private val alignToStart: Boolean) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset(
        x = (if (alignToStart) anchorBounds.left else anchorBounds.right - popupContentSize.width)
            .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
        y = (anchorBounds.top - popupContentSize.height).coerceAtLeast(0),
    )
}
