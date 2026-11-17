package cn.turingzy.ledger.ui.feature.wallet.sheet

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cn.turingzy.ledger.model.account.CardTemplate
import cn.turingzy.ledger.model.account.AccountType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountDialog(
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
    AddAccountSheet(
        onDismissRequest = onDismissRequest,
        onConfirm = onConfirm,
        templates = templates,
        modifier = modifier,
    )
}
