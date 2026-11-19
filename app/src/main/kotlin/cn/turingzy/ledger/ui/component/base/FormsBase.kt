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
                    .background(colors.surface).padding(horizontal = LedgerStyle.spacing.extraSmall),
                style = MaterialTheme.typography.bodyMedium,
                color = if (readOnly) colors.onSurface.copy(alpha = LedgerStyle.opacity.disabled) else colors.onSurfaceVariant,
            )
        }
        if (error != null) {
            Text(error, Modifier.padding(start = LedgerStyle.spacing.large, top = LedgerStyle.spacing.extraSmall), style = MaterialTheme.typography.labelSmall, color = colors.error)
        }
    }
}

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
) {
    val dimensions = LedgerStyle.dimensions
    val colors = LedgerStyle.colors
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        SurfaceBase(
            Modifier.width(dimensions.dropdownWidth).height(minOf(maxHeight,
                dimensions.dropdownItemHeight * options.size + LedgerStyle.spacing.extraSmall * 2)),
            shape = LedgerStyle.shapes.card, color = colors.surface,
            tonalElevation = LedgerStyle.spacing.none, shadowElevation = dimensions.dropdownShadow,
            border = BorderStroke(dimensions.dropdownBorder, colors.divider),
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
                            Icon(MaterialSymbols.Check, null, Modifier.size(LedgerStyle.icons.dropdownCheck), tint = colors.success)
                        }) else null,
                        onClick = { onSelect(option) },
                    )
                }
            }
        }
    }
}

@Composable
fun DropdownIndicatorBase(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    contentColor: Color = LedgerStyle.colors.textSecondary,
) {
    val state = stringResource(if (expanded) R.string.ui_expanded else R.string.ui_collapsed)
    Icon(
        if (expanded) MaterialSymbols.ExpandMore else MaterialSymbols.ChevronRight,
        contentDescription = null,
        modifier = modifier.size(LedgerStyle.icons.default).rotate(if (expanded) 180f else 0f)
            .semantics { stateDescription = state },
        tint = contentColor,
    )
}
