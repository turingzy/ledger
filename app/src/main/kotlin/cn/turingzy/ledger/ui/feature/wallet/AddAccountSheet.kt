package cn.turingzy.ledger.ui.feature.wallet

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import cn.turingzy.ledger.model.AccountType
import cn.turingzy.ledger.model.CardTemplate
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
    AccountSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        AddAccountSheetContent(onDismissRequest, onConfirm, templates = templates)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountSheetContent(
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
    templates: List<CardTemplate> = emptyList(),
) {
    AccountEditorContent(
        account = null,
        templates = templates,
        onClose = onDismissRequest,
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
        AddAccountSheetContent(onDismissRequest = {}, onConfirm = { _, _, _, _, _, _, _ -> })
    }
}
