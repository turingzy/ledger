package cn.turingzy.ledger.ui.feature.wallet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
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
) {
    val color = MaterialTheme.colorScheme.tertiary
    IconButton(showPressFeedback = false, onClick = onClick, enabled = enabled, modifier = modifier) {
        Icon(
            MaterialSymbols.Check,
            contentDescription = "保存账户",
            modifier = Modifier.size(24.dp),
            tint = if (enabled) color else color.copy(alpha = 0.38f),
        )
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
    Column(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            readOnly = readOnly,
            enabled = !readOnly,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
            suffix = suffix?.let { text -> { Text(text, style = MaterialTheme.typography.bodySmall) } },
            isError = error != null,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                onDone = { focusManager.clearFocus() },
            ),
            modifier = Modifier.fillMaxWidth().height(56.dp).semantics { contentDescription = label },
        )
        if (error != null) {
            Text(error, Modifier.padding(start = 12.dp, top = 4.dp), style = MaterialTheme.typography.labelSmall, color = colors.error)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> AccountFormDropdown(
    label: String,
    value: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var menuMaxHeight by remember { mutableStateOf(200.dp) }
    val density = LocalDensity.current
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            focusManager.clearFocus()
            expanded = it
        },
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
                label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                modifier = Modifier.fillMaxWidth().height(56.dp)
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .onGloballyPositioned {
                        menuMaxHeight = with(density) { it.positionInWindow().y.toDp().coerceIn(0.dp, 200.dp) }
                    }
                    .semantics { contentDescription = label },
                trailingIcon = {
                    Icon(
                        if (expanded) MaterialSymbols.ExpandMore else MaterialSymbols.ChevronRight,
                        null,
                        Modifier.size(24.dp).rotate(if (expanded) 180f else 0f),
                        tint = colors.onSurfaceVariant,
                    )
                },
            )
            Box(Modifier.align(Alignment.TopEnd).width(0.dp).height(56.dp)) {
                if (expanded) Popup(
                    popupPositionProvider = AccountMenuPositionProvider,
                    onDismissRequest = { expanded = false },
                    properties = PopupProperties(focusable = true),
                ) {
                    Surface(
                        modifier = Modifier.width(160.dp).height(menuMaxHeight),
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surface,
                        tonalElevation = 0.dp,
                        shadowElevation = 6.dp,
                        border = BorderStroke(1.dp, colors.outlineVariant),
                    ) {
                        Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 4.dp)) {
                            options.forEach { option ->
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
}


private object AccountMenuPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset(
        x = (anchorBounds.right - popupContentSize.width)
            .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
        y = (anchorBounds.top - popupContentSize.height).coerceAtLeast(0),
    )
}
