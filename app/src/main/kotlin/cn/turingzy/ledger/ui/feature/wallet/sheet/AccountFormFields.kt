package cn.turingzy.ledger.ui.feature.wallet.sheet

import cn.turingzy.ledger.ui.component.base.BaseFormFieldFrame
import cn.turingzy.ledger.ui.component.base.BaseIconButtonContent
import cn.turingzy.ledger.ui.theme.LedgerStyle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.ui.component.MaterialSymbols

@Composable
internal fun AccountSaveButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = androidx.compose.ui.res.stringResource(cn.turingzy.ledger.R.string.ui_save_account),
) {
    val color = MaterialTheme.colorScheme.tertiary
    BaseIconButtonContent(showPressFeedback = false, onClick = onClick, enabled = enabled, modifier = modifier) {
        Icon(
            MaterialSymbols.Check,
            contentDescription = contentDescription,
            modifier = Modifier.size(LedgerStyle.icons.default),
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
    val focusRequester = remember { FocusRequester() }
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val textColor = if (readOnly) colors.onSurface.copy(alpha = 0.38f) else colors.onSurface
    BaseFormFieldFrame(
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
    disabledHint: String = androidx.compose.ui.res.stringResource(cn.turingzy.ledger.R.string.ui_enter_an_account_name_and_last_four_digits),
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val colors = MaterialTheme.colorScheme
    val contentColor = if (enabled) colors.onSurface else colors.onSurface.copy(alpha = 0.38f)
    Column(modifier.fillMaxWidth().semantics { if (!enabled) disabled() }) {
        BaseFormFieldFrame(textResources.getString(cn.turingzy.ledger.R.string.ui_card_design), readOnly = !enabled, onClick = if (enabled) onClick else null) {
            Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = contentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(MaterialSymbols.ChevronRight, null, Modifier.size(LedgerStyle.icons.default), tint = contentColor)
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
    AccountFormTextField(
        label = androidx.compose.ui.res.stringResource(cn.turingzy.ledger.R.string.ui_last_digits),
        value = value,
        onValueChange = { if (it.length <= 4 && it.all { digit -> digit in '0'..'9' }) onValueChange(it) },
        modifier = modifier,
        keyboardType = KeyboardType.Number,
    )
}
