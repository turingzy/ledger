package cn.turingzy.ledger.ui.feature.wallet.sheet

import android.content.res.Configuration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.account.CardTemplate
import cn.turingzy.ledger.ui.theme.LedgerTheme

internal enum class AccountSheetPreviewCase { NewAccount, AccountDetails, CardFacePicker }

internal class AccountSheetPreviewProvider : PreviewParameterProvider<AccountSheetPreviewCase> {
    override val values: Sequence<AccountSheetPreviewCase> = AccountSheetPreviewCase.entries.asSequence()
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "AccountSheet Light", showBackground = true, showSystemUi = true, widthDp = 420, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "AccountSheet Dark", showBackground = true, showSystemUi = true, widthDp = 420, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AccountSheetPreview(@PreviewParameter(AccountSheetPreviewProvider::class) case: AccountSheetPreviewCase) {
    val account = Account(1L, "日常账户", AccountType.DEBIT, currentBalance = 1234500L, cardNumber = "1234")
    val templates = listOf(CardTemplate(1L, "示例卡面", ""))
    LedgerTheme {
        when (case) {
            AccountSheetPreviewCase.NewAccount -> AddAccountSheet({}, { _, _, _, _, _, _, _ -> }, templates = templates)
            AccountSheetPreviewCase.AccountDetails -> AccountDetailsSheet(account, {}, {}, {}, templates = templates)
            AccountSheetPreviewCase.CardFacePicker -> AccountCardFaceDialog(templates, null, onDismiss = {}, onConfirm = {})
        }
    }
}
