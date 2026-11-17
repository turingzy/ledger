package cn.turingzy.ledger.ui.interaction

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import cn.turingzy.ledger.ui.theme.LedgerInteractionStyle
import cn.turingzy.ledger.ui.theme.LedgerStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

enum class UiInteractionKind { Action, Navigation, Selection }

internal val LocalUiInteractionClock = staticCompositionLocalOf<() -> Long> {
    { System.nanoTime() / 1_000_000L }
}

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
    onClick: () -> Unit,
): Modifier = clickable(
    enabled = enabled,
    role = Role.Button,
    interactionSource = remember { MutableInteractionSource() },
    indication = rememberUiIndication(),
    onClick = rememberUiAction(onClick, kind),
)

internal class ClickGate(
    private val intervalMillis: Long = LedgerInteractionStyle.clickDebounceMillis,
    private val clockMillis: () -> Long = { System.nanoTime() / 1_000_000L },
) {
    private var lastAccepted: Long? = null

    init {
        require(intervalMillis >= 0L)
    }

    fun tryAccept(): Boolean {
        val now = clockMillis()
        val previous = lastAccepted
        if (previous != null && now >= previous && now - previous < intervalMillis) return false
        lastAccepted = now
        return true
    }
}

@Composable
fun rememberUiIndication(
    color: Color = LedgerStyle.colors.textPrimary.copy(alpha = LedgerStyle.colors.textPrimary.alpha * LedgerStyle.opacity.pressed),
): IndicationNodeFactory = remember(color) { UiPressFeedback(color) }

private data class UiPressFeedback(val color: Color) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = FeedbackNode(interactionSource, color)
}

private class FeedbackNode(private val source: InteractionSource, private val color: Color) : Modifier.Node(), DrawModifierNode {
    private val alpha = Animatable(0f)

    override fun onAttach() {
        coroutineScope.launch {
            alpha.snapTo(0f)
            val presses = mutableSetOf<PressInteraction.Press>()
            source.interactions.filterIsInstance<PressInteraction>().collectLatest { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> presses.add(interaction)
                    is PressInteraction.Release -> presses.remove(interaction.press)
                    is PressInteraction.Cancel -> presses.remove(interaction.press)
                }
                when {
                    presses.isNotEmpty() -> alpha.animateTo(1f, tween(LedgerInteractionStyle.pressEnterMillis))
                    interaction is PressInteraction.Release -> {
                        // A quick tap must remain visible even if press and release share a frame.
                        alpha.animateTo(1f, tween(LedgerInteractionStyle.quickTapEnterMillis))
                        delay(LedgerInteractionStyle.quickTapHoldMillis)
                        alpha.animateTo(0f, tween(LedgerInteractionStyle.pressExitMillis))
                    }
                    else -> alpha.animateTo(0f, tween(LedgerInteractionStyle.pressCancelMillis))
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (alpha.value > 0f) drawRect(color.copy(alpha = color.alpha * alpha.value))
    }
}
