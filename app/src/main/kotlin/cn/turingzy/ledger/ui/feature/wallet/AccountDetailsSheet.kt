package cn.turingzy.ledger.ui.feature.wallet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.LedgerIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.model.AccountType
import cn.turingzy.ledger.model.CardTemplate
import cn.turingzy.ledger.ui.theme.LedgerTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailsSheet(
    account: Account,
    onDismissRequest: () -> Unit,
    onSave: (Account) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
    templates: List<CardTemplate> = emptyList(),
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val scope = rememberCoroutineScope()
    fun dismiss() {
        scope.launch {
            sheetState.hide()
            if (!sheetState.isVisible) onDismissRequest()
        }
    }

    AccountSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        AccountDetailsContent(
            account = account,
            templates = templates,
            onClose = ::dismiss,
            onDelete = { onDelete(account.id); dismiss() },
            onSave = {
                onSave(it)
                dismiss()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailsContent(
    account: Account,
    onClose: () -> Unit,
    onSave: (Account) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    templates: List<CardTemplate> = emptyList(),
) {
    var confirmDelete by rememberSaveable(account.id) { mutableStateOf(false) }
    AccountEditorContent(
        account = account,
        templates = templates,
        onClose = onClose,
        onSave = onSave,
        modifier = modifier,
        onDelete = { confirmDelete = true },
    )
    if (confirmDelete) {
        BasicAlertDialog(onDismissRequest = { confirmDelete = false }) {
            val colors = MaterialTheme.colorScheme
            // Include the official symbols' viewport insets when aligning their visible edges.
            val closeIconInset = 8.dp + 24.dp * (217f / 960f)
            val deleteIconInset = 8.dp + 24.dp * (160f / 960f)
            Surface(shape = RoundedCornerShape(28.dp), color = colors.surface) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("删除账户？", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "删除“${account.name}”后，该账户将不再显示，历史交易保留。",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LedgerIconButton(
                            onClick = { confirmDelete = false },
                            modifier = Modifier.offset(x = -closeIconInset).size(40.dp), showPressFeedback = false,
                        ) {
                            Icon(MaterialSymbols.Close, "取消删除账户", Modifier.size(24.dp), tint = colors.onSurface)
                        }
                        LedgerIconButton(
                            onClick = { confirmDelete = false; onDelete() },
                            modifier = Modifier.offset(x = deleteIconInset).size(40.dp), showPressFeedback = false,
                        ) {
                            Icon(MaterialSymbols.Delete, "确认删除账户", Modifier.size(24.dp), tint = colors.error)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountDetailsPreview() {
    LedgerTheme {
        AccountDetailsContent(
            account = Account(id = 1L, name = "工资卡", type = AccountType.DEBIT, currentBalance = 123456L, cardNumber = "1234"),
            onClose = {},
            onSave = {},
            onDelete = {},
        )
    }
}
