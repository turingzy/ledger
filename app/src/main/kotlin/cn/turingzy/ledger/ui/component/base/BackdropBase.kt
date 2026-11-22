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

/**
 * 拦截背后的触摸事件，使当前组件成为点击目标，同时保持自身内容与父级手势有效。
 */
internal fun Modifier.blockTouchesBehind(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope { while (true) awaitPointerEvent() }
}

/**
 * 背景图层状态，持有录制的绘制图层及在根布局中的全局坐标。
 *
 * @property layer 用于捕获背景绘制内容的图形图层。
 */
internal class BackdropStateBase(val layer: GraphicsLayer) {
    /** 组件在根布局中的全局坐标原点。 */
    var origin by mutableStateOf(Offset.Zero)
}

/**
 * 记住并创建一个 [BackdropStateBase] 实例。
 */
@Composable
internal fun rememberBackdropStateBaseBase(): BackdropStateBase {
    val layer = rememberGraphicsLayer()
    return remember(layer) { BackdropStateBase(layer) }
}

/**
 * 将当前组件的内容录制到 [BackdropStateBase] 的图形图层中，以供背景模糊组件使用。
 *
 * @param state 用于保存录制内容的背景状态。
 */
internal fun Modifier.recordBackdropBase(state: BackdropStateBase): Modifier =
    onGloballyPositioned { state.origin = it.positionInRoot() }
        .drawWithContent {
            state.layer.record { this@drawWithContent.drawContent() }
            drawLayer(state.layer)
        }

/**
 * 绘制磨砂玻璃（毛玻璃）背景效果的修饰符。
 *
 * 将 [BackdropStateBase] 录制的背景图层进行模糊处理并平移对齐，同时叠加半透明底色，支持可选的底部渐隐淡出效果。
 *
 * @param state 录制了背景内容的背景状态；如果为 null 则只绘制底色。
 * @param tint 叠加在模糊层之上的背景色调。
 * @param blurRadius 模糊半径。
 * @param fadeBottom 底部渐变透明淡出的高度；默认值为 0.dp（不淡出）。
 */
@Composable
internal fun Modifier.frostedBackdropBase(
    state: BackdropStateBase?,
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
