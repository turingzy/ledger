package cn.turingzy.ledger.ui.component.input

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import cn.turingzy.ledger.ui.component.base.FormFieldFrameBase

/** Edits a single value using the shared account-form field frame. */
@Composable
internal fun FormTextField(
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
    FormFieldFrameBase(
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
