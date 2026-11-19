package cn.turingzy.ledger.ui.component.base

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import cn.turingzy.ledger.ui.interaction.rememberUiAction
import cn.turingzy.ledger.ui.interaction.rememberUiIndication
import cn.turingzy.ledger.ui.interaction.uiClickable
import cn.turingzy.ledger.ui.theme.LedgerStyle

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
    if (onClick == null) {
        Surface(modifier, shape, color, contentColor, tonalElevation, shadowElevation, border, content = content)
        return
    }
    val source = remember { MutableInteractionSource() }
    Surface(
        onClick = rememberUiAction(onClick),
        modifier = modifier.clip(shape).indication(source, rememberUiIndication()),
        enabled = enabled, shape = shape, color = color, contentColor = contentColor,
        tonalElevation = tonalElevation, shadowElevation = shadowElevation, border = border,
        interactionSource = source, content = content,
    )
}

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
        border = if (outlined) BorderStroke(LedgerStyle.dimensions.dividerThickness, colors.divider) else null,
    ) {
        Column(
            Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(contentSpacing),
        ) {
            ProvideTextStyle(LedgerStyle.typography.body) { content() }
        }
    }
}

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
                CompositionLocalProvider(LocalContentColor provides colors.textSecondary) {
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

enum class SectionHeaderVariantBase { Section, Supporting }

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
                Text(subtitle, style = LedgerStyle.typography.bodySmall, color = colors.textSecondary)
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

@Composable
fun EmptyStateBase(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    illustration: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = LedgerStyle.spacing.pageHorizontal,
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
                color = colors.textSecondary, textAlign = TextAlign.Center,
            )
        }
        if (actions != null) actions()
    }
}

@Composable
fun DividerBase(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = LedgerStyle.dimensions.dividerThickness,
        color = LedgerStyle.colors.divider,
    )
}
