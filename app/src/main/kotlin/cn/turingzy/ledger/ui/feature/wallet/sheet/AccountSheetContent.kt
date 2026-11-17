package cn.turingzy.ledger.ui.feature.wallet.sheet

import cn.turingzy.ledger.ui.theme.LedgerStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.card.summary.SummaryCardIconButton

@Composable
internal fun AccountSheetContent(
    title: String,
    closeDescription: String,
    onClose: () -> Unit,
    saveEnabled: Boolean,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    contentSpacing: Dp = 8.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().heightIn(max = LedgerStyle.dimensions.sheetMaxHeight).imePadding().navigationBarsPadding()) {
        Box(
            Modifier.fillMaxWidth().height(44.dp).padding(horizontal = LedgerStyle.spacing.large),
            contentAlignment = Alignment.Center,
        ) {
            SummaryCardIconButton(
                imageVector = MaterialSymbols.Close,
                contentDescription = closeDescription,
                onClick = onClose,
                modifier = Modifier.align(Alignment.CenterStart),
                containerColor = colors.surface,
                contentColor = colors.onSurface,
                showPressFeedback = false,
            )
            Text(title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
            AccountSaveButton(saveEnabled, onSave, Modifier.align(Alignment.CenterEnd))
        }
        Column(
            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                .padding(horizontal = LedgerStyle.spacing.section).padding(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(contentSpacing),
            content = content,
        )
    }
}
