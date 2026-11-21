package cn.turingzy.ledger.ui.feature.wallet.sheet

import cn.turingzy.ledger.ui.component.base.ModalBottomSheetBase
import cn.turingzy.ledger.ui.component.base.keepSheetAnchorDuringIme
import cn.turingzy.ledger.ui.component.base.DialogWindowBase
import cn.turingzy.ledger.ui.component.base.DialogActions
import cn.turingzy.ledger.ui.component.base.dialogContentPadding
import cn.turingzy.ledger.ui.component.base.SurfaceBase
import cn.turingzy.ledger.ui.theme.LedgerStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.account.CardTemplate
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
    assignedTemplate: CardTemplate? = null,
) {
    val scope = rememberCoroutineScope()
    fun dismiss() {
        scope.launch {
            sheetState.hide()
            if (!sheetState.isVisible) onDismissRequest()
        }
    }

    ModalBottomSheetBase(
        contentHandlesInsets = true,
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier.keepSheetAnchorDuringIme(),
    ) {
        AccountDetailsContent(
            account = account,
            templates = templates,
            assignedTemplate = assignedTemplate,
            onDelete = { onDelete(account.id); dismiss() },
            onDismiss = ::dismiss,
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
    onSave: (Account) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    templates: List<CardTemplate> = emptyList(),
    onDismiss: () -> Unit = {},
    assignedTemplate: CardTemplate? = null,
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    var confirmDelete by rememberSaveable(account.id) { mutableStateOf(false) }
    AccountEditorContent(
        account = account,
        onDismiss = onDismiss,
        templates = templates,
        assignedTemplate = assignedTemplate,
        onSave = onSave,
        modifier = modifier,
        onDelete = { confirmDelete = true },
    )
    if (confirmDelete) {
        DialogWindowBase(onDismissRequest = { confirmDelete = false }) {
            SurfaceBase(shape = LedgerStyle.shapes.confirmationDialog, color = MaterialTheme.colorScheme.surface) {
                Column(
                    Modifier.dialogContentPadding(LedgerStyle.spacing.block),
                    verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.extraLarge),
                ) {
                    Text(textResources.getString(cn.turingzy.ledger.R.string.ui_delete_account_2), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "删除“${account.name}”后，该账户将不再显示，历史交易保留。",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    DialogActions(
                        onCancel = { confirmDelete = false },
                        onConfirm = { confirmDelete = false; onDelete() },
                        confirmLabel = textResources.getString(cn.turingzy.ledger.R.string.bill_delete_action),
                        destructive = true,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountDetailsPreview() {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    LedgerTheme {
        AccountDetailsContent(
            account = Account(id = 1L, name = textResources.getString(cn.turingzy.ledger.R.string.ui_salary_account), type = AccountType.DEBIT, currentBalance = 123456L, cardNumber = "1234"),
            onSave = {},
            onDelete = {},
        )
    }
}
