package cn.turingzy.ledger.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind

@Composable
internal fun Modifier.navigationPressFeedback(
    interactionSource: InteractionSource,
    enabled: Boolean = true,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val alpha by animateFloatAsState(
        targetValue = if (enabled && pressed) 0.05f else 0f,
        animationSpec = tween(durationMillis = if (pressed) 70 else 140),
        label = "NavigationPressAlpha",
    )
    val color = MaterialTheme.colorScheme.onSurface
    return drawBehind { drawRect(color.copy(alpha = alpha)) }
}
