package cn.turingzy.ledger.ui.interaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role

/**
 * UI 交互类型枚举。
 */
enum class UiInteractionKind {
    /** 普通操作（如确认、取消、提交） */
    Action,
    /** 导航跳转（如页面切换、打开详情页） */
    Navigation,
    /** 选中切换（如单选框、标签页切换），触发触感震动反馈 */
    Selection,
}

/**
 * 记住并返回一个经过防抖处理的无参 UI 操作回调。
 *
 * @param onAction 实际执行的业务逻辑回调
 * @param kind 交互行为类型，默认为 [UiInteractionKind.Action]
 * @return 防抖包装后的点击回调函数
 */
@Composable
fun rememberUiAction(
    onAction: () -> Unit,
    kind: UiInteractionKind = UiInteractionKind.Action,
): () -> Unit {
    val event = rememberUiEvent<Unit>({ onAction() }, kind)
    return remember(event) { { event(Unit) } }
}

/**
 * 记住并返回一个经过防抖处理的单参数 UI 事件回调。
 *
 * 当交互类型为 [UiInteractionKind.Selection] 时会自动触发短促的触感反馈。
 *
 * @param T 事件参数类型
 * @param onEvent 实际执行的带参事件回调
 * @param kind 交互行为类型，默认为 [UiInteractionKind.Action]
 * @return 防抖包装后的事件回调函数
 */
@Composable
fun <T> rememberUiEvent(
    onEvent: (T) -> Unit,
    kind: UiInteractionKind = UiInteractionKind.Action,
): (T) -> Unit {
    val clock = LocalUiInteractionClock.current
    val gate = remember(clock) { ClickGate(clockMillis = clock) }
    val currentEvent = rememberUpdatedState(onEvent)
    val currentKind = rememberUpdatedState(kind)
    val currentHaptic = rememberUpdatedState(LocalHapticFeedback.current)
    return remember(gate) {
        { value ->
            if (gate.tryAccept()) {
                if (currentKind.value == UiInteractionKind.Selection) {
                    currentHaptic.value.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                currentEvent.value(value)
            }
        }
    }
}

/**
 * 应用统一的可选中修饰符。
 *
 * 集成 [rememberUiIndication] 按压视觉效果、点击防抖门限以及选中态切换时的震动反馈。
 *
 * @param selected 当前是否处于选中状态
 * @param enabled 是否允许交互
 * @param onClick 点击事件回调
 * @return 带有统一选中特性的 Modifier
 */
@Composable
fun Modifier.uiSelectable(
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = selectable(
    selected = selected,
    enabled = enabled,
    interactionSource = remember { MutableInteractionSource() },
    indication = rememberUiIndication(),
    onClick = rememberUiAction(onClick, if (selected) UiInteractionKind.Action else UiInteractionKind.Selection),
)

/**
 * 应用统一的可点击修饰符。
 *
 * 预设 [Role.Button] 无障碍角色、统一的按压反馈 [rememberUiIndication] 以及基于时间的点击防抖处理。
 *
 * @param enabled 是否允许点击
 * @param kind 交互类型（操作、导航或选中）
 * @param showPressFeedback 是否展示按压高亮视觉反馈
 * @param onClick 点击回调函数
 * @return 带有统一点按特性的 Modifier
 */
@Composable
fun Modifier.uiClickable(
    enabled: Boolean = true,
    kind: UiInteractionKind = UiInteractionKind.Action,
    showPressFeedback: Boolean = true,
    onClick: () -> Unit,
): Modifier = clickable(
    enabled = enabled,
    role = Role.Button,
    interactionSource = remember { MutableInteractionSource() },
    indication = if (showPressFeedback) rememberUiIndication() else null,
    onClick = rememberUiAction(onClick, kind),
)
