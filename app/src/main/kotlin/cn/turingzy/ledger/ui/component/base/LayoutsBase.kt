package cn.turingzy.ledger.ui.component.base

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import cn.turingzy.ledger.ui.interaction.rememberUiAction
import cn.turingzy.ledger.ui.interaction.rememberUiIndication
import cn.turingzy.ledger.ui.interaction.uiClickable
import cn.turingzy.ledger.ui.theme.LedgerStyle

/** 最近一层容器底色，用于让浮动的表单标签覆盖字段边框。 */
internal val LocalSurfaceBackground = compositionLocalOf { Color.Unspecified }

/**
 * 基础表面（Surface）容器组件。
 *
 * 包装 Material3 [Surface]，在提供背景色、形状和高程的基础上，统一处理按压动效与触觉反馈。
 *
 * @param modifier 应用于表面容器的修饰符。
 * @param shape 表面形状。
 * @param color 背景颜色。
 * @param contentColor 内部内容默认颜色。
 * @param tonalElevation 色调高程。
 * @param shadowElevation 阴影高程。
 * @param border 可选的边框样式。
 * @param onClick 可选的点击事件回调；不为 null 时容器支持点击与按压动效。
 * @param enabled 是否启用交互。
 * @param content 内部内容插槽。
 */
@Composable
fun SurfaceBase(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = LedgerStyle.colors.surface,
    contentColor: Color = contentColorFor(color),
    tonalElevation: Dp = LedgerStyle.spacing.none,
    shadowElevation: Dp = LedgerStyle.spacing.none,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val surfaceContent: @Composable () -> Unit = {
        CompositionLocalProvider(LocalSurfaceBackground provides color, content = content)
    }
    if (onClick == null) {
        Surface(modifier, shape, color, contentColor, tonalElevation, shadowElevation, border, content = surfaceContent)
        return
    }
    val source = remember { MutableInteractionSource() }
    Surface(
        onClick = rememberUiAction(onClick),
        modifier = modifier.clip(shape).indication(source, rememberUiIndication()),
        enabled = enabled, shape = shape, color = color, contentColor = contentColor,
        tonalElevation = tonalElevation, shadowElevation = shadowElevation, border = border,
        interactionSource = source, content = surfaceContent,
    )
}

/**
 * 基础卡片组件。
 *
 * 基于 [SurfaceBase] 构建，提供符合应用规范的圆角形状、内边距、行间距及可选描边。
 *
 * @param modifier 应用于卡片的修饰符。
 * @param shape 卡片形状。
 * @param outlined 是否显示细描边。
 * @param contentPadding 卡片内部内边距。
 * @param contentSpacing 垂直子项之间的间距。
 * @param content 卡片垂直布局内容插槽。
 */
@Composable
fun CardBase(
    modifier: Modifier = Modifier,
    shape: Shape = LedgerStyle.shapes.card,
    outlined: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(LedgerStyle.spacing.cardPadding),
    contentSpacing: Dp = LedgerStyle.spacing.sectionGap,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LedgerStyle.colors
    SurfaceBase(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = colors.surface,
        contentColor = colors.textPrimary,
        border = if (outlined) BorderStroke(LedgerStyle.dimensions.dividerThickness, colors.outline) else null,
    ) {
        Column(
            Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(contentSpacing),
        ) {
            ProvideTextStyle(LedgerStyle.typography.body) { content() }
        }
    }
}

/**
 * 基础列表行组件。
 *
 * 采用三段式结构：前缀（leading）、主体标题与辅助文本（headline / supportingText）、尾部（trailing）。
 *
 * @param headline 主标题内容插槽。
 * @param modifier 应用于列表行的修饰符。
 * @param supportingText 可选的下方辅助说明文本插槽。
 * @param leading 可选的左侧前缀插槽（如小图标或标识）。
 * @param trailing 可选的右侧尾部插槽（如操作按键、数值或辅助信息）。
 * @param enabled 是否处于可用交互状态。
 * @param contentPadding 列表行内边距。
 * @param leadingSpacing 前缀与主体之间的间距。
 * @param trailingSpacing 主体与尾部之间的间距。
 * @param supportingSpacing 主标题与辅助文本之间的垂直间距。
 * @param onClick 可选的整行点击回调。
 */
