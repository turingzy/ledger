package cn.turingzy.ledger.ui.feature.wallet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.SummaryCardIconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        content = content,
    )
}

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
    Column(modifier.fillMaxWidth().height(640.dp).imePadding().navigationBarsPadding()) {
        Box(
            Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 12.dp),
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
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(contentSpacing),
            content = content,
        )
    }
}
