package cn.turingzy.ledger.ui.feature.bill.sheet

import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.ui.interaction.rememberSystemClickFeedback
import cn.turingzy.ledger.ui.component.MaterialSymbols
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 记账内置数字键盘组件。
 *
 * 采用 4 列 4 行自定义网格布局，包含数字 0-9、小数点、加/乘连击键、减/除连击键、
 * 退格删除键、AC 全清键，以及右下角的“完成/=”操作按钮。
 *
 * @param hasPendingCalculation 是否存在未求值的运算符表达式（为 true 时右下角按钮显示 "="，否则显示“完成”）。
 * @param onNumberClick 点击数字键回调（传入数字字符）。
 * @param onDotClick 点击小数点键回调。
 * @param onOperatorClick 点击运算符键回调（传入 "+" 或 "-"）。
 * @param onBackspaceClick 点击退格删除键回调。
 * @param onClearClick 点击 AC 清空键回调。
 * @param onActionClick 点击操作按钮（"=" 求值或“完成”提交）回调。
 * @param modifier 外部修饰符。
 * @param actionEnabled 操作按钮是否可用。
 * @param keyHeight 单个按键的高度。
 * @param actionColor 操作按钮背景色。
 * @param actionContentColor 操作按钮内容文字颜色。
 */
@Composable
fun BuiltInKeypad(
    hasPendingCalculation: Boolean,
    onNumberClick: (String) -> Unit,
    onDotClick: () -> Unit,
    onOperatorClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onClearClick: () -> Unit,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionEnabled: Boolean = true,
    keyHeight: Dp = 48.dp,
    actionColor: Color = MaterialTheme.colorScheme.tertiary,
    actionContentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    Layout(
        modifier = modifier.fillMaxWidth(),
        content = {
            for (digit in 1..3) KeypadButton(digit.toString(), { onNumberClick(digit.toString()) }, Modifier.testTag("keypad_$digit"))
            KeypadButton("+×", { onOperatorClick("+") }, Modifier.testTag("keypad_add"))
            for (digit in 4..6) KeypadButton(digit.toString(), { onNumberClick(digit.toString()) }, Modifier.testTag("keypad_$digit"))
            KeypadButton("−÷", { onOperatorClick("-") }, Modifier.testTag("keypad_subtract"))
            for (digit in 7..9) KeypadButton(digit.toString(), { onNumberClick(digit.toString()) }, Modifier.testTag("keypad_$digit"))
            KeypadIconButton(MaterialSymbols.Backspace, onBackspaceClick, Modifier.testTag("keypad_backspace"))
            KeypadButton("·", onDotClick, Modifier.testTag("keypad_dot").semantics { contentDescription = textResources.getString(cn.turingzy.ledger.R.string.ui_decimal_point) })
            KeypadButton("0", { onNumberClick("0") }, Modifier.testTag("keypad_0"))
            KeypadButton("AC", onClearClick, Modifier.testTag("keypad_clear"))
            Box(
                Modifier.testTag("keypad_action").clip(LedgerStyle.shapes.field)
                    .background(actionColor).keypadClickable(enabled = actionEnabled, onClick = onActionClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (hasPendingCalculation) "=" else textResources.getString(cn.turingzy.ledger.R.string.save),
                    fontSize = if (hasPendingCalculation) 20.sp else 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = actionContentColor.copy(alpha = if (actionEnabled) 1f else 0.55f),
                )
            }
        },
    ) { measurables, constraints ->
        val spacing = 8.dp.roundToPx()
        val rowHeight = keyHeight.roundToPx()
        val availableWidth = (constraints.maxWidth - spacing * 3).coerceAtLeast(0)
        val placeables = measurables.mapIndexed { index, measurable ->
            val column = index % 4
            val width = (column + 1) * availableWidth / 4 - column * availableWidth / 4
            measurable.measure(Constraints.fixed(width, rowHeight))
        }
        layout(constraints.maxWidth, constraints.constrainHeight(rowHeight * 4 + spacing * 3)) {
            placeables.forEachIndexed { index, placeable ->
                val row = index / 4
                val column = index % 4
                placeable.placeRelative(column * availableWidth / 4 + column * spacing, row * (rowHeight + spacing))
            }
        }
    }
}

/**
 * 键盘文本按键组件。
 *
 * @param text 按键上展示的字符（如数字、运算符、AC 等）。
 * @param onClick 点击事件回调。
 * @param modifier 外部修饰符。
 */
@Composable
fun KeypadButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(LedgerStyle.shapes.field)
            .background(MaterialTheme.colorScheme.surface)
            .keypadClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = when (text) { "+×", "−÷" -> 22.sp; "·" -> 26.sp; "AC" -> 18.sp; else -> 20.sp },
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * 键盘图标按键组件（例如退格删除）。
 *
 * @param icon 按键图标矢量图 [ImageVector]。
 * @param onClick 点击事件回调。
 * @param modifier 外部修饰符。
 */
@Composable
fun KeypadIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    Box(
        modifier = modifier
            .clip(LedgerStyle.shapes.field)
            .background(MaterialTheme.colorScheme.surface)
            .keypadClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = textResources.getString(cn.turingzy.ledger.R.string.ui_delete_last_digit),
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(LedgerStyle.icons.default),
        )
    }
}

/** Uses the same system click feedback as the bottom navigation. */
@Composable
private fun Modifier.keypadClickable(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val clickFeedback = rememberSystemClickFeedback()
    return clickable(enabled = enabled, role = Role.Button) {
        clickFeedback()
        onClick()
    }
}
