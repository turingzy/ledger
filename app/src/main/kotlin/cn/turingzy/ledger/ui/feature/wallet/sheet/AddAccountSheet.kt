package cn.turingzy.ledger.ui.feature.wallet.sheet

import cn.turingzy.ledger.ui.component.base.BaseModalBottomSheet
import cn.turingzy.ledger.ui.component.base.keepSheetAnchorDuringIme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.account.CardTemplate
import cn.turingzy.ledger.ui.theme.LedgerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountSheet(
    onDismissRequest: () -> Unit,
    onConfirm: (
        name: String,
        type: AccountType,
        initialBalance: Long,
        cardNumber: String?,
        includeInTotal: Boolean,
        currency: String,
        templateId: Long?,
    ) -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    templates: List<CardTemplate> = emptyList(),
) {
    BaseModalBottomSheet(
        contentHandlesInsets = true,
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier.keepSheetAnchorDuringIme(),
    ) {
        AddAccountSheetContent(onConfirm, templates = templates, onDismiss = onDismissRequest)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountSheetContent(
    onConfirm: (
        name: String,
        type: AccountType,
        initialBalance: Long,
        cardNumber: String?,
        includeInTotal: Boolean,
        currency: String,
        templateId: Long?,
    ) -> Unit,
    modifier: Modifier = Modifier,
    templates: List<CardTemplate> = emptyList(),
    onDismiss: () -> Unit = {},
) {
    AccountEditorContent(
        account = null,
        onDismiss = onDismiss,
        templates = templates,
        onSave = { value ->
            onConfirm(
                value.name, value.type, value.initialBalance, value.cardNumber,
                value.includeInTotal, value.currency, value.templateId,
            )
        },
        modifier = modifier,
    )
}


@Preview(showBackground = true, heightDp = 900)
@Composable
private fun AddAccountSheetContentPreview() {
    LedgerTheme {
        AddAccountSheetContent(onConfirm = { _, _, _, _, _, _, _ -> })
    }
}
