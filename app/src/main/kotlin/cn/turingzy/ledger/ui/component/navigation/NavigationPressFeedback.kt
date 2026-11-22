package cn.turingzy.ledger.ui.component.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind

/**
 * 导航项按压视觉反馈修饰符。
 *
 * 监听 [interactionSource] 的按压状态变化，在组件背景平滑绘制微弱半透明遮罩，提供轻量按压反馈。
 *
 * @param interactionSource 交互源，用于监听组件的按压事件。
 * @param enabled 是否启用按压视觉反馈。
 * @return 附加了按压背景动效的 [Modifier]。
 */
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
