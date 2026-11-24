package cn.turingzy.ledger.ui.component.base

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import cn.turingzy.ledger.R
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.interaction.UiInteractionKind
import cn.turingzy.ledger.ui.theme.LedgerStyle

/**
 * 主页面标题行。筛选项默认居中，也可使用标题和右侧操作之间的剩余宽度。
 */
@Composable
internal fun PrimaryPageHeaderBase(
    title: String,
    centerContent: (@Composable () -> Unit)? = null,
    centerContentUsesAvailableWidth: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val useAvailableWidth = centerContent != null && centerContentUsesAvailableWidth
    BoxWithConstraints(Modifier.fillMaxWidth().height(LedgerStyle.dimensions.toolbarHeight)) {
        val centerMaxWidth = maxWidth / 2
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                modifier = (if (useAvailableWidth) Modifier else Modifier.weight(1f))
                    .semantics { heading() },
                style = LedgerStyle.typography.screenTitle,
                letterSpacing = (-0.6).sp,
                color = LedgerStyle.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (centerContent != null) {
                Box(
                    modifier = if (useAvailableWidth) Modifier.weight(1f)
                    else Modifier.widthIn(max = centerMaxWidth),
                    contentAlignment = Alignment.Center,
                ) { centerContent() }
            }
            Row(
                modifier = if (centerContent == null || useAvailableWidth) Modifier
                else Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.labelGap, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}

/**
 * 基础页面脚手架组件。
 *
 * 包含沉浸式毛玻璃效果顶部栏、页面标题、返回/关闭操作按钮和自定义操作插槽，
 * 顶部栏和内容内边距共用固定的导航栏高度。
 *
 * @param title 页面顶部标题文本。
 * @param modifier 应用于脚手架的修饰符。
 * @param onBack 可选的返回按钮点击回调；为 null 时不显示返回按钮。
 * @param scrolled 页面内容是否已发生滚动（滚动时可改变顶部栏透明度）。
 * @param windowInsets 页面适配的窗口边距，默认为 [WindowInsets.safeDrawing]。
 * @param closeInsteadOfBack 返回按钮是否显示为“关闭（叉号）”图标而非“返回（箭头）”图标。
 * @param headerOpacity 顶部栏背景的不透明度。
 * @param actions 顶栏右侧操作区，放置最多两个图标或一个文字操作。
 * @param content 页面主内容区域，提供包含顶部导航栏高度的 [PaddingValues]。
 */
@Composable
fun PageScaffoldBase(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    scrolled: Boolean = false,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
    closeInsteadOfBack: Boolean = false,
    headerOpacity: Float = if (scrolled) LedgerStyle.opacity.topBarScrolled else 1f,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val colors = LedgerStyle.colors
    val backdrop = rememberBackdropStateBaseBase()
    val barHeight = LedgerStyle.dimensions.toolbarHeight
    PageFrameBase(modifier, windowInsets) {
        Box(Modifier.fillMaxSize().recordBackdropBase(backdrop)) {
            content(PaddingValues(top = barHeight))
        }
        Box(Modifier.fillMaxWidth().blockTouchesBehind().frostedBackdropBase(
            backdrop,
            colors.background.copy(alpha = headerOpacity),
            blurRadius = LedgerStyle.dimensions.topBarBlur,
            fadeBottom = LedgerStyle.dimensions.subpageTopBarFade,
        )) {
            Row(
                Modifier.fillMaxWidth().height(barHeight)
                    .padding(horizontal = LedgerStyle.spacing.medium).testTag("basePageTopBar"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) PageBackButton(
                    stringResource(if (closeInsteadOfBack) R.string.ui_cancel else R.string.ui_back),
                    closeInsteadOfBack,
                    onBack,
                )
                Text(
                    title, Modifier.weight(1f).semantics { heading() },
                    style = LedgerStyle.typography.detailPageTitle,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.labelGap),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        }
    }
}

/**
 * 页面返回或关闭按钮。
 *
 * 在保持触控目标区域大小的同时，为图标应用按压透明度变化与导航触觉反馈。
 *
 * @param description 无障碍文本描述。
 * @param closeInsteadOfBack 是否显示为叉号关闭图标。
 * @param onBack 点击回调。
 */
@Composable
private fun PageBackButton(description: String, closeInsteadOfBack: Boolean, onBack: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val tint = LedgerStyle.colors.textPrimary
    IconButtonContentBase(
        onClick = onBack,
        interactionSource = source,
        interaction = UiInteractionKind.Navigation,
        showPressFeedback = false,
    ) {
        Icon(
            if (closeInsteadOfBack) MaterialSymbols.Close else MaterialSymbols.ChevronLeft,
            description,
            Modifier.size(if (closeInsteadOfBack) LedgerStyle.icons.default else LedgerStyle.icons.back),
            tint = if (pressed) tint.copy(alpha = tint.alpha * 0.55f) else tint,
        )
    }
}

/**
 * 基础页面框架容器。
 *
 * 填充整个屏幕并统一应用背景色、文字颜色和窗口安全区避让。
 *
 * @param modifier 应用于页面的修饰符。
 * @param windowInsets 需要避让的窗口系统内边距。
 * @param content 页面容器内容插槽。
 */
@Composable
fun PageFrameBase(
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
    content: @Composable BoxScope.() -> Unit,
) {
    SurfaceBase(modifier.fillMaxSize(), color = LedgerStyle.colors.background, contentColor = LedgerStyle.colors.textPrimary) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(windowInsets), content = content)
    }
}
