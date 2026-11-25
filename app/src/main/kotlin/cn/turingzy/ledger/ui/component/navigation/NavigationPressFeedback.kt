package cn.turingzy.ledger.ui.component.navigation

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import cn.turingzy.ledger.ui.interaction.rememberPressFeedbackProgress

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
    val progress = rememberPressFeedbackProgress(interactionSource, enabled)
    val color = MaterialTheme.colorScheme.onSurface
    return drawBehind {
        if (progress.value > 0f) drawRect(color.copy(alpha = progress.value * 0.05f))
    }
}
