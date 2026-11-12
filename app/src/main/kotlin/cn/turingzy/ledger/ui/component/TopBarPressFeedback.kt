package cn.turingzy.ledger.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable
internal fun Modifier.topBarPressFeedback(
    interactionSource: MutableInteractionSource,
    enabled: Boolean,
): Modifier {
    val alpha = remember { Animatable(0f) }
    val color = MaterialTheme.colorScheme.onSurface
    LaunchedEffect(interactionSource, enabled) {
        alpha.snapTo(0f)
        if (!enabled) return@LaunchedEffect
        interactionSource.interactions.collectLatest { interaction ->
            when (interaction) {
                is PressInteraction.Press -> alpha.animateTo(0.12f, tween(70))
                is PressInteraction.Release -> {
                    // A quick tap must remain visible even when down and up arrive in one frame.
                    alpha.animateTo(0.12f, tween(40))
                    delay(40)
                    alpha.animateTo(0f, tween(140))
                }
                is PressInteraction.Cancel -> alpha.animateTo(0f, tween(100))
            }
        }
    }
    return drawBehind { drawRect(color.copy(alpha = alpha.value)) }
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
