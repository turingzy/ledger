package cn.turingzy.ledger.ui.feature.ledger

import cn.turingzy.ledger.ui.component.MaterialSymbols
import androidx.compose.foundation.LocalIndication
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    Layout(
        modifier = modifier.fillMaxWidth(),
        content = {
            for (digit in 1..3) KeypadButton(digit.toString(), { onNumberClick(digit.toString()) }, Modifier.testTag("keypad_$digit"))
            KeypadButton("+ ×", { onOperatorClick("+") }, Modifier.testTag("keypad_add"))
            for (digit in 4..6) KeypadButton(digit.toString(), { onNumberClick(digit.toString()) }, Modifier.testTag("keypad_$digit"))
            KeypadButton("− ÷", { onOperatorClick("-") }, Modifier.testTag("keypad_subtract"))
            for (digit in 7..9) KeypadButton(digit.toString(), { onNumberClick(digit.toString()) }, Modifier.testTag("keypad_$digit"))
            KeypadIconButton(MaterialSymbols.Backspace, onBackspaceClick, Modifier.testTag("keypad_backspace"))
            KeypadButton("·", onDotClick, Modifier.testTag("keypad_dot").semantics { contentDescription = "小数点" })
            KeypadButton("0", { onNumberClick("0") }, Modifier.testTag("keypad_0"))
            KeypadButton("AC", onClearClick, Modifier.testTag("keypad_clear"))
            Box(
                Modifier.testTag("keypad_action").clip(RoundedCornerShape(12.dp))
                    .background(actionColor).clickable(enabled = actionEnabled, onClick = onActionClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (hasPendingCalculation) "=" else "完成",
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

@Composable
fun KeypadButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = when (text) { "+ ×", "− ÷" -> 22.sp; "·" -> 26.sp; "AC" -> 18.sp; else -> 20.sp },
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun KeypadIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "删除最后一位",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(26.dp),
        )
    }
}
