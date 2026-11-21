package cn.turingzy.ledger.ui.component.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.ui.interaction.UiInteractionKind
import cn.turingzy.ledger.ui.interaction.uiClickable
import cn.turingzy.ledger.ui.theme.LedgerStyle

/** Shares the date entry's size and typography between the bill and calendar pages. */
@Composable
internal fun PeriodPickerPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = LedgerStyle.colors.surface,
    description: String? = null,
    trailingIcon: ImageVector? = null,
) {
    val foreground = LedgerStyle.colors.textPrimary
    Row(
        modifier
            .heightIn(min = 32.dp)
            .clip(LedgerStyle.shapes.pill)
            .background(background)
            .uiClickable(kind = UiInteractionKind.Navigation, onClick = onClick)
            .semantics { if (description != null) contentDescription = description }
            .padding(horizontal = LedgerStyle.spacing.large, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            style = LedgerStyle.typography.periodPill,
            color = foreground, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        if (trailingIcon != null) Icon(trailingIcon, null, Modifier.size(17.dp), tint = foreground)
    }
}
