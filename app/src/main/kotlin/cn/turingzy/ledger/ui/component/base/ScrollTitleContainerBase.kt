package cn.turingzy.ledger.ui.component.base

import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.card.summary.TopBarIconButton
import cn.turingzy.ledger.ui.theme.LedgerStyle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

/**
 * 伴随页面滚动的吸顶标题容器组件。
 *
 * 当内容垂直滚动超过指定阈值时，平滑淡入展示半透明磨砂毛玻璃样式的固定标题栏，
 * 动态调整标题字体大小并提供点击返回顶部的快捷操作。
 *
 * @param title 顶部标题文本。
 * @param scrollOffset 获取当前垂直滚动偏移量（像素）的函数。
 * @param onScrollToTop 点击返回顶部按钮时执行的挂起动作。
 * @param modifier 应用于根容器的修饰符。
 * @param actions 始终可用的顶栏右侧操作。
 * @param content 页面主体内容插槽，接收需要附加到可滚动内容上的 [Modifier]（用于录制毛玻璃背景图层）。
 */
@Composable
internal fun ScrollTitleContainerBase(
    title: String,
    scrollOffset: () -> Int,
    onScrollToTop: suspend () -> Unit,
    modifier: Modifier = Modifier,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable (Modifier) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val backdrop = rememberBackdropStateBaseBase()
    val density = LocalDensity.current
    val surface = MaterialTheme.colorScheme.background
    Box(modifier.fillMaxSize()) {
        content(Modifier.recordBackdropBase(backdrop))
        val offset = with(density) { scrollOffset().toDp().value }
        val progress = ((offset - 20f) / 36f).coerceIn(0f, 1f)
        if (progress > 0f) {
            Box(
                Modifier.fillMaxWidth().height(LedgerStyle.dimensions.toolbarHeight).clipToBounds()
                    .blockTouchesBehind()
                    .graphicsLayer {
                        alpha = progress
                    }
                    .frostedBackdropBase(
                        backdrop, surface.copy(alpha = LedgerStyle.opacity.topBarScrolled),
                        blurRadius = LedgerStyle.dimensions.topBarBlur,
                        fadeBottom = LedgerStyle.dimensions.primaryTopBarFade,
                    ),
            )
        }
        if (progress > 0f || actions != null) {
            val topPadding = if (actions == null) 0f else 1f - progress
            Row(
                Modifier.fillMaxWidth()
                    .padding(top = LedgerStyle.spacing.extraSmall * topPadding)
                    .height(LedgerStyle.dimensions.toolbarHeight)
                    .padding(horizontal = LedgerStyle.spacing.pageHorizontal),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (progress > 0f) {
                    Text(
                        title,
                        modifier = Modifier.weight(1f).semantics { heading() }.graphicsLayer { alpha = progress },
                        fontSize = (22f - 4f * progress).sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    TopBarIconButton(
                        imageVector = MaterialSymbols.ArrowUpward,
                        contentDescription = stringResource(cn.turingzy.ledger.R.string.ui_scroll_to_top),
                        onClick = { scope.launch { onScrollToTop() } },
                        modifier = Modifier.graphicsLayer { alpha = progress },
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                actions?.invoke(this)
            }
        }
    }
}
