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
import cn.turingzy.ledger.ui.theme.LedgerStyle

/**
 * 表单单行文本输入框组件。
 *
 * 基于通用表单边框 [FormFieldFrameBase]，支持输入焦点跟踪、占位文本、后缀文案、只读状态及软键盘行为配置。
 *
 * @param label 表单项的标题标签。
 * @param value 当前输入的文本内容。
 * @param onValueChange 文本变化时的回调函数。
 * @param modifier 应用于该组件的修饰符。
 * @param placeholder 当文本为空时显示的占位符。
 * @param suffix 文本框右侧显示的后缀文案（如单位、币种等）。
 * @param error 错误提示信息，非空时边框展示错误样式。
 * @param readOnly 是否为只读状态。只读时不可聚焦输入且呈半透明禁用文本色。
 * @param keyboardType 软键盘类型，默认为 [KeyboardType.Text]。
 * @param imeAction 软键盘动作键类型，默认为 [ImeAction.Next]（切换到下一个焦点）。
 */
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
    val supportingColor = LedgerStyle.colors.textSupporting
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
                    if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = supportingColor)
                    inner()
                }
            },
        )
        if (suffix != null) Text(suffix, style = MaterialTheme.typography.bodySmall, color = if (readOnly) textColor else supportingColor)
    }
}
