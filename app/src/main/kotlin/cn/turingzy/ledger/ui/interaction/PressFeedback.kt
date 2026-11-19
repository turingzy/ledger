package cn.turingzy.ledger.ui.interaction

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import cn.turingzy.ledger.ui.theme.LedgerInteractionStyle
import cn.turingzy.ledger.ui.theme.LedgerStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

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
                        delay(LedgerInteractionStyle.quickTapHoldMillis.milliseconds)
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
