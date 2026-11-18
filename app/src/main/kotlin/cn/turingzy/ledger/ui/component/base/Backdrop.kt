package cn.turingzy.ledger.ui.component.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp

// Make the overlay the hit target while leaving its own buttons and parent gestures active.
internal fun Modifier.blockTouchesBehind(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope { while (true) awaitPointerEvent() }
}

internal class BackdropState(val layer: GraphicsLayer) {
    var origin by mutableStateOf(Offset.Zero)
}

@Composable
internal fun rememberBackdropState(): BackdropState {
    val layer = rememberGraphicsLayer()
    return remember(layer) { BackdropState(layer) }
}

internal fun Modifier.recordBackdrop(state: BackdropState): Modifier =
    onGloballyPositioned { state.origin = it.positionInRoot() }
        .drawWithContent {
            state.layer.record { this@drawWithContent.drawContent() }
            drawLayer(state.layer)
        }

@Composable
internal fun Modifier.frostedBackdrop(
    state: BackdropState?,
    tint: Color,
    blurRadius: Dp,
    fadeBottom: Dp = 0.dp,
): Modifier {
    val blurred = rememberGraphicsLayer()
    var origin by remember { mutableStateOf(Offset.Zero) }
    return onGloballyPositioned { origin = it.positionInRoot() }
        .then(if (fadeBottom > 0.dp) Modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen } else Modifier)
        .drawWithContent {
            if (state != null) {
                val displacement = state.origin - origin
                blurred.renderEffect = BlurEffect(blurRadius.toPx(), blurRadius.toPx())
                val padding = blurRadius.roundToPx()
                // Sample beyond the visible bounds so blur does not fade into transparent edges.
                blurred.record(size = IntSize(size.width.toInt() + padding * 2, size.height.toInt() + padding * 2)) {
                    translate(displacement.x + padding, displacement.y + padding) { drawLayer(state.layer) }
                }
                translate(-padding.toFloat(), -padding.toFloat()) { drawLayer(blurred) }
            }
            drawRect(tint)
            if (fadeBottom > 0.dp) {
                val fadeStart = ((size.height - fadeBottom.toPx()) / size.height).coerceIn(0f, 1f)
                drawRect(
                    Brush.verticalGradient(0f to Color.Black, fadeStart to Color.Black, 1f to Color.Transparent),
                    blendMode = BlendMode.DstIn,
                )
            }
            drawContent()
        }
}
