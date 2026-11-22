package cn.turingzy.ledger.ui.feature.analytics.component

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

/**
 * 记录图表选中交互边界及取消选中回调的载体。
 *
 * @property dismiss 取消选中的回调操作。
 */
private class ChartSelectionBoundary(val dismiss: () -> Unit) {
    var bounds: Rect = Rect.Zero
}

/**
 * 图表交互协调器，维护活跃的图表边界集合及宿主在根视图中的坐标原点。
 */
private class ChartInteractions {
    val boundaries = mutableSetOf<ChartSelectionBoundary>()
    var origin = Offset.Zero
}

/**
 * 用于在组件树中向下传递 [ChartInteractions] 实例的 CompositionLocal。
 */
private val LocalChartInteractions = staticCompositionLocalOf<ChartInteractions?> { null }

/**
 * 图表交互宿主容器。
 *
 * 监听全局触摸按下事件，当用户点击图表范围之外时，自动调用图表的取消选中回调以关闭高亮或提示框，
 * 且不影响原本的手势消费与列表滚动。
 *
 * @param modifier 修饰符。
 * @param content 子可组合项内容。
 */
@Composable
internal fun AnalyticsChartInteractionHost(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val interactions = remember { ChartInteractions() }
    CompositionLocalProvider(LocalChartInteractions provides interactions) {
        Box(modifier.onGloballyPositioned { interactions.origin = it.positionInRoot() }
            .pointerInput(interactions) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        event.changes.firstOrNull { it.changedToDownIgnoreConsumed() }?.let { change ->
                            val position = change.position + interactions.origin
                            // Observe before child gestures consume the event, without interrupting scrolling or taps.
                            interactions.boundaries.filterNot { it.bounds.contains(position) }.forEach { it.dismiss() }
                        }
                    }
                }
            }) { content() }
    }
}

/**
 * 为图表组件挂载选中边界检测的 Modifier 扩展。
 *
 * 将当前组件在根视图中的布局边界注册至 [LocalChartInteractions]，当点击落在边界外时触发 [onDismiss]。
 *
 * @param onDismiss 点击外部触发的取消选中回调。
 */
@Composable
internal fun Modifier.chartSelectionBoundary(onDismiss: () -> Unit): Modifier {
    val interactions = LocalChartInteractions.current
    val dismiss by rememberUpdatedState(onDismiss)
    val boundary = remember { ChartSelectionBoundary { dismiss() } }
    DisposableEffect(interactions, boundary) {
        interactions?.boundaries?.add(boundary)
        onDispose { interactions?.boundaries?.remove(boundary) }
    }
    return onGloballyPositioned { boundary.bounds = it.boundsInRoot() }
}
