package cn.turingzy.ledger.ui.feature.wallet.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.account.AccountWithTemplate
import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.ui.theme.LedgerTheme

private fun previewAccounts(): List<AccountWithTemplate> = listOf(
    Account(1L, "银行卡", AccountType.DEBIT, currentBalance = 1234500L, cardNumber = "6222000012345678"),
    Account(2L, "电子钱包", AccountType.WALLET, currentBalance = 56000L),
    Account(3L, "信用卡", AccountType.CREDIT, currentBalance = -230000L, cardNumber = "4392000012349012"),
    Account(4L, "现金", AccountType.CASH, currentBalance = 50000L),
).map { AccountWithTemplate(it, null) }

@Preview(name = "Wallet Deck Light", showBackground = true, widthDp = 420, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Wallet Deck Dark", showBackground = true, widthDp = 420, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WalletDeckPreview() {
    LedgerTheme {
        Box(Modifier.fillMaxWidth().padding(LedgerStyle.spacing.extraLarge)) {
            SwipeableBankCardDeck(previewAccounts(), {})
        }
    }
}
