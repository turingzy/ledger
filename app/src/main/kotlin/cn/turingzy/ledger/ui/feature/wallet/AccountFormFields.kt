package cn.turingzy.ledger.ui.feature.wallet

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
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import cn.turingzy.ledger.ui.component.LedgerIconButton as IconButton
import cn.turingzy.ledger.ui.component.MaterialSymbols

@Composable
internal fun AccountSaveButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "保存账户",
) {
    val color = MaterialTheme.colorScheme.tertiary
    IconButton(showPressFeedback = false, onClick = onClick, enabled = enabled, modifier = modifier) {
        Icon(
            MaterialSymbols.Check,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
            tint = if (enabled) color else color.copy(alpha = 0.38f),
        )
    }
}

@Composable
private fun AccountFieldFrame(
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
internal fun AccountFormTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    suffix: String? = null,
    error: String? = null,
    readOnly: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
) {
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val textColor = if (readOnly) colors.onSurface.copy(alpha = 0.38f) else colors.onSurface
    AccountFieldFrame(
        label, modifier, focused = focused, readOnly = readOnly, error = error,
        onClick = if (readOnly) null else ({ focusRequester.requestFocus() }),
    ) {
        BasicTextField(
            value = value, onValueChange = onValueChange, enabled = !readOnly, readOnly = readOnly,
            singleLine = true, interactionSource = source,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor),
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                onDone = { focusManager.clearFocus() },
            ),
            modifier = Modifier.weight(1f).focusRequester(focusRequester).semantics { contentDescription = label },
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    inner()
                }
            },
        )
        if (suffix != null) Text(suffix, style = MaterialTheme.typography.bodySmall, color = textColor)
    }
}

@Composable
internal fun AccountCardFaceField(
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    disabledHint: String = "先填写账户名称和四位尾号",
) {
    val colors = MaterialTheme.colorScheme
    val contentColor = if (enabled) colors.onSurface else colors.onSurface.copy(alpha = 0.38f)
    Column(modifier.fillMaxWidth().semantics { if (!enabled) disabled() }) {
        AccountFieldFrame("卡面", readOnly = !enabled, onClick = if (enabled) onClick else null) {
            Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = contentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(MaterialSymbols.ChevronRight, null, Modifier.size(24.dp), tint = contentColor)
        }
        if (!enabled) {
            Text(
                disabledHint,
                Modifier.padding(start = 12.dp, top = 4.dp),
                style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun AccountTailNumberField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val focusRequester = remember { FocusRequester() }
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("尾号", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        BasicTextField(
            value = value,
            onValueChange = { if (it.length <= 4 && it.all { digit -> digit in '0'..'9' }) onValueChange(it) },
            singleLine = true,
            interactionSource = source,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            modifier = Modifier.width(200.dp).height(44.dp).focusRequester(focusRequester)
                .semantics { contentDescription = "尾号" },
            decorationBox = { inner ->
                Box(
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() }, indication = null,
                    ) {
                        focusRequester.requestFocus()
                        keyboard?.show()
                    },
                ) {
                    Box(Modifier.alpha(0f)) { inner() }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(4) { index ->
                            val borderColor = if (focused && index == value.length.coerceAtMost(3)) colors.primary else colors.outlineVariant
                            Box(
                                Modifier.size(44.dp).border(1.dp, borderColor, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(value.getOrNull(index)?.toString().orEmpty(), style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
                            }
                        }
                    }
                }
            },
        )
    }
}

@Composable
internal fun <T> AccountFormDropdown(
    label: String,
    value: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    alignMenuToStart: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    val menuPositionProvider = remember(alignMenuToStart) { AccountMenuPositionProvider(alignMenuToStart) }
    var menuMaxHeight by remember { mutableStateOf(200.dp) }
    val fieldCoordinates = remember { AccountDropdownAnchor() }
    val density = LocalDensity.current
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    Box(modifier.fillMaxWidth()) {
        AccountFieldFrame(
            label = label,
            modifier = Modifier
                .onGloballyPositioned {
                    fieldCoordinates.coordinates = it
                    if (expanded) menuMaxHeight = with(density) { it.positionInWindow().y.toDp().coerceIn(0.dp, 200.dp) }
                }
                .semantics { contentDescription = label },
            focused = expanded,
            onClick = {
                focusManager.clearFocus()
                if (!expanded) {
                    fieldCoordinates.coordinates?.takeIf { it.isAttached }?.let {
                        menuMaxHeight = with(density) { it.positionInWindow().y.toDp().coerceIn(0.dp, 200.dp) }
                    }
                }
                expanded = !expanded
            },
        ) {
            Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(
                if (expanded) MaterialSymbols.ExpandMore else MaterialSymbols.ChevronRight,
                null, Modifier.size(24.dp).rotate(if (expanded) 180f else 0f), tint = colors.onSurfaceVariant,
            )
        }
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
                        modifier = Modifier.testTag("accountDropdownOptions"),
                        state = rememberLazyListState(
                            initialFirstVisibleItemIndex = options.indexOfFirst { optionLabel(it) == value }.coerceAtLeast(0),
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
                                trailingIcon = if (optionLabel(option) == value) ({
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

private class AccountDropdownAnchor {
    var coordinates: LayoutCoordinates? = null
}

private class AccountMenuPositionProvider(private val alignToStart: Boolean) : PopupPositionProvider {
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
