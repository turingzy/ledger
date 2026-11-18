package cn.turingzy.ledger.ui.feature.wallet.sheet

import cn.turingzy.ledger.ui.theme.LedgerStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import cn.turingzy.ledger.R
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.base.BaseIconButton
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

@Composable
internal fun AccountSheetContent(
    title: String,
    saveEnabled: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    contentSpacing: Dp = 8.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val keyboardHeight = WindowInsets.ime.getBottom(density)
    val bottomInset = maxOf(keyboardHeight, WindowInsets.navigationBars.getBottom(density))
    Column(
        modifier.fillMaxWidth().height(LedgerStyle.dimensions.sheetMaxHeight)
            .padding(bottom = with(density) { bottomInset.toDp() }),
    ) {
        Box(
            Modifier.fillMaxWidth().height(44.dp).padding(horizontal = LedgerStyle.spacing.large),
            contentAlignment = Alignment.Center,
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
            BaseIconButton(MaterialSymbols.Close, stringResource(R.string.ui_cancel), onDismiss,
                modifier = Modifier.align(Alignment.CenterStart), showPressFeedback = false)
            AccountSaveButton(saveEnabled, onSave, Modifier.align(Alignment.CenterEnd))
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = LedgerStyle.spacing.section).padding(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(contentSpacing),
            content = content,
        )
    }
}
