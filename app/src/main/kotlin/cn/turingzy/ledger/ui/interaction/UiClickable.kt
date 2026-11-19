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

enum class UiInteractionKind { Action, Navigation, Selection }

@Composable
fun rememberUiAction(
    onAction: () -> Unit,
    kind: UiInteractionKind = UiInteractionKind.Action,
): () -> Unit {
    val event = rememberUiEvent<Unit>({ onAction() }, kind)
    return remember(event) { { event(Unit) } }
}

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
