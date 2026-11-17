package cn.turingzy.ledger.ui.components.base

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import cn.turingzy.ledger.ui.interaction.UiInteractionKind
import cn.turingzy.ledger.ui.interaction.rememberUiAction
import cn.turingzy.ledger.ui.interaction.rememberUiIndication
import cn.turingzy.ledger.ui.theme.LedgerStyle

enum class BaseButtonVariant { Filled, Outlined, Text }

enum class BaseButtonTone { Default, Destructive, Neutral }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: BaseButtonVariant = BaseButtonVariant.Filled,
    tone: BaseButtonTone = BaseButtonTone.Default,
    leadingIcon: ImageVector? = null,
    interaction: UiInteractionKind = UiInteractionKind.Action,
    trailingIcon: ImageVector? = null,
    iconSize: Dp = LedgerStyle.icons.default,
    shape: Shape = LedgerStyle.shapes.control,
    showPressFeedback: Boolean = true,
) {
    val colors = LedgerStyle.colors
    val accent = when (tone) {
        BaseButtonTone.Default -> colors.info
        BaseButtonTone.Destructive -> colors.destructive
        BaseButtonTone.Neutral -> colors.textSecondary
    }
    val onAccent = when (tone) {
        BaseButtonTone.Default -> MaterialTheme.colorScheme.onPrimary
        BaseButtonTone.Destructive -> MaterialTheme.colorScheme.onError
        BaseButtonTone.Neutral -> colors.textSecondary
    }
    val buttonModifier = modifier.heightIn(min = LedgerStyle.dimensions.touchTarget)
    val padding = PaddingValues(
        horizontal = LedgerStyle.spacing.extraLarge,
        vertical = LedgerStyle.spacing.rowVertical,
    )
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
        BaseButtonVariant.Filled -> ButtonDefaults.buttonColors(
            containerColor = if (tone == BaseButtonTone.Neutral) colors.background else accent,
            contentColor = onAccent, disabledContainerColor = colors.fieldBackground,
            disabledContentColor = colors.textPrimary.copy(alpha = LedgerStyle.opacity.disabled),
        )
        BaseButtonVariant.Outlined -> ButtonDefaults.outlinedButtonColors(contentColor = accent,
            disabledContentColor = colors.textPrimary.copy(alpha = LedgerStyle.opacity.disabled))
        BaseButtonVariant.Text -> ButtonDefaults.textButtonColors(contentColor = accent,
            disabledContentColor = colors.textPrimary.copy(alpha = LedgerStyle.opacity.disabled))
    }
    BaseActionButton(
        onClick, buttonModifier, enabled, variant, shape, buttonColors,
        border = if (variant == BaseButtonVariant.Outlined) BorderStroke(LedgerStyle.dimensions.dividerThickness, colors.divider) else null,
        contentPadding = padding, interaction = interaction, showPressFeedback = showPressFeedback, content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: BaseButtonVariant = BaseButtonVariant.Filled,
    shape: Shape = ButtonDefaults.outlinedShape,
    colors: androidx.compose.material3.ButtonColors = when (variant) {
        BaseButtonVariant.Filled -> ButtonDefaults.buttonColors()
        BaseButtonVariant.Outlined -> ButtonDefaults.outlinedButtonColors()
        BaseButtonVariant.Text -> ButtonDefaults.textButtonColors()
    },
    elevation: androidx.compose.material3.ButtonElevation? = if (variant == BaseButtonVariant.Filled) ButtonDefaults.buttonElevation() else null,
    border: BorderStroke? = if (variant == BaseButtonVariant.Outlined) ButtonDefaults.outlinedButtonBorder(enabled) else null,
    contentPadding: PaddingValues = if (variant == BaseButtonVariant.Text) ButtonDefaults.TextButtonContentPadding else ButtonDefaults.ContentPadding,
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
            BaseButtonVariant.Filled -> Button(action, target, enabled, shape, colors, elevation, border, contentPadding, source, content)
            BaseButtonVariant.Outlined -> OutlinedButton(action, target, enabled, shape, colors, elevation, border, contentPadding, source, content)
            BaseButtonVariant.Text -> TextButton(onClick = action, modifier = target, enabled = enabled, shape = shape, colors = colors,
                contentPadding = contentPadding, interactionSource = source, content = content)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseIconButton(
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
    BaseIconButtonContent(
        onClick, modifier, enabled,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = contentColor,
            disabledContentColor = contentColor.copy(alpha = contentColor.alpha * LedgerStyle.opacity.disabled),
        ),
        interaction = interaction, showPressFeedback = showPressFeedback,
    ) { Icon(imageVector, contentDescription, Modifier.size(iconSize)) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseIconButtonContent(
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
