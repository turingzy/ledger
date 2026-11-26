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
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import cn.turingzy.ledger.R
import cn.turingzy.ledger.ui.component.base.FormFieldFrameBase
import cn.turingzy.ledger.ui.theme.LedgerStyle

/** 单行输入状态样式；调用方保留自己的容器、占位布局和键盘行为。 */
@Composable
internal fun FormInputBase(
    value: String,
    onValueChange: (String) -> Unit,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    visualTransformation: VisualTransformation = VisualTransformation.None,
    decorationBox: @Composable (@Composable () -> Unit) -> Unit = { it() },
) {
    val colors = MaterialTheme.colorScheme
    val focused by interactionSource.collectIsFocusedAsState()
    val normalColor = textStyle.color.takeOrElse { colors.onSurface }
    val inputColor = when {
        !enabled -> normalColor.copy(alpha = LedgerStyle.opacity.disabled)
        isError -> colors.error
        focused && !readOnly -> colors.onSurface
        else -> normalColor
    }
    BasicTextField(
        value = value, onValueChange = onValueChange, modifier = modifier,
        enabled = enabled, readOnly = readOnly, singleLine = true,
        textStyle = textStyle.copy(color = inputColor),
        cursorBrush = SolidColor(if (enabled && isError) colors.error else colors.primary),
        keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        interactionSource = interactionSource, decorationBox = decorationBox,
    )
}

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
 * @param readOnly 是否仅显示信息，允许选择和复制文本。
 * @param enabled 字段是否可用。
 * @param isError 是否使用错误色。跨字段校验可在组下显示统一提示。
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
    enabled: Boolean = true,
    isError: Boolean = error != null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val supportingColor = LedgerStyle.colors.textSupporting.copy(alpha = if (enabled) 1f else LedgerStyle.opacity.disabled)
    val readOnlyDescription = if (readOnly) stringResource(R.string.ui_read_only) else null
    FormFieldFrameBase(
        label, modifier, focused = focused && !readOnly, enabled = enabled, error = error, isError = isError,
        onClick = if (enabled && !readOnly) ({ focusRequester.requestFocus() }) else null,
    ) {
        FormInputBase(
            value = value, onValueChange = onValueChange, enabled = enabled, readOnly = readOnly,
            interactionSource = source, isError = isError, textStyle = MaterialTheme.typography.bodyMedium,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            visualTransformation = visualTransformation,
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                onDone = { focusManager.clearFocus() },
            ),
            modifier = Modifier.weight(1f).focusRequester(focusRequester).semantics {
                contentDescription = label
                if (readOnlyDescription != null) stateDescription = readOnlyDescription
            },
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = supportingColor)
                    inner()
                }
            },
        )
        if (suffix != null) Text(suffix, style = MaterialTheme.typography.bodySmall, color = supportingColor)
    }
}
