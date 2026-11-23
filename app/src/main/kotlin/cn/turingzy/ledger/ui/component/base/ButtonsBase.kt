package cn.turingzy.ledger.ui.component.base

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.ui.interaction.UiInteractionKind
import cn.turingzy.ledger.ui.interaction.rememberUiAction
import cn.turingzy.ledger.ui.interaction.rememberUiIndication
import cn.turingzy.ledger.ui.theme.LedgerStyle

/**
 * 按钮变体样式。
 */
enum class ButtonVariantBase {
    /** 实心填充样式。 */
    Filled,
    /** 描边轮廓样式。 */
    Outlined,
    /** 纯文本无边框样式。 */
    Text,
}

/**
 * 按钮语义色调。
 */
enum class ButtonToneBase {
    /** 默认主色调。 */
    Default,
    /** 破坏性/危险操作色调（红色）。 */
    Destructive,
    /** 中性次要色调。 */
    Neutral,
}

/**
 * 基础操作按钮组件。
 *
 * 支持文字标签、前置与后置图标、不同变体（填充、描边、文本）和色调（默认、破坏性、中性），
 * 并集成了全局触觉与按下动效反馈。
 *
 * @param label 按钮显示的文本内容。
 * @param onClick 点击事件回调。
 * @param modifier 应用于按钮的修饰符。
 * @param enabled 是否启用按钮。
 * @param variant 按钮外观变体，参见 [ButtonVariantBase]。
 * @param tone 按钮语义色调，参见 [ButtonToneBase]。
 * @param leadingIcon 可选的前置图标矢量图。
 * @param interaction 交互反馈类型（如触觉效果），参见 [UiInteractionKind]。
 * @param trailingIcon 可选的后置图标矢量图。
 * @param iconSize 图标显示尺寸。
 * @param shape 按钮的形状裁剪。
 * @param showPressFeedback 是否展示按压指示反馈。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ButtonBase(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: ButtonVariantBase = ButtonVariantBase.Filled,
    tone: ButtonToneBase = ButtonToneBase.Default,
    leadingIcon: ImageVector? = null,
    interaction: UiInteractionKind = UiInteractionKind.Action,
    trailingIcon: ImageVector? = null,
    iconSize: Dp = LedgerStyle.icons.default,
    shape: Shape = if (variant == ButtonVariantBase.Text) LedgerStyle.shapes.pill else LedgerStyle.shapes.control,
    showPressFeedback: Boolean = true,
) {
    val colors = LedgerStyle.colors
    val accent = when (tone) {
        ButtonToneBase.Default -> colors.info
        ButtonToneBase.Destructive -> colors.destructive
        ButtonToneBase.Neutral -> colors.textSecondary
    }
    val onAccent = when (tone) {
        ButtonToneBase.Default -> MaterialTheme.colorScheme.onPrimary
        ButtonToneBase.Destructive -> MaterialTheme.colorScheme.onError
        ButtonToneBase.Neutral -> colors.textSecondary
    }
    val isTextButton = variant == ButtonVariantBase.Text
    // Reserve the touch target outside the compact, clipped press background.
    val buttonModifier = if (isTextButton) modifier.minimumInteractiveComponentSize().height(40.dp)
        else modifier.heightIn(min = LedgerStyle.dimensions.touchTarget)
    val padding = if (isTextButton) PaddingValues(horizontal = LedgerStyle.spacing.large)
        else PaddingValues(horizontal = LedgerStyle.spacing.extraLarge, vertical = LedgerStyle.spacing.rowVertical)
    val content: @Composable RowScope.() -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.inlineGap),
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(iconSize))
            }
            Text(label, style = LedgerStyle.typography.button)
            if (trailingIcon != null) {
                Icon(trailingIcon, contentDescription = null, modifier = Modifier.size(iconSize).testTag("baseButtonTrailingIcon"))
            }
        }
    }
    val buttonColors = when (variant) {
        ButtonVariantBase.Filled -> ButtonDefaults.buttonColors(
            containerColor = if (tone == ButtonToneBase.Neutral) colors.background else accent,
            contentColor = onAccent, disabledContainerColor = colors.fieldBackground,
            disabledContentColor = colors.textPrimary.copy(alpha = LedgerStyle.opacity.disabled),
        )
        ButtonVariantBase.Outlined -> ButtonDefaults.outlinedButtonColors(contentColor = accent,
            disabledContentColor = colors.textPrimary.copy(alpha = LedgerStyle.opacity.disabled))
        ButtonVariantBase.Text -> ButtonDefaults.textButtonColors(contentColor = accent,
            disabledContentColor = colors.textPrimary.copy(alpha = LedgerStyle.opacity.disabled))
    }
    ActionButtonBase(
        onClick, buttonModifier, enabled, variant, shape, buttonColors,
        border = if (variant == ButtonVariantBase.Outlined) BorderStroke(LedgerStyle.dimensions.dividerThickness, colors.outline) else null,
        contentPadding = padding, interaction = interaction, showPressFeedback = showPressFeedback, content = content,
    )
}

/**
 * 底层通用操作按钮。
 *
 * 包装了 Material3 的 [Button]、[OutlinedButton] 和 [TextButton]，统一禁用默认涟漪并注入自定义交互动效与触觉反馈。
 *
 * @param onClick 点击事件回调。
 * @param modifier 应用于按钮的修饰符。
 * @param enabled 按钮是否可用。
 * @param variant 按钮样式变体。
 * @param shape 按钮的形状裁剪。
 * @param colors 按钮的颜色配置。
 * @param elevation 按钮阴影高程。
 * @param border 按钮描边样式。
 * @param contentPadding 内部内容的内边距。
 * @param interactionSource 交互事件源。
 * @param interaction 触觉/操作交互类型。
 * @param showPressFeedback 是否展示按压指示反馈。
 * @param actionKey 用于重置 action 记忆的键。
 * @param content 按钮内容插槽。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionButtonBase(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: ButtonVariantBase = ButtonVariantBase.Filled,
    shape: Shape = ButtonDefaults.outlinedShape,
    colors: androidx.compose.material3.ButtonColors = when (variant) {
        ButtonVariantBase.Filled -> ButtonDefaults.buttonColors()
        ButtonVariantBase.Outlined -> ButtonDefaults.outlinedButtonColors()
        ButtonVariantBase.Text -> ButtonDefaults.textButtonColors()
    },
    elevation: androidx.compose.material3.ButtonElevation? = if (variant == ButtonVariantBase.Filled) ButtonDefaults.buttonElevation() else null,
    border: BorderStroke? = if (variant == ButtonVariantBase.Outlined) ButtonDefaults.outlinedButtonBorder(enabled) else null,
    contentPadding: PaddingValues = if (variant == ButtonVariantBase.Text) ButtonDefaults.TextButtonContentPadding else ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    interaction: UiInteractionKind = UiInteractionKind.Action,
    showPressFeedback: Boolean = true,
    actionKey: Any? = Unit,
    content: @Composable RowScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val action = androidx.compose.runtime.key(actionKey) { rememberUiAction(onClick, interaction) }
    val target = modifier.clip(shape).then(if (showPressFeedback) Modifier.indication(source, rememberUiIndication()) else Modifier)
    CompositionLocalProvider(LocalRippleConfiguration provides null) {
        when (variant) {
            ButtonVariantBase.Filled -> Button(action, target, enabled, shape, colors, elevation, border, contentPadding, source, content)
            ButtonVariantBase.Outlined -> OutlinedButton(action, target, enabled, shape, colors, elevation, border, contentPadding, source, content)
            ButtonVariantBase.Text -> TextButton(onClick = action, modifier = target, enabled = enabled, shape = shape, colors = colors,
                contentPadding = contentPadding, interactionSource = source, content = content)
        }
    }
}

/**
 * 基础图标按钮。
 *
 * 包装图标并提供标准触控尺寸与交互动效。
 *
 * @param imageVector 图标矢量图。
 * @param contentDescription 图标无障碍描述。
 * @param onClick 点击事件回调。
 * @param modifier 应用于按钮的修饰符。
 * @param enabled 按钮是否可用。
 * @param contentColor 图标颜色。
 * @param iconSize 图标显示尺寸。
 * @param interaction 触觉/操作交互类型。
 * @param showPressFeedback 是否展示按压指示反馈。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconButtonBase(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = LedgerStyle.colors.textSecondary,
    iconSize: Dp = LedgerStyle.icons.default,
    interaction: UiInteractionKind = UiInteractionKind.Action,
    showPressFeedback: Boolean = true,
) {
    IconButtonContentBase(
        onClick, modifier, enabled,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = contentColor,
            disabledContentColor = contentColor.copy(alpha = contentColor.alpha * LedgerStyle.opacity.disabled),
        ),
        interaction = interaction, showPressFeedback = showPressFeedback,
    ) { Icon(imageVector, contentDescription, Modifier.size(iconSize)) }
}

/**
 * 基础图标按钮内容容器。
 *
 * 保证最小触控区域（48dp）与药丸状形状裁剪，支持自定义内容。
 *
 * @param onClick 点击事件回调。
 * @param modifier 应用于按钮的修饰符。
 * @param enabled 按钮是否可用。
 * @param colors 图标按钮的颜色配置。
 * @param interactionSource 交互事件源。
 * @param interaction 触觉/操作交互类型。
 * @param showPressFeedback 是否展示按压指示反馈。
 * @param content 按钮内部自定义内容。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconButtonContentBase(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    interaction: UiInteractionKind = UiInteractionKind.Action,
    showPressFeedback: Boolean = true,
    content: @Composable () -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    CompositionLocalProvider(LocalRippleConfiguration provides null) {
        IconButton(
            onClick = rememberUiAction(onClick, interaction),
            modifier = modifier.size(LedgerStyle.dimensions.touchTarget).clip(LedgerStyle.shapes.pill)
                .then(if (showPressFeedback) Modifier.indication(source, rememberUiIndication()) else Modifier),
            enabled = enabled, colors = colors, interactionSource = source, content = content,
        )
    }
}
