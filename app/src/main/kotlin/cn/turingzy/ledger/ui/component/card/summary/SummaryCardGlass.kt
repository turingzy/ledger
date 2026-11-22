package cn.turingzy.ledger.ui.component.card.summary

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.model.settings.LedgerCardAppearance

/**
 * 摘要卡片毛玻璃质感底栏背景组件。
 *
 * 在卡片底部区域渲染半透明磨砂玻璃视觉效果：
 * - 优先采用预模糊的高质量位图 [blurredBackdrop]；若不存在则使用 [Modifier.blur] 降级模糊 [background]。
 * - 叠加动态渐变暗色蒙层以提升上层文字可读性。
 * - 透明度由 [appearance] 中的覆盖层不透明度实时驱动。
 *
 * @param appearance 卡片外观配置，决定毛玻璃不透明度。
 * @param background 原始背景图绘制器（作为动态模糊降级源）。
 * @param blurredBackdrop 预模糊背景位图；若有效则跳过昂贵的实时模糊计算。
 * @param modifier 修饰符。
 * @param showDivider 是否在毛玻璃顶缘绘制微弱的浅色分割线。
 */
@Composable
internal fun SummaryCardGlass(
    appearance: LedgerCardAppearance,
    background: Painter,
    blurredBackdrop: ImageBitmap?,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val opacity = appearance.overlayOpacity
    Box(modifier.fillMaxSize().graphicsLayer { alpha = opacity }) {
        if (blurredBackdrop != null) {
            Image(
                bitmap = blurredBackdrop,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = BiasAlignment(0f, 0.7f),
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Image(background, null, Modifier.fillMaxSize().blur(20.dp), contentScale = ContentScale.Crop)
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        colors.scrim.copy(alpha = 0.35f + 0.65f * opacity),
                        colors.scrim.copy(alpha = 0.55f + 0.45f * opacity),
                    ),
                ),
            ),
        )
        if (showDivider) {
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.onPrimary.copy(alpha = 0.32f)))
        }
    }
}
