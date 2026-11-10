package cn.turingzy.ledger.ui.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.ui.component.LedgerBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsBottomSheet(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    action: @Composable () -> Unit = { Box(Modifier.size(48.dp)) },
    content: @Composable ColumnScope.() -> Unit,
) {
    LedgerBottomSheet(title, onDismissRequest, modifier, onBack = onBack, action = action, content = content)
}
