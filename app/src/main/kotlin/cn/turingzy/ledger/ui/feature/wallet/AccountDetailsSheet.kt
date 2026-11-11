package cn.turingzy.ledger.ui.feature.wallet

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
            onSave = {
                onSave(it)
                dismiss()
            },
        )
    }
}

@Composable
fun AccountDetailsContent(
    account: Account,
    onClose: () -> Unit,
    onSave: (Account) -> Unit,
    modifier: Modifier = Modifier,
    templates: List<CardTemplate> = emptyList(),
) {
    AccountEditorContent(
        account = account,
        templates = templates,
        onClose = onClose,
        onSave = onSave,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun AccountDetailsPreview() {
    LedgerTheme {
        AccountDetailsContent(
            account = Account(id = 1L, name = "工资卡", type = AccountType.DEBIT, currentBalance = 123456L, cardNumber = "1234"),
            onClose = {},
            onSave = {},
        )
    }
}
