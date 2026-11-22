package cn.turingzy.ledger.ui.feature.settings.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import cn.turingzy.ledger.R
import cn.turingzy.ledger.ui.component.base.ButtonBase
import cn.turingzy.ledger.ui.component.base.ButtonToneBase
import cn.turingzy.ledger.ui.component.base.ButtonVariantBase
import cn.turingzy.ledger.ui.component.base.IconButtonBase
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.PaddingValues
import cn.turingzy.ledger.ui.component.base.CardBase
import cn.turingzy.ledger.ui.component.base.ListRowBase
import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.ui.component.MaterialSymbols

internal val SettingsItemHeight = LedgerStyle.dimensions.rowMinHeight

@Composable
internal fun SettingsActionRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    externalLink: Boolean = false,
    showChevron: Boolean = true,
    enabled: Boolean = true,
) {
    CardBase(modifier, contentPadding = PaddingValues(LedgerStyle.spacing.none), contentSpacing = LedgerStyle.spacing.none) {
        ListRowBase(
            headline = {
                Row(horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.hairline), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.bodyLarge,
                        color = contentColor.copy(alpha = contentColor.alpha * if (enabled) 1f else LedgerStyle.opacity.disabled))
                    if (externalLink) Icon(MaterialSymbols.NorthEast, null, Modifier.size(LedgerStyle.icons.externalLink),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            modifier = Modifier.heightIn(min = if (subtitle == null) SettingsItemHeight else LedgerStyle.dimensions.supportingRowMinHeight)
                .semantics { if (!enabled) disabled() },
            supportingText = if (subtitle == null) null else ({
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }),
            supportingSpacing = LedgerStyle.spacing.tiny,
            trailing = if (!showChevron || externalLink) null else ({
                Icon(MaterialSymbols.ChevronRight, null, Modifier.size(LedgerStyle.icons.default), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }),
            onClick = if (enabled) onClick else null,
        )
    }
}

/** Replaces the delete text with inline confirmation for the current item. */
@Composable
internal fun SettingsDeleteAction(
    itemId: Long,
    title: String,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var confirming by remember(itemId) { mutableStateOf(false) }
    if (confirming) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            IconButtonBase(
                MaterialSymbols.Close,
                stringResource(R.string.ui_cancel),
                onClick = { confirming = false },
                iconSize = LedgerStyle.icons.inline,
            )
            IconButtonBase(
                MaterialSymbols.Check,
                stringResource(R.string.ui_confirm),
                onClick = {
                    confirming = false
                    onDelete()
                },
                enabled = enabled,
                contentColor = LedgerStyle.colors.success,
            )
        }
    } else {
        ButtonBase(
            title,
            onClick = { confirming = true },
            modifier = modifier,
            enabled = enabled,
            variant = ButtonVariantBase.Text,
            tone = ButtonToneBase.Destructive,
        )
    }
}
