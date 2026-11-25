package cn.turingzy.ledger.ui.component.card.summary

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import cn.turingzy.ledger.ui.interaction.rememberPressFeedbackProgress
import cn.turingzy.ledger.ui.theme.LedgerStyle

/**
 * 摘要卡片专用的紧凑型圆形图标按钮。
 *
 * 具有半透明圆形背景容器，常用于摘要卡片上的金额显隐切换或翻转等操作。
 *
 * @param imageVector 图标矢量图。
 * @param contentDescription 辅助功能描述。
 * @param onClick 点击事件回调。
 * @param modifier 修饰符。
 * @param enabled 是否可点击交互。
 * @param containerColor 圆形背景填充颜色。
 * @param contentColor 图标颜色。
 * @param showPressFeedback 是否展示水波纹点击反馈。
 * @param iconSize 图标显示尺寸。
 */
@Composable
internal fun SummaryCardIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.scrim.copy(alpha = LedgerStyle.opacity.cardIconBackground),
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    showPressFeedback: Boolean = true,
    iconSize: Dp = LedgerStyle.icons.default,
) {
    Box(
        modifier.size(LedgerStyle.dimensions.compactIconButton).clip(CircleShape)
            .background(containerColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = if (showPressFeedback) LocalIndication.current else null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = contentColor.copy(alpha = contentColor.alpha * if (enabled) 1f else LedgerStyle.opacity.disabled),
            modifier = Modifier.size(iconSize),
        )
    }
}

/**
 * 顶部工具栏使用的轻量级圆形图标按钮。
 *
 * 采用自定义的非侵入式按压高光背景动效替代默认的水波纹反馈。
 *
 * @param imageVector 图标矢量图。
 * @param contentDescription 辅助功能描述。
 * @param onClick 点击事件回调。
 * @param modifier 修饰符。
 * @param showPressFeedback 是否启用按下淡入淡出反馈。
 * @param iconSize 图标显示尺寸。
 */
@Composable
internal fun TopBarIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showPressFeedback: Boolean = true,
    iconSize: Dp = LedgerStyle.icons.default,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier.size(LedgerStyle.dimensions.compactIconButton).clip(CircleShape)
            .topBarPressFeedback(interactionSource, enabled = showPressFeedback)
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector, contentDescription, Modifier.size(iconSize), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * 为顶部工具栏按钮添加细腻的快速按压高光背景动效。
 *
 * 监听 [PointerEventPass.Initial] 通道捕获手势事件，确保不阻断点击或滑动事件的分发。
 *
 * @param interactionSource 手势交互数据源。
 * @param enabled 是否开启反馈效果。
 * @param color 按压时的高光填充色。
 */
@Composable
private fun Modifier.topBarPressFeedback(
    interactionSource: MutableInteractionSource,
    enabled: Boolean,
    color: Color = MaterialTheme.colorScheme.onSurface,
): Modifier {
    val progress = rememberPressFeedbackProgress(interactionSource, enabled)
    val pressedAlpha = LedgerStyle.opacity.toolbarPressed
    return drawBehind {
        if (progress.value > 0f) drawRect(color.copy(alpha = progress.value * pressedAlpha))
    }
        .pointerInput(interactionSource, enabled) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                // Observe the initial pass without consuming events needed by clicks or scrolling.
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val press = PressInteraction.Press(down.position)
                interactionSource.tryEmit(press)
                var finished = false
                try {
                    val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    interactionSource.tryEmit(
                        if (up == null) PressInteraction.Cancel(press) else PressInteraction.Release(press),
                    )
                    finished = true
                } finally {
                    if (!finished) interactionSource.tryEmit(PressInteraction.Cancel(press))
                }
            }
        }
}