@Composable
fun ListRowBase(
    headline: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: (@Composable () -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = LedgerStyle.spacing.rowHorizontal, vertical = LedgerStyle.spacing.rowVertical,
    ),
    leadingSpacing: Dp = LedgerStyle.spacing.sectionGap,
    trailingSpacing: Dp = LedgerStyle.spacing.sectionGap,
    supportingSpacing: Dp = LedgerStyle.spacing.labelGap,
    onClick: (() -> Unit)? = null,
) {
    val colors = LedgerStyle.colors
    val interaction = if (onClick == null) Modifier else Modifier.uiClickable(enabled = enabled, onClick = onClick)
    Row(
        modifier.fillMaxWidth().heightIn(min = LedgerStyle.dimensions.rowMinHeight)
            .then(interaction)
            .semantics { if (!enabled) disabled() }
            .alpha(if (enabled) 1f else LedgerStyle.opacity.disabled)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            CompositionLocalProvider(LocalContentColor provides colors.textSecondary) { leading() }
            Spacer(Modifier.width(leadingSpacing))
        }
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(supportingSpacing),
        ) {
            CompositionLocalProvider(LocalContentColor provides colors.textPrimary) {
                ProvideTextStyle(LedgerStyle.typography.body, content = headline)
            }
            if (supportingText != null) {
                CompositionLocalProvider(LocalContentColor provides colors.textSupporting) {
                    ProvideTextStyle(LedgerStyle.typography.bodySmall, content = supportingText)
                }
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(trailingSpacing))
            CompositionLocalProvider(LocalContentColor provides colors.textSecondary) {
                ProvideTextStyle(LedgerStyle.typography.bodyMedium, content = trailing)
            }
        }
    }
}

/**
 * 分组标题样式变体。
 */
enum class SectionHeaderVariantBase {
    /** 主要章节标题样式。 */
    Section,
    /** 次要辅助标题样式。 */
    Supporting,
}

/**
 * 分组标题栏组件。
 *
 * 包含左侧标题与副标题，以及右侧操作区域。
 *
 * @param title 标题文本。
 * @param modifier 应用于分组标题栏的修饰符。
 * @param subtitle 可选的副标题文本。
 * @param variant 标题样式变体，参见 [SectionHeaderVariantBase]。
 * @param actions 可选的右侧操作项布局插槽。
 */
@Composable
fun SectionHeaderBase(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    variant: SectionHeaderVariantBase = SectionHeaderVariantBase.Section,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = LedgerStyle.colors
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.inlineGap),
    ) {
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.labelGap),
        ) {
            Text(
                title,
                style = if (variant == SectionHeaderVariantBase.Supporting) LedgerStyle.typography.bodyMedium else LedgerStyle.typography.sectionTitle,
                color = if (variant == SectionHeaderVariantBase.Supporting) colors.textSecondary else colors.textPrimary,
            )
            if (subtitle != null) {
                Text(subtitle, style = LedgerStyle.typography.bodySmall, color = colors.textSupporting)
            }
        }
        if (actions != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.inlineGap),
                content = actions,
            )
        }
    }
}

/**
 * 空状态展示组件。
 *
 * 用于数据列表为空或未配置时的占位展示，支持展示插画/图标、标题、解释文案和操作按钮。
 *
 * @param title 空状态标题文本。
 * @param modifier 应用于空状态容器的修饰符。
 * @param message 可选的空状态补充说明文本。
 * @param illustration 可选的插画或图标内容插槽。
 * @param actions 可选的操作按钮区插槽。
 * @param contentPadding 容器内边距。
 * @param titleStyle 标题文字样式。
 * @param titleColor 标题文字颜色。
 */
@Composable
fun EmptyStateBase(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    illustration: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = LedgerStyle.spacing.section,
        vertical = LedgerStyle.spacing.block,
    ),
    titleStyle: TextStyle = LedgerStyle.typography.cardTitle,
    titleColor: Color = LedgerStyle.colors.textPrimary,
) {
    val colors = LedgerStyle.colors
    Column(
        modifier.fillMaxWidth().padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.sectionGap),
    ) {
        if (illustration != null) illustration()
        Text(
            title, style = titleStyle,
            color = titleColor, textAlign = TextAlign.Center,
        )
        if (message != null) {
            Text(
                message, style = LedgerStyle.typography.bodySmall,
                color = colors.textSupporting, textAlign = TextAlign.Center,
            )
        }
        if (actions != null) actions()
    }
}

/** 将单行空状态提示居中放在原本展示内容的区域。 */
@Composable
internal fun CenteredEmptyStateBase(title: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            color = LedgerStyle.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * 基础水平分割线。
 *
 * 采用全局统一的分割线粗细与颜色。
 *
 * @param modifier 应用于分割线的修饰符。
 */
@Composable
fun DividerBase(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = LedgerStyle.dimensions.dividerThickness,
        color = LedgerStyle.colors.divider,
    )
}

/** 在受影响的内容或操作旁显示状态，并由无障碍服务读出更新。 */
@Composable
internal fun StatusMessageBase(message: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        message,
        modifier.semantics { liveRegion = LiveRegionMode.Polite },
        style = LedgerStyle.typography.bodySmall,
        color = color,
    )
}

/** 列表分割线从标题起点开始，尾部与行内容对齐。 */
@Composable
internal fun ListRowDividerBase(startInset: Dp = LedgerStyle.spacing.rowHorizontal) {
    DividerBase(Modifier.padding(start = startInset, end = LedgerStyle.spacing.rowHorizontal))
}
