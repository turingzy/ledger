package cn.turingzy.ledger.ui.feature.ledger

import cn.turingzy.ledger.ui.component.MaterialSymbols
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BuiltInKeypad(
    hasPendingCalculation: Boolean,
    onNumberClick: (String) -> Unit,
    onDotClick: () -> Unit,
    onOperatorClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionEnabled: Boolean = true,
) {
    val keyHeight = 48.dp
    val spacing = 8.dp

    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
        ) {
            KeypadButton(text = "1", onClick = { onNumberClick("1") }, modifier = Modifier.weight(1f).height(keyHeight))
            KeypadButton(text = "2", onClick = { onNumberClick("2") }, modifier = Modifier.weight(1f).height(keyHeight))
            KeypadButton(text = "3", onClick = { onNumberClick("3") }, modifier = Modifier.weight(1f).height(keyHeight))
            KeypadButton(text = "+", onClick = { onOperatorClick("+") }, modifier = Modifier.weight(1f).height(keyHeight))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
        ) {
            KeypadButton(text = "4", onClick = { onNumberClick("4") }, modifier = Modifier.weight(1f).height(keyHeight))
            KeypadButton(text = "5", onClick = { onNumberClick("5") }, modifier = Modifier.weight(1f).height(keyHeight))
            KeypadButton(text = "6", onClick = { onNumberClick("6") }, modifier = Modifier.weight(1f).height(keyHeight))
            KeypadButton(text = "-", onClick = { onOperatorClick("-") }, modifier = Modifier.weight(1f).height(keyHeight))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
        ) {
            Column(
                modifier = Modifier.weight(3f),
                verticalArrangement = Arrangement.spacedBy(spacing),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    KeypadButton(text = "7", onClick = { onNumberClick("7") }, modifier = Modifier.weight(1f).height(keyHeight))
                    KeypadButton(text = "8", onClick = { onNumberClick("8") }, modifier = Modifier.weight(1f).height(keyHeight))
                    KeypadButton(text = "9", onClick = { onNumberClick("9") }, modifier = Modifier.weight(1f).height(keyHeight))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    KeypadButton(text = ".", onClick = onDotClick, modifier = Modifier.weight(1f).height(keyHeight))
                    KeypadButton(text = "0", onClick = { onNumberClick("0") }, modifier = Modifier.weight(1f).height(keyHeight))
                    KeypadIconButton(
                        icon = MaterialSymbols.Backspace,
                        onClick = onBackspaceClick,
                        modifier = Modifier.weight(1f).height(keyHeight),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(keyHeight * 2 + spacing)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = if (actionEnabled) 1f else 0.38f))
                    .clickable(enabled = actionEnabled, onClick = onActionClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (hasPendingCalculation) "=" else "完成",
                    fontSize = if (hasPendingCalculation) 24.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiary,
                )
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
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, tween(90), label = "keyPress")
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .clickable(interactionSource = interactionSource, indication = androidx.compose.material3.ripple(), role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
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
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, tween(90), label = "keyPress")
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .clickable(interactionSource = interactionSource, indication = androidx.compose.material3.ripple(), role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "删除最后一位",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp),
        )
    }
}
